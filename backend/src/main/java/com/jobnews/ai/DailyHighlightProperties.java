package com.jobnews.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "오늘 한 줄 요약" 기능의 임계값 설정을 담는 클래스입니다.
 * 두 값 모두 코드에 하드코딩하지 않고 application.yml의 daily-highlight.*로 뺀
 * 이유는, 운영 중 "기준 점수를 좀 낮추자/건수를 늘리자" 같은 조정이 코드 배포
 * 없이 설정 변경만으로 가능하게 하기 위함입니다(ai.structuring.max-age-days와
 * 같은 이유).
 */
@Component
@ConfigurationProperties(prefix = "daily-highlight")
public class DailyHighlightProperties {

    // 이 점수 이상인 뉴스만 "오늘 한 줄 요약"의 재료로 씁니다. application.yml 기본값 6.
    private int minImportanceScore;
    // 재료로 쓸 뉴스가 이 건수 미만이면 LLM을 아예 호출하지 않습니다(1~2건으로 "여러
    // 뉴스를 관통하는 흐름"을 억지로 짜낼 위험을 코드 레벨에서 차단). application.yml 기본값 3.
    private int minNewsCount;

    public int getMinImportanceScore() {
        return minImportanceScore;
    }

    public void setMinImportanceScore(int minImportanceScore) {
        this.minImportanceScore = minImportanceScore;
    }

    public int getMinNewsCount() {
        return minNewsCount;
    }

    public void setMinNewsCount(int minNewsCount) {
        this.minNewsCount = minNewsCount;
    }
}
