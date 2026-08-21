package com.jobnews.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화"가 만든 요약 품질을 개발자가 눈으로 확인하기
 * 위한 QA 전용 조회 API입니다. fix/no-raw-storage 원칙(기사 원문을 어디에도 저장하지
 * 않음)은 그대로 유지하면서, 이미 저장된 news(url/title)와 news_analysis(summary)만
 * join해서 보여줍니다. 원문이 궁금하면 url을 열어서 사람이 직접 확인해야 합니다.
 */
// @Tag: Swagger UI에서 이 컨트롤러의 엔드포인트들을 묶어서 보여줄 그룹 이름/설명입니다.
// 안 쓰면 Swagger UI에 그룹 이름 없이 컨트롤러 클래스 이름만 자동으로 붙습니다.
@Tag(name = "Summary Review", description = "AI 요약 품질 확인용 QA 전용 조회 API (개발 중에만 사용)")
@RestController
@RequestMapping("/api/review")
public class SummaryReviewController {

    private final SummaryReviewMapper summaryReviewMapper;

    public SummaryReviewController(SummaryReviewMapper summaryReviewMapper) {
        this.summaryReviewMapper = summaryReviewMapper;
    }

    // [무엇을 받아서] 요청 파라미터 없음.
    // [무엇을 하고] SummaryReviewMapper로 news+news_analysis를 join해서 최근 수집된
    //              50건을 가져오고, 각 SummaryReviewRow(매퍼용 mutable 객체)를
    //              SummaryReviewItem(API 응답용 불변 객체)으로 바꿉니다.
    // [무엇을 돌려주는지] collected_at(수집 시각) 최신순 최대 50건. 페이징 없음(요구사항:
    //              "개발 중 확인용이라 단순하게").
    // @Operation: Swagger UI에서 이 엔드포인트 하나에 대해 보여줄 요약/설명입니다.
    @GetMapping("/summaries")
    @Operation(
            summary = "최근 AI 요약 목록 조회",
            description = "collected_at 최신순으로 최대 50건. title/url/summary/importanceScore와 함께, "
                    + "같은 뉴스를 IT전산/데이터분석/백엔드 3개 직무 관점으로 재해석한 jobAnalyses도 반환합니다. "
                    + "기사 원문은 어디에도 저장하지 않으므로 응답에 포함되지 않습니다. 원문 확인은 url을 직접 열어서 합니다."
    )
    public List<SummaryReviewItem> summaries() {
        List<SummaryReviewRow> rows = summaryReviewMapper.selectRecentSummaries();
        return rows.stream()
                .map(row -> new SummaryReviewItem(
                        row.getTitle(),
                        row.getUrl(),
                        row.getSummary(),
                        row.getImportanceScore(),
                        row.getJobAnalyses().stream()
                                .map(j -> new JobAnalysisResult(j.getJob(), j.getWhyItMatters(), j.getKeySkills()))
                                .toList()))
                .toList();
    }
}
