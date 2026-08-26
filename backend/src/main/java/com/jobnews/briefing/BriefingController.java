package com.jobnews.briefing;

import com.jobnews.ai.AiTaxonomyProperties;
import com.jobnews.ai.JobAnalysisResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 단계(핵심기능3)의 REST API입니다.
 * job 쿼리 파라미터가 없으면 예전과 완전히 같은 "일반 모드"(오늘 분석된 뉴스 중
 * 중요도 상위 N건, 공통 요약)로 동작합니다. job 파라미터로 IT전산/데이터분석/백엔드
 * 중 하나를 넘기면, 같은 뉴스 목록에 그 직무 관점의 재해석(jobInsight: whyItMatters/
 * keySkills)이 추가로 채워집니다. date 쿼리 파라미터(yyyy-MM-dd)가 없으면 오늘 날짜를
 * 쓰고, 있으면 그 날짜에 분석된 뉴스를 봅니다(날짜 탐색 UI가 이 파라미터로 과거 날짜를
 * 조회합니다). job과 date는 서로 독립적으로 함께 쓸 수 있습니다. 별도 엔드포인트를
 * 새로 만들지 않고 기존 "/api/briefings"를 파라미터로 확장한 이유는, 모든 모드가
 * "특정 날짜의 중요도순 브리핑 목록"이라는 같은 리소스를 다르게 필터링한 것뿐이기
 * 때문입니다.
 */
@RestController
@RequestMapping("/api/briefings")
@Tag(name = "Briefing", description = "직무별 맞춤 브리핑 조회 API (job 파라미터로 일반 모드/직무별 모드 전환)")
public class BriefingController {

    private final BriefingMapper briefingMapper;
    private final BriefingProperties briefingProperties;
    private final AiTaxonomyProperties aiTaxonomyProperties;

    public BriefingController(BriefingMapper briefingMapper, BriefingProperties briefingProperties,
                               AiTaxonomyProperties aiTaxonomyProperties) {
        this.briefingMapper = briefingMapper;
        this.briefingProperties = briefingProperties;
        this.aiTaxonomyProperties = aiTaxonomyProperties;
    }

    // [무엇을 받아서] job 쿼리 파라미터(선택, 예: ?job=IT전산)와 date 쿼리 파라미터
    //              (선택, yyyy-MM-dd 형식, 예: ?date=2026-08-15). 개수는 application.yml의
    //              briefing.top-n을 그대로 씀(파라미터로 안 받음).
    // [무엇을 하고] date가 없으면 오늘 날짜로, 있으면 resolveDate로 파싱해서(형식이 잘못되면
    //              400) 그 날짜를 씁니다. job이 없으면 selectTopBriefings(일반 모드)를, 있으면
    //              먼저 application.yml의 ai.jobs(AiTaxonomyProperties) 목록에 있는 값인지
    //              검증한 뒤 selectTopBriefingsByJob(직무별 모드)을 호출합니다. 존재하지
    //              않는 직무명이 들어오면(오타 등) 400 Bad Request로 즉시 응답합니다 —
    //              잘못된 값으로 조용히 빈 목록을 돌려주면 프론트에서 원인을 찾기 어렵기 때문입니다.
    // [무엇을 돌려주는지] 중요도 내림차순으로 정렬된 브리핑 목록(JSON 배열). job이 있으면
    //              각 항목의 jobInsight에 그 직무 재해석이 채워지고, 없으면 jobInsight는 null입니다.
    //              해당 날짜에 분석된 뉴스가 없으면 빈 배열입니다(에러 아님 — 정상적인 빈 상태).
    // @GetMapping: 데이터를 "조회"만 하고 서버 상태를 바꾸지 않는 요청이라 GET을 씁니다
    // (수동 트리거가 POST인 것과 대비됩니다).
    @GetMapping
    @Operation(
            summary = "특정 날짜의 중요도순 브리핑 조회 (일반 모드 / 직무별 모드)",
            description = "date 파라미터 없이 호출하면 오늘 분석된 뉴스 중 importance_score 내림차순으로 상위 N건(application.yml의 "
                    + "briefing.top-n, 기본 10건)을 반환합니다. date=yyyy-MM-dd로 다른 날짜를 조회할 수 있고, 그 날짜에 분석된 "
                    + "뉴스가 없으면 빈 배열을 반환합니다(에러 아님). job=IT전산|데이터분석|백엔드 중 하나를 넘기면, 같은 목록에 "
                    + "그 직무 관점의 재해석(jobInsight.whyItMatters/keySkills)이 추가로 채워집니다. job/date는 서로 독립적으로 "
                    + "함께 쓸 수 있습니다. ai.jobs에 없는 job 값이나 yyyy-MM-dd 형식이 아닌 date 값을 넘기면 400을 반환합니다."
    )
    public List<BriefingItem> list(
            @Parameter(description = "직무 필터. 생략하면 일반 모드. 예: IT전산, 데이터분석, 백엔드")
            @RequestParam(required = false) String job,
            @Parameter(description = "조회할 날짜(yyyy-MM-dd). 생략하면 오늘.")
            @RequestParam(required = false) String date
    ) {
        LocalDate targetDate = resolveDate(date);

        if (job == null || job.isBlank()) {
            List<BriefingRow> rows = briefingMapper.selectTopBriefings(targetDate, briefingProperties.getTopN());
            return rows.stream().map(row -> toItem(row, false)).toList();
        }

        if (!aiTaxonomyProperties.getJobs().contains(job)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "지원하지 않는 직무입니다. 사용 가능한 값: " + aiTaxonomyProperties.getJobs());
        }

