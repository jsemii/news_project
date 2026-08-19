package com.jobnews.ai;

import com.jobnews.news.News;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계의 핵심 실행부입니다. RssFetcher가 RSS를
 * 가져오듯, ArticleContentFetcher가 원문을 크롤링하듯, 이 클래스는 뉴스 하나(News)를
 * 받아서 OpenAI에게 "이 기사를 요약하고, 관련 산업을 골라주고, IT전산/데이터분석/백엔드
 * 3개 직무 관점에서 각각 재해석해줘"라고 요청하고, 그 응답을 우리 프로젝트가 다루기
 * 쉬운 형태(AiAnalysisResult)로 바꿔서 돌려줍니다.
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

    // [무엇을 받아서] WebClient(HTTP 요청 도구), OpenAiProperties(API 키/모델 등 설정),
    //              AiTaxonomyProperties(산업/직무 정해진 목록), ObjectMapper(JSON을 읽고
    //              쓰는 도구 — Spring이 기본으로 등록해주는 빈이라 별도 라이브러리 추가 없이 사용)를
    //              스프링이 자동으로 주입해줍니다.
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
     * [무엇을 받아서] 분석할 뉴스 하나(News — title/description을 사용)를 받습니다.
     * [무엇을 하고] 1) 산업/직무 목록과 뉴스 내용을 넣어 프롬프트를 만들고,
     *              2) OpenAI Chat Completions API를 호출하고,
     *              3) 응답 JSON을 파싱·검증해서 AiAnalysisResult로 바꿉니다.
     * [무엇을 돌려주는지] 분석 결과(AiAnalysisResult). 실패하면 null이 아니라
     *              AiStructureException을 던집니다 — RssFetcher와 같은 이유로,
     *              "조용한 실패"를 막아 호출하는 쪽이 재시도/로그를 남기도록 강제합니다.
     * [try/catch 의도] 네트워크 오류, OpenAI의 오류 응답, 예상과 다른 JSON 구조 등
     *              어떤 이유로 실패하든 전부 AiStructureException으로 통일해서 던집니다.
     */
    public AiAnalysisResult analyze(News news) {
        try {
            Map<String, Object> requestBody = buildRequestBody(news);

            String responseBody = webClient.post()
                    .uri(openAiProperties.getBaseUrl() + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);

            return parseResponse(responseBody);
        } catch (AiStructureException e) {
            throw e; // 이미 우리가 의미를 붙여 던진 예외는 그대로 다시 던집니다.
        } catch (Exception e) {
            throw new AiStructureException("Failed to analyze news via OpenAI (newsId=" + news.getId() + ")", e);
        }
    }

    // [무엇을 받아서] 분석할 뉴스를 받습니다.
    // [무엇을 하고] OpenAI Chat Completions API가 요구하는 형태의 요청 본문(Map)을 만듭니다.
    //              "response_format": {"type": "json_object"}는 OpenAI에게 "반드시 JSON
    //              형식으로만 응답해라"라고 강제하는 옵션입니다 — 이게 없으면 모델이 JSON
    //              앞뒤에 설명을 덧붙이는 등 파싱하기 어려운 형태로 응답할 수 있습니다.
    // [무엇을 돌려주는지] WebClient가 그대로 JSON으로 직렬화(자바 객체를 JSON 문자열로
    //              바꾸는 것)할 수 있는 Map 객체.
    private Map<String, Object> buildRequestBody(News news) {
        Map<String, Object> systemMessage = Map.of("role", "system", "content", buildSystemPrompt());
        Map<String, Object> userMessage = Map.of("role", "user", "content", buildUserPrompt(news));

        return Map.of(
                "model", openAiProperties.getModel(),
                "messages", List.of(systemMessage, userMessage),
                "response_format", Map.of("type", "json_object")
        );
    }

    // [무엇을 받아서] 입력값 없음.
    // [무엇을 하고] AI에게 "어떤 역할을 맡고, 어떤 형식으로 답해야 하는지"를 지시하는
    //              시스템 프롬프트를 만듭니다. 산업/직무 목록은 AiTaxonomyProperties(즉
    //              application.yml)에서 읽어와 프롬프트에 그대로 끼워 넣습니다 — 목록을
    //              yml에서 바꾸면 프롬프트도 코드 수정 없이 자동으로 따라 바뀝니다.
    // [무엇을 돌려주는지] 시스템 프롬프트 문자열.
    private String buildSystemPrompt() {
        String industryList = String.join(", ", taxonomyProperties.getIndustries());
        String jobList = String.join(", ", taxonomyProperties.getJobs());

        StringBuilder jobKeysExample = new StringBuilder();
        for (String job : taxonomyProperties.getJobs()) {
            jobKeysExample.append("    \"").append(job)
                    .append("\": {\"why_it_matters\": \"...\", \"key_skills\": \"...\"},\n");
        }

        return """
                당신은 IT 직무 취업 준비생을 위한 뉴스 분석가입니다.
                주어진 뉴스 기사를 읽고, 반드시 아래 JSON 형식으로만 응답하세요. 다른 설명은 붙이지 마세요.

                {
                  "summary": "기사 내용을 2~3문장으로 요약",
                  "importance_score": "이 뉴스가 IT전산/데이터분석/백엔드 취준생에게 얼마나 중요한지 1~10 사이 정수",
                  "importance_reason": "위 점수를 그렇게 매긴 이유를 1~2문장으로",
                  "industries": ["관련 산업들 (아래 산업 목록 중에서만 선택, 관련 있는 것만 포함, 없으면 빈 배열)"],
                  "jobs": {
                %s  }
                }

                산업 목록(이 안에서만 골라야 함): %s

                직무 목록(반드시 아래 %d개 키를 jobs 안에 전부 포함해야 함): %s
                각 직무의 why_it_matters는 "이 뉴스가 그 직무 취준생에게 왜 중요한지"를 2~3문장으로,
                key_skills는 이 뉴스와 관련해 도움이 될 역량/기술 키워드를 콤마로 구분해서 작성하세요.
                직접적인 관련이 적어 보여도 그 직무 관점에서 최대한 의미를 찾아 작성하세요(빈 값으로 두지 마세요).

                importance_score 기준: 취준생의 산업 이해나 직무 역량과 크게 관련 없는 단순 사실 전달이면 낮게(1~3점),
                산업 동향이나 기술 트렌드를 보여주면 중간(4~6점), 취준생의 취업 전략에 직접 참고가 될 정도로
                중요하면 높게(7~10점) 매기세요.
                """.formatted(jobKeysExample, industryList, taxonomyProperties.getJobs().size(), jobList);
    }

    // [무엇을 받아서] 분석할 뉴스를 받습니다.
    // [무엇을 하고] 뉴스 제목과, 너무 길지 않게 자른 본문(description)을 사용자 메시지로
    //              만듭니다. maxInputChars(기본 1500자)로 자르는 이유는 원문이 길어질수록
    //              OpenAI API 비용이 커지기 때문입니다(비용 관리).
    // [무엇을 돌려주는지] 사용자 메시지 문자열.
    private String buildUserPrompt(News news) {
        String description = news.getDescription() == null ? "" : news.getDescription();
        int limit = openAiProperties.getMaxInputChars();
        String truncated = description.length() > limit ? description.substring(0, limit) : description;

        return "제목: " + news.getTitle() + "\n\n본문: " + truncated;
    }

    // [무엇을 받아서] OpenAI가 돌려준 응답 원문(JSON 문자열)을 받습니다.
    // [무엇을 하고] 1) OpenAI 응답의 바깥 구조(choices[0].message.content)에서 실제
    //              우리가 요청한 JSON 문자열을 꺼내고, 2) 그 문자열을 다시 한번 JSON으로
    //              파싱해서 summary/industries/jobs를 읽어냅니다("겹JSON" 구조인 이유는
    //              OpenAI Chat Completions API 자체의 표준 응답 형식이 그렇기 때문입니다).
    //              3) industries는 정해진 8개 목록에 없는 값이 섞여 있으면 걸러내고,
    //              4) jobs는 정해진 3개 직무가 전부 있는지 검증해서, 하나라도 빠지면
    //              실패로 처리합니다(요구사항: "직무별 분석은 3개 각각 생성"이 DB 제약과
    //              화면 설계의 전제이기 때문에, 여기서 어긴 응답은 재시도 대상으로 봅니다).
    //              5) importance_score는 1~10 범위를 벗어나면(모델이 이상한 값을 줄 경우를
    //              대비해) 범위 안으로 강제 보정합니다.
    // [무엇을 돌려주는지] 검증까지 끝난 AiAnalysisResult.
    private AiAnalysisResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asString();
            JsonNode parsed = objectMapper.readTree(content);

            String summary = parsed.path("summary").asString();
            int importanceScore = clampImportanceScore(parsed.path("importance_score").asInt());
            String importanceReason = parsed.path("importance_reason").asString();
            List<String> industries = extractValidIndustries(parsed.path("industries"));
            List<AiAnalysisResult.JobAnalysis> jobs = extractJobs(parsed.path("jobs"));

            return new AiAnalysisResult(summary, industries, jobs, importanceScore, importanceReason);
        } catch (AiStructureException e) {
            throw e;
        } catch (Exception e) {
            throw new AiStructureException("Failed to parse OpenAI response: " + responseBody, e);
        }
    }

    // [무엇을 받아서] AI가 응답한 importance_score 원값을 받습니다.
    // [무엇을 하고] 프롬프트에서 1~10 사이 정수로 요청했지만, LLM이 가끔 그 범위를
    //              벗어난 값(0, 11 등)을 줄 수 있어 방어적으로 범위 안으로 잘라냅니다.
    // [무엇을 돌려주는지] 1~10 사이로 보정된 값.
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

    private List<AiAnalysisResult.JobAnalysis> extractJobs(JsonNode jobsNode) {
        List<AiAnalysisResult.JobAnalysis> result = new ArrayList<>();

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
            result.add(new AiAnalysisResult.JobAnalysis(job, whyItMatters, keySkills));
        }
        return result;
    }
}
