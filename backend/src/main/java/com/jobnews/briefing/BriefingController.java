package com.jobnews.briefing;

import com.jobnews.ai.AiTaxonomyProperties;
import com.jobnews.ai.JobAnalysisResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 단계(핵심기능3)의 REST API입니다.
 * job 쿼리 파라미터가 없으면 예전과 완전히 같은 "일반 모드"(오늘 분석된 뉴스 중
 * 중요도 상위 N건, 공통 요약)로 동작합니다. job 파라미터로 IT전산/데이터분석/백엔드
 * 중 하나를 넘기면, 같은 뉴스 목록에 그 직무 관점의 재해석(jobInsight: whyItMatters/
 * keySkills)이 추가로 채워집니다. 별도 엔드포인트를 새로 만들지 않고 기존
 * "/api/briefings"를 파라미터로 확장한 이유는, 두 모드 모두 "오늘의 중요도순 브리핑
 * 목록"이라는 같은 리소스를 다르게 필터링한 것뿐이기 때문입니다.
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

    // [무엇을 받아서] job 쿼리 파라미터(선택, 예: ?job=IT전산). 개수는 application.yml의
    //              briefing.top-n을 그대로 씀(파라미터로 안 받음).
    // [무엇을 하고] job이 없으면 selectTopBriefings(일반 모드)를, 있으면 먼저
    //              application.yml의 ai.jobs(AiTaxonomyProperties) 목록에 있는 값인지
    //              검증한 뒤 selectTopBriefingsByJob(직무별 모드)을 호출합니다. 존재하지
    //              않는 직무명이 들어오면(오타 등) 400 Bad Request로 즉시 응답합니다 —
    //              잘못된 값으로 조용히 빈 목록을 돌려주면 프론트에서 원인을 찾기 어렵기 때문입니다.
    // [무엇을 돌려주는지] 중요도 내림차순으로 정렬된 브리핑 목록(JSON 배열). job이 있으면
    //              각 항목의 jobInsight에 그 직무 재해석이 채워지고, 없으면 jobInsight는 null입니다.
    // @GetMapping: 데이터를 "조회"만 하고 서버 상태를 바꾸지 않는 요청이라 GET을 씁니다
    // (수동 트리거가 POST인 것과 대비됩니다).
    @GetMapping
    @Operation(
            summary = "오늘의 중요도순 브리핑 조회 (일반 모드 / 직무별 모드)",
            description = "job 파라미터 없이 호출하면 오늘 분석된 뉴스 중 importance_score 내림차순으로 상위 N건(application.yml의 "
                    + "briefing.top-n, 기본 10건)을 공통 요약과 함께 반환합니다. job=IT전산|데이터분석|백엔드 중 하나를 넘기면, "
                    + "같은 목록에 그 직무 관점의 재해석(jobInsight.whyItMatters/keySkills)이 추가로 채워집니다. "
                    + "ai.jobs에 없는 값을 넘기면 400을 반환합니다."
    )
    public List<BriefingItem> list(
            @Parameter(description = "직무 필터. 생략하면 일반 모드. 예: IT전산, 데이터분석, 백엔드")
            @RequestParam(required = false) String job
    ) {
        if (job == null || job.isBlank()) {
            List<BriefingRow> rows = briefingMapper.selectTopBriefings(briefingProperties.getTopN());
            return rows.stream().map(row -> toItem(row, false)).toList();
        }

        if (!aiTaxonomyProperties.getJobs().contains(job)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "지원하지 않는 직무입니다. 사용 가능한 값: " + aiTaxonomyProperties.getJobs());
        }

        List<BriefingRow> rows = briefingMapper.selectTopBriefingsByJob(job, briefingProperties.getTopN());
        return rows.stream().map(row -> toItem(row, true)).toList();
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
                ? new JobAnalysisResult(row.getJob(), row.getWhyItMatters(), row.getKeySkills())
                : null;

        return new BriefingItem(
                row.getNewsId(),
                row.getTitle(),
                row.getUrl(),
                row.getPublishedAt(),
                row.getSummary(),
                row.getImportanceScore(),
                industries,
                jobInsight
        );
    }
}
