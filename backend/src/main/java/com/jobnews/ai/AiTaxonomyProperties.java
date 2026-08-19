package com.jobnews.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계에서 사용할 산업/직무의 "정해진 목록"을
 * 담는 설정 클래스입니다. AI가 아무 이름이나 만들어내지 않고, 이 목록 안에서만
 * 골라 태깅하도록 OpenAiClient가 프롬프트를 만들 때 이 값을 사용합니다. 목록을
 * 코드에 하드코딩하지 않고 application.yml의 ai.industries / ai.jobs에서 읽어오므로,
 * 나중에 산업/직무 종류를 조정할 때 코드를 고칠 필요가 없습니다.
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AiTaxonomyProperties {

    // 산업 8개 목록(금융, 제조, 반도체 등). news_industry.industry에 들어갈 수 있는 값의 전부입니다.
    private List<String> industries = new ArrayList<>();
    // 직무 3개 목록(IT전산, 데이터분석, 백엔드). 뉴스마다 이 3개 전부에 대해 분석이 만들어집니다.
    private List<String> jobs = new ArrayList<>();

    public List<String> getIndustries() {
        return industries;
    }

    public void setIndustries(List<String> industries) {
        this.industries = industries;
    }

    public List<String> getJobs() {
        return jobs;
    }

    public void setJobs(List<String> jobs) {
        this.jobs = jobs;
    }
}
