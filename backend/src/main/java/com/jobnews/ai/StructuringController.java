package com.jobnews.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화"를 사람이 원할 때 즉시 1회 실행할 수 있게 해주는
 * REST API입니다(이 프로젝트에서 만드는 첫 번째 REST Controller입니다). 원래는
 * NewsStructuringScheduler가 정해진 시각(매일 08/13/18시)에만 자동으로 실행하는데,
 * 개발 중에는 "지금 당장 결과를 보고 싶다"는 경우가 많아서, 스케줄러가 하는 것과
 * 완전히 같은 로직을 그 자리에서 실행하는 엔드포인트를 만들었습니다.
 */
// @RestController: 이 클래스가 "HTTP 요청을 받아서 처리하고, 그 결과를 JSON으로
// 돌려주는" 컨트롤러라는 표시입니다(AGENTS.md 규칙 5: REST API). @Controller +
// @ResponseBody를 합쳐놓은 것과 같아서, 메서드가 반환하는 객체가 화면(HTML)이 아니라
// JSON 응답 본문으로 그대로 직렬화됩니다. 안 쓰면 스프링이 이 클래스를 HTTP 요청과
// 연결해주지 않습니다.
@RestController
// @RequestMapping("/api/structuring"): 이 컨트롤러의 모든 엔드포인트 앞에 공통으로
// 붙는 URL 경로입니다. 아래 @PostMapping("/run")과 합쳐져 최종 경로는 "/api/structuring/run"이 됩니다.
@RequestMapping("/api/structuring")
// @Tag: Swagger UI에서 이 컨트롤러의 엔드포인트들을 묶어서 보여줄 그룹 이름/설명입니다.
@Tag(name = "Structuring", description = "AI 뉴스 구조화를 수동으로 즉시 실행하는 API")
public class StructuringController {

    private final NewsStructuringService newsStructuringService;

    public StructuringController(NewsStructuringService newsStructuringService) {
        this.newsStructuringService = newsStructuringService;
    }

    // [무엇을 받아서] HTTP 요청 본문 없음(그냥 POST 요청 자체가 "지금 실행해줘"라는 신호입니다).
    // [무엇을 하고] NewsStructuringService.structureAll()을 그대로 호출합니다 — 스케줄러가
    //              자동으로 실행하는 것과 완전히 같은 코드 경로입니다.
    // [무엇을 돌려주는지] 이번 실행 결과 요약(StructuringSummary)을 JSON으로 돌려줍니다.
    // [왜 동기식인지] 미분석 뉴스가 많으면(각 건마다 OpenAI 호출 + 재시도까지 걸릴 수
    //              있어서) 응답이 늦게 올 수 있습니다. 그래도 "호출 한 번으로 몇 건 처리됐는지
    //              바로 확인 가능"이 개발 중 테스트 목적에 더 유용하다고 판단해 동기식으로
    //              구현했습니다(응답이 올 때까지 요청이 대기 상태로 남습니다).
    // @PostMapping("/run"): HTTP POST 요청을 이 메서드와 연결합니다. GET이 아니라 POST를
    // 쓴 이유는, 이 호출이 DB에 실제로 데이터를 써서(분석 결과 저장) 서버 상태를 바꾸는
    // "행동"이기 때문입니다(REST 관례상 상태를 바꾸는 요청은 GET을 쓰지 않습니다).
    @PostMapping("/run")
    @Operation(
            summary = "AI 뉴스 구조화 즉시 실행",
            description = "미분석 뉴스를 openai.batch-size만큼 조회해 크롤링→1단계 요약→2단계 직무별 재해석 순으로 처리합니다. "
                    + "스케줄러(cron)가 자동으로 실행하는 것과 완전히 같은 로직입니다."
    )
    public StructuringSummary run() {
        return newsStructuringService.structureAll();
    }
}
