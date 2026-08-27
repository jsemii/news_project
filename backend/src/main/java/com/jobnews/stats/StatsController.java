package com.jobnews.stats;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [전체 흐름에서의 위치] 관리자 전용 통계 대시보드의 REST API입니다. 회원(users)의
 * role이 ADMIN인 경우에만 호출할 수 있고, 그 외(비로그인 또는 일반 회원)는 403
 * Forbidden을 받습니다 — 이 접근 제한은 여기 컨트롤러 코드가 아니라
 * config.SecurityConfig에서 URL 단위로 처리합니다(이 컨트롤러는 "누가 호출했는지"를
 * 전혀 신경 쓰지 않고 조회 로직에만 집중합니다). 4개 지표 모두 파라미터가 없는
 * "전체 데이터 집계" 조회라 입력값 검증 분기가 없습니다(잘못된 입력 자체가 있을 수
 * 없음).
 */
@RestController
@RequestMapping("/api/stats")
@Tag(name = "Stats", description = "관리자 전용 통계 대시보드 API (ADMIN 권한 필요)")
public class StatsController {

    private final StatsMapper statsMapper;

    public StatsController(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    // [무엇을 받아서] 파라미터 없음.
    // [무엇을 하고] 산업별 뉴스 건수를 조회합니다.
    // [무엇을 돌려주는지] 산업별 건수 목록(JSON 배열). 데이터가 아예 없으면 빈
    //              배열입니다(에러 아님 — 이 서비스의 다른 조회 API들과 같은 철학).
    @GetMapping("/industries")
    @Operation(
            summary = "산업별 뉴스 건수",
            description = "news_industry.industry로 그룹핑한 건수를 건수 내림차순으로 반환합니다."
    )
    public List<IndustryStatItem> industries() {
        return statsMapper.selectIndustryCounts();
    }

    // [무엇을 받아서] 파라미터 없음.
    // [무엇을 하고] 오늘을 포함한 최근 14일간의 일별 뉴스 수집 건수를 조회합니다.
    // [무엇을 돌려주는지] 날짜 오름차순 14개 항목의 목록(수집이 0건인 날짜도 포함).
    @GetMapping("/daily-collection")
    @Operation(
            summary = "최근 14일 일별 수집 건수",
            description = "오늘 포함 최근 14일을 항상 14개 항목으로 반환합니다(수집 0건인 날짜도 포함됨)."
    )
    public List<DailyCollectionStatItem> dailyCollection() {
        return statsMapper.selectDailyCollectionCounts();
    }

    // [무엇을 받아서] 파라미터 없음.
    // [무엇을 하고] 직무별 importance_score 평균을 조회합니다.
    // [무엇을 돌려주는지] 직무별 평균 점수 목록(평균 점수 내림차순).
    @GetMapping("/job-scores")
    @Operation(
            summary = "직무별 평균 중요도 점수",
            description = "news_job_analysis.importance_score의 직무별 평균을 평균 점수 내림차순으로 반환합니다."
    )
    public List<JobScoreStatItem> jobScores() {
        return statsMapper.selectJobAverageScores();
    }

    // [무엇을 받아서] 파라미터 없음.
    // [무엇을 하고] 규칙 기반 필터가 뉴스를 걸러낸 사유별 건수를 조회합니다.
    // [무엇을 돌려주는지] 필터링 사유별 건수 목록(건수 내림차순).
    @GetMapping("/filtered")
    @Operation(
            summary = "필터링 사유별 건수",
            description = "news_filtered_out.reason으로 그룹핑한 건수를 건수 내림차순으로 반환합니다."
    )
    public List<FilteredReasonStatItem> filtered() {
        return statsMapper.selectFilteredReasonCounts();
    }
}
