package com.jobnews.briefing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 단계의 설정값을 담는 클래스입니다.
 * 값을 코드에 하드코딩하지 않고 application.yml의 briefing.*로 뺀 이유는, 서비스
 * 운영 중 "보여주는 개수를 늘리자/줄이자", "1순위 기준 점수를 조정하자" 같은 변경이
 * 코드 배포 없이 설정 변경만으로 가능하게 하기 위함입니다.
 */
@Component
@ConfigurationProperties(prefix = "briefing")
public class BriefingProperties {

    // 하루에 중요도 상위 몇 건까지 브리핑에 보여줄지. application.yml 기본값 10.
    private int topN;
    // 직무 탭에서, 그 직무의 importance_score(news_job_analysis)가 이 값 이상이면
    // "1순위"(⭐로 표시될 수 있는 강한 연관 뉴스)로 우선 채웁니다. application.yml 기본값 8.
    private int jobHighlightMinScore;

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    public int getJobHighlightMinScore() {
        return jobHighlightMinScore;
    }

    public void setJobHighlightMinScore(int jobHighlightMinScore) {
        this.jobHighlightMinScore = jobHighlightMinScore;
    }
}
