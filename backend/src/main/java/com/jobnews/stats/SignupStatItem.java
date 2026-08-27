package com.jobnews.stats;

import java.time.LocalDate;

/**
 * [전체 흐름에서의 위치] 관리자 통계 대시보드의 "최근 14일 일별 회원가입 추이"
 * API가 돌려주는 응답 항목입니다. DailyCollectionStatItem과 같은 이유로 오늘을
 * 포함한 최근 14일 전체(가입이 0건인 날짜도 빠짐없이)를 항상 14행으로 돌려주고,
 * provider(github/google)별 건수를 한 행에 같이 담아서 프론트가 막대 그래프
 * 하나로 바로 그릴 수 있게 합니다.
 */
public class SignupStatItem {

    private final LocalDate date;
    // count(u.id) FILTER (...)는 LEFT JOIN이 매칭되지 않아도(그 날짜에 가입이
    // 0건이어도) 항상 값이 채워지는 컬럼이라 정확히 0을 셀 수 있습니다
    // (StatsMapper.xml 주석 참고). bigint → Long, 불변 객체라 primitive 대신
    // 박싱 타입을 쓰는 이유는 IndustryStatItem과 동일합니다.
    private final Long githubCount;
    private final Long googleCount;

    public SignupStatItem(LocalDate date, Long githubCount, Long googleCount) {
        this.date = date;
        this.githubCount = githubCount;
        this.googleCount = googleCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public Long getGithubCount() {
        return githubCount;
    }

    public Long getGoogleCount() {
        return googleCount;
    }
}
