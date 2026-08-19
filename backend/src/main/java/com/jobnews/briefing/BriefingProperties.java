package com.jobnews.briefing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 단계의 설정값을 담는 클래스입니다.
 * 지금은 "하루에 상위 몇 건을 보여줄지" 하나뿐입니다. 이 값을 코드에 하드코딩하지
 * 않고 application.yml의 briefing.top-n으로 뺀 이유는, 서비스 운영 중 "보여주는
 * 개수를 좀 늘리자/줄이자" 같은 조정이 코드 배포 없이 설정 변경만으로 가능하게 하기 위함입니다.
 */
@Component
@ConfigurationProperties(prefix = "briefing")
public class BriefingProperties {

    // 하루에 중요도 상위 몇 건까지 브리핑에 보여줄지. application.yml 기본값 10.
    private int topN;

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }
}
