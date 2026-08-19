package com.jobnews.ai;

import java.time.LocalDateTime;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계에서 만들어지는 "공통 정보"를 표현하는
 * 모델 클래스입니다. news 테이블(원본 수집 데이터) 1건당 이 클래스의 객체 1개가
 * news_analysis 테이블에 1행으로 저장됩니다. 직무를 아직 선택하지 않은 사용자에게
 * 보여줄 "일반 모드" 브리핑이 바로 이 정보(summary)입니다. 직무별로 다르게 보여줄
 * 내용은 이 클래스가 아니라 NewsJobAnalysis가 담당합니다.
 */
public class NewsAnalysis {

    private Long id;
    // 이 분석이 어느 뉴스(news 테이블의 id)에 대한 것인지 가리키는 외래키 값입니다.
    // news_analysis 테이블에 news_id가 UNIQUE로 걸려 있어서, 같은 뉴스에 대한 공통
    // 분석은 항상 최대 1개만 존재할 수 있습니다(재분석을 막는 안전장치).
    private Long newsId;
    // AI(OpenAI)가 뉴스 원문(news.description)을 읽고 만든 짧은 요약입니다.
    private String summary;
    // 이 뉴스가 IT전산/데이터분석/백엔드 취준생에게 얼마나 중요한지 AI가 매긴 1~10점입니다.
    // 브리핑 조회(briefing 패키지)에서 이 값을 기준으로 정렬해 상위 N건만 보여줍니다.
    private int importanceScore;
    // 이 분석이 만들어진 시각. DB의 기본값(now())으로 자동 채워집니다.
    private LocalDateTime analyzedAt;

    // [무엇을 받아서] 아무것도 받지 않는 기본 생성자입니다.
    // [왜 필요한지] MyBatis가 DB 조회 결과를 객체로 변환할 때 필요합니다(News.java와 동일한 이유).
    public NewsAnalysis() {
    }

    // [무엇을 받아서] AI 분석 결과를 저장할 때 필요한 정보(어느 뉴스인지, 요약, 중요도 점수)를 받습니다.
    // [무엇을 하고 돌려주는지] 각 필드에 값을 채워 넣습니다. id와 analyzedAt은 저장 시점에
    //              DB가 채워주므로 비워둡니다.
    // [왜 필요한지] NewsAnalysisSaver가 OpenAI 응답을 저장할 때 이 생성자를 사용합니다.
    public NewsAnalysis(Long newsId, String summary, int importanceScore) {
        this.newsId = newsId;
        this.summary = summary;
        this.importanceScore = importanceScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(int importanceScore) {
        this.importanceScore = importanceScore;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