        List<BriefingRow> rows = briefingMapper.selectTopBriefingsByJob(
                job, targetDate, briefingProperties.getTopN(), briefingProperties.getJobHighlightMinScore());
        return rows.stream().map(row -> toItem(row, true)).toList();
    }

    // [무엇을 받아서] date 쿼리 파라미터(선택, yyyy-MM-dd). 생략하면 오늘.
    // [무엇을 하고] daily_highlight에서 그 날짜의 "오늘 한 줄 요약"을 조회합니다. job
    //              파라미터가 없는 이유: 이 요약은 직무 필터 없는 전체 뉴스를 대상으로
    //              계산되므로(요구사항), 직무별로 달라지지 않습니다.
    // [무엇을 돌려주는지] 그 날짜에 요약이 있으면 200 + DailyHighlightItem, 없으면
    //              204 No Content입니다(에러 아님 — GET /api/briefings가 빈 배열로
    //              응답하는 것과 같은 철학. 재료 뉴스가 부족했거나 아직 배치가 안 돈
    //              경우가 정상적으로 있을 수 있기 때문입니다).
    @GetMapping("/highlight")
    @Operation(
            summary = "오늘(또는 지정한 날짜)의 한 줄 요약 조회",
            description = "그날 importance_score가 기준 이상인 뉴스들을 관통하는 공통 흐름을 LLM이 뽑아낸 한 문장을 반환합니다. "
                    + "재료 뉴스가 너무 적었거나 아직 계산되지 않은 날짜는 204 No Content를 반환합니다(에러 아님)."
    )
    public ResponseEntity<DailyHighlightItem> highlight(
            @Parameter(description = "조회할 날짜(yyyy-MM-dd). 생략하면 오늘.")
            @RequestParam(required = false) String date
    ) {
        LocalDate targetDate = resolveDate(date);
        DailyHighlightItem item = briefingMapper.selectHighlight(targetDate);
        return item == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(item);
    }

    // [무엇을 받아서] date 쿼리 파라미터 원본 문자열(없으면 null).
    // [무엇을 하고] null/빈 문자열이면 오늘 날짜를 씁니다. 값이 있으면 yyyy-MM-dd로
    //              파싱을 시도하고, "2026-13-45"처럼 형식이 잘못됐으면(DateTimeParseException)
    //              400 Bad Request로 즉시 응답합니다 — job 값 검증과 같은 방식으로, 잘못된
    //              입력을 조용히 무시하지 않고 바로 알려줍니다.
    // [무엇을 돌려주는지] 실제 조회에 쓸 LocalDate.
    private LocalDate resolveDate(String date) {
        if (date == null || date.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "날짜 형식이 올바르지 않습니다(yyyy-MM-dd). 입력값: " + date);
        }
    }

    // [무엇을 받아서] DB에서 읽은 가공 전 행(BriefingRow)과, 직무별 모드였는지 여부를 받습니다.
    // [무엇을 하고] industriesCsv(콤마 문자열)를 List&lt;String&gt;으로 쪼개고, 직무별
    //              모드일 때만 row의 job/whyItMatters/keySkills로 JobAnalysisResult를
    //              만들어 jobInsight에 채웁니다(일반 모드는 그 컬럼들을 애초에 select하지
    //              않았으므로 row에 값이 없어 null로 둡니다).
    // [무엇을 돌려주는지] API 응답용 불변 객체(BriefingItem).
    private BriefingItem toItem(BriefingRow row, boolean jobMode) {
        List<String> industries = row.getIndustriesCsv() == null
                ? List.of()
                : Arrays.asList(row.getIndustriesCsv().split(","));

        JobAnalysisResult jobInsight = jobMode
                ? new JobAnalysisResult(row.getJob(), row.getWhyItMatters(), row.getKeySkills(),
                        row.getJobImportanceScore())
                : null;
        Boolean isJobHighlighted = jobMode ? row.isJobHighlighted() : null;

        return new BriefingItem(
                row.getNewsId(),
                row.getTitle(),
                row.getUrl(),
                row.getPublishedAt(),
                row.getSummary(),
                row.getImportanceScore(),
                industries,
                jobInsight,
                isJobHighlighted
        );
    }
}
