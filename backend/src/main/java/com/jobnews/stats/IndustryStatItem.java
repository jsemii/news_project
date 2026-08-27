package com.jobnews.stats;

/**
 * [전체 흐름에서의 위치] 관리자 통계 대시보드(핵심기능과는 별개로, 운영자가 서비스
 * 현황을 파악하기 위한 화면)의 "산업별 뉴스 건수" API가 돌려주는 응답 항목입니다.
 * news_industry 테이블을 산업(industry)별로 묶어서 몇 건씩 있는지 센 결과 1행이
 * 이 객체 1개에 대응합니다.
 */
public class IndustryStatItem {

    private final String industry;
    // COUNT(*)는 PostgreSQL에서 bigint(64비트 정수)를 돌려주기 때문에 자바의
    // Long으로 받습니다. 그리고 이 클래스는 setter 없이 생성자로만 값을 채우는
    // 불변 객체라, MyBatis가 리플렉션으로 이 생성자를 찾아야 하는데, 매개변수를
    // primitive long으로 선언하면 그 생성자를 못 찾는 문제가 실제로 있었습니다
    // (docs/troubleshooting.md 26번 항목 참고) — 그래서 primitive가 아니라
    // 박싱된 타입(Long)을 씁니다.
    private final Long count;

    public IndustryStatItem(String industry, Long count) {
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
