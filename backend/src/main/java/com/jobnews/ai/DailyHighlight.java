package com.jobnews.ai;

import java.time.LocalDate;

/**
 * [전체 흐름에서의 위치] "오늘 한 줄 요약" 기능이 만들어내는 결과를 표현하는 모델
 * 클래스입니다. 뉴스 1건당 1행인 NewsAnalysis와 달리, 날짜 1일당 1행이 daily_highlight
 * 테이블에 저장됩니다 — 그날 importance_score가 높은 뉴스들을 관통하는 공통 흐름을
 * DailyHighlightService가 LLM으로 뽑아낸 뒤 이 객체로 담아 저장합니다.
 */
public class DailyHighlight {

    // 이 요약이 어느 날짜("오늘의 브리핑"과 같은 기준 — BriefingMapper의 날짜 조건 참고)에
    // 대한 것인지. daily_highlight 테이블의 기본키입니다.
    private LocalDate briefingDate;
    // LLM이 뽑아낸 한 문장. 공통 흐름이 없으면 그 사실을 정직하게 담은 문장일 수도 있습니다.
    private String headline;
    // 이 요약이 몇 건의 뉴스(importance_score 기준 이상)를 근거로 만들어졌는지.
    private int basedOnCount;

    // [무엇을 받아서] 아무것도 받지 않는 기본 생성자입니다.
    // [왜 필요한지] MyBatis가 DB 조회 결과를 객체로 변환할 때 필요합니다.
    public DailyHighlight() {
    }

    // [무엇을 받아서] 저장할 요약에 필요한 정보(날짜, 문장, 근거 건수)를 받습니다.
    // [왜 필요한지] DailyHighlightService가 upsert할 때 이 생성자를 사용합니다.
    public DailyHighlight(LocalDate briefingDate, String headline, int basedOnCount) {
        this.briefingDate = briefingDate;
        this.headline = headline;
        this.basedOnCount = basedOnCount;
    }

    public LocalDate getBriefingDate() {
        return briefingDate;
    }

    public void setBriefingDate(LocalDate briefingDate) {
        this.briefingDate = briefingDate;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public int getBasedOnCount() {
        return basedOnCount;
    }

    public void setBasedOnCount(int basedOnCount) {
        this.basedOnCount = basedOnCount;
    }
}
