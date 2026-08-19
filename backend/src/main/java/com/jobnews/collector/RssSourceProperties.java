package com.jobnews.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "수집" 단계에서 "어떤 RSS 주소들을 가져올지"를 정의하는 설정
 * 클래스입니다. 실제 값(전자신문/연합뉴스 주소 등)은 코드가 아니라
 * application.yml의 rss.sources 항목에 적혀 있고, 이 클래스는 그 yml 내용을
 * 자바 객체로 그대로 옮겨 담는 역할만 합니다. 그래서 RSS 소스를 추가/삭제할 때
 * 이 자바 코드는 건드릴 필요 없이 yml만 고치면 됩니다.
 */
// @Component: 이 클래스의 객체를 스프링이 하나 만들어서 컨테이너에 등록하게 하는
// 범용 어노테이션입니다. 안 쓰면 다른 클래스(NewsCollectorService 등)가 이 설정값을
// 주입받을 방법이 없습니다.
@Component
// @ConfigurationProperties(prefix = "rss"): application.yml에서 "rss:"로 시작하는
// 항목들을 자동으로 이 클래스의 필드에 채워 넣으라는 어노테이션입니다. 예를 들어
// yml의 rss.sources 리스트가 아래 sources 필드로 자동 매핑됩니다. 안 쓰면 yml 값을
// 하나하나 @Value("${...}")로 직접 읽어와야 해서 코드가 훨씬 번거로워집니다.
@ConfigurationProperties(prefix = "rss")
public class RssSourceProperties {

    // application.yml의 rss.sources 리스트(이름 + 주소 쌍의 목록)가 그대로 담기는 필드입니다.
    private List<Source> sources = new ArrayList<>();

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    /**
     * RSS 소스 하나(예: "전자신문" + 그 RSS 주소)를 표현하는 작은 데이터 묶음입니다.
     * yml의 "- name: 전자신문 / url: https://..." 한 항목이 이 클래스의 객체 하나에 대응됩니다.
     */
    public static class Source {
        private String name;
        private String url;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
