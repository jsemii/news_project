package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계에서 뉴스 하나를 특정 직무 관점으로
 * 재해석한 내용 하나를 표현하는 모델 클래스입니다. 산업 태그와 달리 "관련 있는 직무만
 * 선택"하는 게 아니라, application.yml의 ai.jobs에 정의된 3개 직무(IT전산/데이터분석/
 * 백엔드) 전부에 대해 항상 분석을 만들기로 했기 때문에, news_job_analysis 테이블에는
 * 뉴스 1건당 이 클래스의 객체가 정확히 3개(직무 수만큼) 행으로 저장됩니다. 사용자가
 * 프론트엔드에서 자기 직무를 선택하면, 그 직무에 해당하는 행 하나를 보여주는 방식으로
 * "직무별 맞춤 브리핑"(핵심기능3)에 사용될 예정입니다.
 */
public class NewsJobAnalysis {

    private Long id;
    // 이 직무별 분석이 어느 뉴스에 대한 것인지 가리키는 외래키 값입니다.
    private Long newsId;
    // 어느 직무 관점의 분석인지("IT전산" / "데이터분석" / "백엔드" 중 하나).
    private String job;
    // "이 뉴스가 이 직무에게 왜 중요한지"를 AI가 풀어 쓴 설명입니다.
    private String whyItMatters;
    // 이 뉴스와 관련해서 이 직무 종사자에게 도움이 될 만한 역량/기술 키워드입니다.
    private String keySkills;
    // 이 뉴스가 "이 직무 하나"에 얼마나 중요한지(1~10, 다른 직무와의 상대평가가 아님).
    private int importanceScore;

    public NewsJobAnalysis() {
    }

    // [무엇을 받아서] 이 분석이 속한 뉴스 id, 직무 이름, AI가 만든 내용(whyItMatters,
    //              keySkills, importanceScore)을 받습니다.
    // [왜 필요한지] NewsStructuringService가 OpenAI 응답의 jobs 목록(직무 3개)을 순회하면서
    //              직무별 NewsJobAnalysis 객체를 하나씩 만들 때 사용합니다.
    public NewsJobAnalysis(Long newsId, String job, String whyItMatters, String keySkills, int importanceScore) {
        this.newsId = newsId;
        this.job = job;
        this.whyItMatters = whyItMatters;
        this.keySkills = keySkills;
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

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getWhyItMatters() {
        return whyItMatters;
    }

    public void setWhyItMatters(String whyItMatters) {
        this.whyItMatters = whyItMatters;
    }

    public String getKeySkills() {
        return keySkills;
    }

    public void setKeySkills(String keySkills) {
        this.keySkills = keySkills;
    }

    public int getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(int importanceScore) {
        this.importanceScore = importanceScore;
    }
}
