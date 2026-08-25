package com.jobnews.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계의 핵심 실행부입니다. OpenAI를 **두 번**
 * 나눠서 호출합니다(저작권 리스크 완화를 위한 설계 — docs/troubleshooting.md 참고).
 * 1단계(analyzeGeneral)는 크롤링한 원문(메모리 상의 값, DB에 없음)을 읽고 일반 요약을
 * 만들고, 2단계(analyzeJobs)는 원문이 아니라 1단계가 만든 요약을 입력으로 받아
 * 직무별(IT전산/데이터분석/백엔드) 재해석을 만듭니다. 두 단계 모두 "원문/요약 문장을
 * 그대로 옮기지 말고 완전히 새로 쓰라"는 지시를 프롬프트에 명시해서, 표절처럼 보일 수
 * 있는 문장 재사용을 막습니다.
 */
@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    // LLM은 응답을 만드는 데 시간이 걸릴 수 있어서, 단순 HTTP 요청(RSS 10초)보다
    // 넉넉하게 30초로 잡았습니다.
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final WebClient webClient;
    private final OpenAiProperties openAiProperties;
    private final AiTaxonomyProperties taxonomyProperties;
    private final ObjectMapper objectMapper;

    public OpenAiClient(WebClient webClient,
                         OpenAiProperties openAiProperties,
                         AiTaxonomyProperties taxonomyProperties,
                         ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.openAiProperties = openAiProperties;
        this.taxonomyProperties = taxonomyProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * [1단계] [무엇을 받아서] 기사 제목과, 방금 크롤링해서 얻은 원문(메모리 상의 문자열 —
     *              이 메서드가 끝나면 호출한 쪽에서 더 이상 참조하지 않는 이상 버려짐)을 받습니다.
     * [무엇을 하고] 원문을 사실관계 중심으로 완전히 새로운 문장으로 요약하고, 관련 산업과
     *              중요도 점수를 함께 요청합니다.
     * [무엇을 돌려주는지] GeneralAnalysisResult. 실패하면 AiStructureException을 던집니다.
     */
    public GeneralAnalysisResult analyzeGeneral(String title, String rawText) {
        String systemPrompt = buildGeneralSystemPrompt();
        String userPrompt = buildGeneralUserPrompt(title, rawText);
        JsonNode parsed = call(systemPrompt, userPrompt);

        String summary = parsed.path("summary").asString();
        int importanceScore = clampImportanceScore(parsed.path("importance_score").asInt());
        String importanceReason = parsed.path("importance_reason").asString();
        List<String> industries = extractValidIndustries(parsed.path("industries"));

        return new GeneralAnalysisResult(summary, importanceScore, importanceReason, industries);
    }

    /**
     * [2단계] [무엇을 받아서] 1단계에서 만든 일반 요약(원문이 아님)을 받습니다.
     * [무엇을 하고] 이 요약을 IT전산/데이터분석/백엔드 3개 직무 관점에서 재해석합니다.
     *              입력이 원문이 아니라 짧은 요약이라, 1단계보다 훨씬 적은 토큰으로 처리됩니다.
     * [무엇을 돌려주는지] 직무 3개 각각의 JobAnalysisResult 목록. 정해진 3개 직무 키가
     *              응답에 전부 없으면 실패로 처리합니다(요구사항: "직무별 분석은 3개
     *              각각 생성"이 DB 제약과 화면 설계의 전제이기 때문입니다).
     */
    public List<JobAnalysisResult> analyzeJobs(String generalSummary) {
        String systemPrompt = buildJobsSystemPrompt();
        String userPrompt = "다음은 어떤 뉴스에 대한 일반 요약입니다(원문이 아닙니다):\n\n" + generalSummary;
        JsonNode parsed = call(systemPrompt, userPrompt);

        return extractJobs(parsed.path("jobs"));
    }

    // [무엇을 받아서] 시스템 프롬프트와 사용자 프롬프트를 받습니다.
    // [무엇을 하고] OpenAI Chat Completions API를 호출하고, 겹JSON 구조(choices[0].message.content
    // 안에 우리가 요청한 JSON 문자열이 또 들어있는 OpenAI 표준 응답 형식)를 한 번 풀어서
    // 실제 응답 JSON 노드를 돌려줍니다. analyzeGeneral/analyzeJobs 둘 다 이 공통 로직을 씁니다.
    // [try/catch 의도] 네트워크 오류, OpenAI 오류 응답, JSON 파싱 실패 등 원인이 무엇이든
    // AiStructureException 하나로 통일해서, 호출하는 쪽이 재시도 여부만 판단하면 되게 합니다.
    private JsonNode call(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> systemMessage = Map.of("role", "system", "content", systemPrompt);
            Map<String, Object> userMessage = Map.of("role", "user", "content", userPrompt);
            Map<String, Object> requestBody = Map.of(
                    "model", openAiProperties.getModel(),
                    "messages", List.of(systemMessage, userMessage),
                    "response_format", Map.of("type", "json_object")
            );

            String responseBody = webClient.post()
                    .uri(openAiProperties.getBaseUrl() + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);

            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asString();
            return objectMapper.readTree(content);
        } catch (AiStructureException e) {
            throw e;
        } catch (Exception e) {
            throw new AiStructureException("Failed to call OpenAI", e);
        }
    }

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] 1단계(일반 요약) 시스템 프롬프트를 만듭니다. 산업 목록은
    //              AiTaxonomyProperties(즉 application.yml)에서 읽어와 그대로 끼워 넣습니다.
    //              "원문 문장을 그대로 옮기지 말 것"을 명시적으로 지시하는 것이 핵심입니다
    //              (저작권 리스크 완화 — 원문 표현을 답습한 요약은 표절 시비가 될 수 있음).
    //              ⚠️ summary를 "핵심 사실과 수치 / 배경·원인 / 맥락·시사점" 3단 구조로
    //              요구하도록 재설계했습니다 — 예전엔 수치 나열에 그쳐서(예: "수출액이
    //              552억 달러로 56% 증가") 2단계(직무별 재해석)가 참고할 재료가 부족했기
    //              때문입니다. 배경/맥락까지 담아야 2단계가 "왜 이게 이 직무에 중요한지"를
    //              뽑아낼 재료가 생깁니다.
    //              ⚠️ 그런데 길이를 억지로 늘리려는 지시("시사점이 없어도 최소 1문장은
    //              써라")가 오히려 사실 오류(주체 바꿔치기)와 할루시네이션(원문에 없는
    //              전망을 그럴듯하게 지어냄)을 유발한다는 것을 QA 조회(GET /api/review/summaries)로
    //              직접 확인했습니다. 그래서 "정확성 규칙"을 길이 목표보다 항상 우선하도록
    //              프롬프트 구조를 바꿨습니다 — 시사점이 원문에 없으면 억지로 채우지 않고
    //              짧게 끝내는 것을 허용합니다(길이보다 정확성이 중요하다는 판단).
    // [무엇을 돌려주는지] 1단계 시스템 프롬프트 문자열.
    private String buildGeneralSystemPrompt() {
        String industryList = String.join(", ", taxonomyProperties.getIndustries());

        return """
                당신은 뉴스 기사를 사실관계 중심으로 요약하는 전문 에디터입니다.
                주어진 뉴스 원문을 읽고, 반드시 아래 JSON 형식으로만 응답하세요. 다른 설명은 붙이지 마세요.

                {
                  "summary": "아래 3가지 요소를 자연스럽게 이어붙인 4~6문장(약 400~600자) 요약",
                  "importance_score": "이 뉴스가 IT전산/데이터분석/백엔드 취준생에게 얼마나 중요한지 1~10 사이 정수",
                  "importance_reason": "위 점수를 그렇게 매긴 이유를 1~2문장으로",
                  "industries": ["관련 산업들 (아래 산업 목록 중에서만 선택, 관련 있는 것만 포함, 없으면 빈 배열)"]
                }

                summary는 아래 3가지 요소를 이 순서대로 담되, 아래에 명시된 "정확성 규칙"을
                절대 어기지 않는 선에서 각 요소별 최소 글자수를 최대한 채우세요(3개 합쳐
                목표 420자 이상, 총 4~6문장). 정확성 규칙과 최소 글자수가 충돌하면 정확성이
                항상 우선입니다 — 채울 내용이 없으면 짧게 쓰는 것이 지어내는 것보다 낫습니다.
                1. 핵심 사실과 수치 (목표 120자): 기사에서 가장 중요한 사실과 구체적인 수치
                2. 배경/원인 (목표 150자): 그 사실이나 수치가 왜 나왔는지, 원문에 나온 설명을 사실
                중심으로 재구성. 원인이 여러 개라면 하나로 뭉뚱그리지 말고 풀어서 서술하세요.
                3. 맥락/시사점 (목표 150자): 이 흐름이 산업/시장/관련 종사자들에게 어떤 의미를 갖는지.
                원문에 이 내용이 실제로 나와 있을 때만 재구성해서 쓰세요. 원문에 시사점·전망·파급효과에
                대한 언급이 없다면, 절대로 지어내지 말고 대신 원문에 실제로 나온 다른 세부사실
                (예: 향후 일정, 추가 조치 계획, 관계자 반응 등)로 채우세요. 그래도 채울 내용이
                원문에 없다면 이 요소는 짧게 끝내도 됩니다.

                산업 목록(이 안에서만 골라야 함): %s

                importance_score 기준 — 반드시 아래 기준만 사용하세요. 뉴스 자체의 사회적 파장·화제성·
                인명 피해 규모·충격도는 절대 점수에 반영하지 마세요. 오직 "이 뉴스가 IT전산/데이터분석/
                백엔드 3개 직무 또는 위 산업 목록과 얼마나 직접적으로 연관되는가"만 봅니다.
                - 1~2점: 3개 직무(IT전산/데이터분석/백엔드)나 산업 목록 어디와도 명확한 연결고리가
                없는 뉴스. 사건·사고, 재해, 연예, 스포츠, 날씨, 부고/인사 등은 아무리 사회적으로
                중요하거나 충격적이어도 무조건 이 구간입니다.
                - 3점: 산업 목록의 산업 중 하나와 아주 약하게(스치듯) 관련은 있지만, 3개 직무의
                업무나 역량과는 직접 연결되지 않는 뉴스.
                - 4~6점: 산업 목록의 산업 동향이나 기술 트렌드를 보여주면서, 3개 직무 중 하나
                이상과 간접적으로 연결되는 뉴스(예: 그 산업에서 이런 기술/시스템이 쓰인다는
                정도의 연결).
                - 7~10점: 3개 직무의 구체적인 채용 동향, 필요 기술 스택, 직무 역량 요구사항과
                직접 연결되는 뉴스(예: 해당 직무 채용 확대, 특정 기술 도입/전환, 관련 프로젝트 발주 등).

                [정확성 규칙 — 아래 두 가지는 길이나 다른 어떤 지시보다도 우선합니다]

                규칙 A (주체를 바꾸거나 일반화하지 말 것): 어떤 사실이나 발언을 "누가" 했는지
                (지자체명·기관명·인명·기업명 등 고유명사)는 원문과 정확히 동일하게 유지하세요.
                "전주시"를 "정부"나 "지자체"로, 특정 기업명을 "업계"로 바꾸는 것처럼 더 상위/유사한
                개념으로 일반화하거나 치환하는 것은 금지입니다. 원문에 명시된 주체를 요약에서도
                똑같은 이름으로 그대로 사용하세요.

                규칙 B (없는 내용을 추측해서 덧붙이지 말 것): 원문에 명시적으로 나오지 않은 전망,
                예측, 파급효과, 시사점을 그럴듯하게 만들어서 추가하지 마세요("~에 영향을 미칠
                가능성이 있다", "~로 이어질 것으로 보인다" 같은 문장을 원문 근거 없이 새로 쓰는 것
                금지). summary에 들어가는 모든 문장은 원문에 실제로 나온 사실을 재구성한 것이어야
                하며, 원문에 없는 내용을 새로 만들어내면 안 됩니다.

                자체 점검: 답변을 작성한 뒤, 다음 세 가지를 스스로 확인하세요.
                (1) summary에 등장하는 모든 주체(누가 했는지)가 원문의 주체와 정확히 일치하는가?
                (2) summary의 모든 문장이 원문에 실제로 있는 내용에 근거하고 있으며, 원문에 없는
                추측이나 전망을 덧붙이지 않았는가?
                (3) importance_score를 매길 때 뉴스의 사회적 중요도·화제성이 아니라 3개 직무/산업
                목록과의 직접적 연관성만 기준으로 삼았는가? 산업 목록과 무관한 사건·사고·화제성
                뉴스인데도 "중요해 보인다"는 이유로 점수를 높게 주지 않았는가?
                셋 중 하나라도 어긋난다면, 최종 답변을 보내기 전에 그 부분을 수정하세요.

                중요: summary 길이를 채우는 것은 "원문을 더 많이 베끼라"는 뜻도, "없는 내용을
                추측해서 채우라"는 뜻도 아닙니다. 원문의 문장 구조, 어순, 표현은 그대로 옮기면
                안 되지만(완전히 새로운 문장으로 재구성), 담기는 내용 자체는 항상 원문에 실제로
                있는 사실이어야 합니다. 원문을 요약하는 게 아니라 원문이 전하는 사실을 당신의
                언어로 재구성한다고 생각하세요 — 새로운 사실을 만들어내는 것이 아닙니다.
                """.formatted(industryList);
    }

    // [무엇을 받아서] 기사 제목과, 길이를 자른 원문을 받습니다.
    // [무엇을 하고] maxInputChars(기본 1500자)로 원문을 자릅니다 — 원문이 길어질수록
    //              OpenAI API 비용이 커지기 때문입니다(비용 관리). 원문은 이 메서드
    //              호출 이후 더 이상 참조되지 않으면 곧바로 가비지 컬렉션 대상이 됩니다.
    // [무엇을 돌려주는지] 1단계 사용자 프롬프트 문자열.
    private String buildGeneralUserPrompt(String title, String rawText) {
        String text = rawText == null ? "" : rawText;
        int limit = openAiProperties.getMaxInputChars();
        String truncated = text.length() > limit ? text.substring(0, limit) : text;

        return "제목: " + title + "\n\n원문: " + truncated;
    }

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] 2단계(직무별 재해석) 시스템 프롬프트를 만듭니다. 여기도 "요약 문장을
    //              그대로 옮기지 말 것"을 명시합니다 — 1단계 요약을 그대로 복사해 붙이는
    //              식으로 재해석하면 의미가 없기 때문입니다.
    // [무엇을 돌려주는지] 2단계 시스템 프롬프트 문자열.
    private String buildJobsSystemPrompt() {
        String jobList = String.join(", ", taxonomyProperties.getJobs());

        StringBuilder jobKeysExample = new StringBuilder();
        for (String job : taxonomyProperties.getJobs()) {
            jobKeysExample.append("    \"").append(job)
                    .append("\": {\"why_it_matters\": \"...\", \"key_skills\": \"...\"},\n");
        }

        return """
                당신은 IT 직무 취업 준비생을 위한 뉴스 분석가입니다.
                사용자가 보내는 내용은 어떤 뉴스에 대한 일반 요약입니다(뉴스 원문이 아닙니다).
                이 요약을 읽고, 반드시 아래 JSON 형식으로만 응답하세요. 다른 설명은 붙이지 마세요.

                {
                  "jobs": {
                %s  }
                }

                직무 목록(반드시 아래 %d개 키를 jobs 안에 전부 포함해야 함): %s
                각 직무의 why_it_matters는 "이 요약이 그 직무 취준생에게 왜 중요한지"를 2~3문장으로,
                key_skills는 관련해 도움이 될 역량/기술 키워드를 콤마로 구분해서 작성하세요.
                직접적인 관련이 적어 보여도 그 직무 관점에서 최대한 의미를 찾아 작성하세요(빈 값으로 두지 마세요).

                중요: 입력으로 받은 요약 문장을 그대로 옮겨 적지 마세요. 그 안의 사실관계를 바탕으로
                각 직무 관점에서 완전히 새로운 문장으로 재해석해서 작성하세요.
                """.formatted(jobKeysExample, taxonomyProperties.getJobs().size(), jobList);
    }

    private int clampImportanceScore(int rawScore) {
        return Math.max(1, Math.min(10, rawScore));
    }

    private List<String> extractValidIndustries(JsonNode industriesNode) {
        List<String> allowed = taxonomyProperties.getIndustries();
        List<String> result = new ArrayList<>();

        for (JsonNode node : industriesNode) {
            String industry = node.asString();
            // if: AI가 정해진 8개 목록에 없는 값을 만들어냈다면(할루시네이션), 저장하지 않고
            // 걸러냅니다. news_industry.industry는 화면에서 필터로도 쓰일 값이라, 정해진
            // 값이 아니면 오히려 나중에 혼란을 줄 수 있기 때문입니다.
            if (allowed.contains(industry)) {
                result.add(industry);
            } else {
                log.warn("Dropped unexpected industry from AI response: {}", industry);
            }
        }
        return result;
    }

    private List<JobAnalysisResult> extractJobs(JsonNode jobsNode) {
        List<JobAnalysisResult> result = new ArrayList<>();

        for (String job : taxonomyProperties.getJobs()) {
            JsonNode jobNode = jobsNode.path(job);
            // if: 정해진 직무 3개 중 하나라도 응답에 없으면, 이 응답 전체를 실패로 봅니다.
            // 부분적으로만 있는 결과를 어설프게 저장하는 대신, 재시도해서 3개가 온전히
            // 갖춰진 응답을 다시 받는 편이 데이터 일관성 면에서 안전합니다.
            if (jobNode.isMissingNode()) {
                throw new AiStructureException("OpenAI response is missing job analysis for: " + job);
            }
            String whyItMatters = jobNode.path("why_it_matters").asString();
            String keySkills = jobNode.path("key_skills").asString();
            result.add(new JobAnalysisResult(job, whyItMatters, keySkills));
        }
        return result;
    }
}
