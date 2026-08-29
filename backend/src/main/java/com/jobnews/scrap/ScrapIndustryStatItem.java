package com.jobnews.scrap;

/**
 * [전체 흐름에서의 위치] "내 관심 산업" 위젯(로그인한 사용자가 스크랩한 뉴스들을
 * 산업별로 묶어 몇 건인지 보여주는 기능)의 응답 항목입니다. 관리자 통계의
 * stats.IndustryStatItem과 필드 모양은 같지만, 저건 "전체 뉴스 기준" 집계이고
 * 이건 "내 스크랩 기준" 집계라 의미가 다릅니다 — 패키지 간 결합을 늘리지 않기
 * 위해 재사용하지 않고 scrap 패키지 안에 따로 둡니다.
 */
public class ScrapIndustryStatItem {

    private final String industry;
    // COUNT(*)는 PostgreSQL에서 bigint를 돌려주므로 Long을 씁니다. 불변 객체라
    // primitive가 아니라 박싱 타입을 쓰는 이유는 stats 패키지의 DTO들과 동일합니다
    // (MyBatis <constructor> 매핑이 primitive 매개변수를 가진 생성자를 못 찾는
    // 문제를 이미 겪었기 때문 — docs/troubleshooting.md 26번 항목).
    private final Long count;

    public ScrapIndustryStatItem(String industry, Long count) {
        this.industry = industry;
        this.count = count;
    }

    public String getIndustry() {
        return industry;
    }

    public Long getCount() {
        return count;
    }
}
