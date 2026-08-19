package com.jobnews.collector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * [전체 흐름에서의 위치] "수집" 단계 중 "기사 원문 크롤링"의 핵심 실행부입니다.
 * RssFetcher가 RSS 피드 자체를 가져오는 역할이라면, 이 클래스는 그렇게 얻은 기사
 * 하나하나의 url로 직접 들어가서 실제 기사 페이지(HTML)를 가져오고, 언론사별로
 * 등록된 CSS 셀렉터(ArticleSelectorProperties)를 이용해 광고나 메뉴 같은 군더더기를
 * 뺀 "본문 텍스트만" 뽑아냅니다. RSS description(80~100자 짧은 요약)만으로는 AI가
 * 근거 있는 요약/직무 연결을 만들기 어렵기 때문에 추가된 단계입니다.
 */
// @Component: 이 클래스의 객체를 스프링이 관리하게 해서, NewsCollectorService가
// 생성자로 자동 주입받을 수 있게 합니다.
@Component
public class ArticleContentFetcher {

    // 기사 페이지 응답을 무한정 기다리지 않도록 최대 대기 시간을 못박아 둔 값입니다.
    // RSS 수집(RssFetcher)과 동일하게 10초로 맞췄습니다.
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final ArticleSelectorProperties selectorProperties;

    // [무엇을 받아서] WebClient(HTTP 요청 도구)와 ArticleSelectorProperties(언론사별
    //              셀렉터 설정)를 스프링이 자동으로 주입해줍니다.
    public ArticleContentFetcher(WebClient webClient, ArticleSelectorProperties selectorProperties) {
        this.webClient = webClient;
        this.selectorProperties = selectorProperties;
    }

    /**
     * [무엇을 받아서] 크롤링할 기사의 url과, 이 기사가 어느 언론사(source) 소속인지를 받습니다.
     * [무엇을 하고]
     *   1) source 이름으로 등록된 CSS 셀렉터를 찾습니다. 등록이 안 되어 있으면 바로 실패 처리합니다
     *      (예: 새 RSS 소스를 추가했는데 아직 셀렉터를 안 정해준 경우를 놓치지 않기 위해서입니다).
     *   2) WebClient로 해당 url에 HTTP GET 요청을 보내 HTML 전체를 문자열로 받아옵니다.
     *   3) Jsoup(HTML을 분석해서 원하는 태그를 CSS 셀렉터로 뽑아낼 수 있게 해주는 라이브러리)으로
     *      HTML을 파싱하고, 셀렉터에 해당하는 첫 번째 요소를 찾습니다.
     *   4) 그 요소 안의 태그(광고, 스크립트 등)는 다 걷어내고 순수 텍스트만 뽑습니다.
     * [무엇을 돌려주는지] 추출된 본문 텍스트(String). 실패하면 null이 아니라 예외를 던집니다
     *              (실패를 "조용히 null 반환"하지 않고 예외로 알리는 이유는, 호출하는 쪽에서
     *              "실패했다"는 사실을 로그로 남기도록 강제하기 위해서입니다 — 조용한 실패는
     *              나중에 원인을 찾기 어렵게 만듭니다).
     * [왜 필요한지] "URL만 있으면 본문 텍스트를 뽑아준다"는 기능을 한 곳에 모아두면,
     *              NewsCollectorService는 크롤링 세부 구현(Jsoup 사용법 등)을 몰라도 됩니다.
     */
    public String fetch(String url, String source) {
        String selector = selectorProperties.selectorFor(source);
        if (selector == null) {
            // 비즈니스 용어 설명: "셀렉터 매핑"이 없다는 것은 이 언론사에 대해 본문을
            // 어디서 찾아야 할지 우리가 아직 등록해두지 않았다는 뜻입니다. 잘못된 셀렉터로
            // 엉뚱한 내용을 긁어오는 것보다, 아예 시도하지 않고 실패로 처리하는 것이 안전합니다.
            throw new ArticleFetchException("No selector configured for source: " + source);
        }

        try {
            String html = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT);

            // Jsoup.parse(html, url): 두 번째 인자(url)는 "이 HTML 안의 상대 경로 링크를
            // 해석할 때 기준이 되는 주소"입니다. 본문 텍스트만 뽑을 것이라 지금 당장은
            // 크게 중요하지 않지만, Jsoup의 표준 사용법을 따랐습니다.
            Document doc = Jsoup.parse(html, url);
            Element body = doc.selectFirst(selector);

            if (body == null) {
                // if: 셀렉터 자체는 설정되어 있지만, 실제 페이지에 그 태그가 없는 경우입니다
                // (언론사가 HTML 구조를 바꿨을 가능성이 큼). 이것도 실패로 처리해서, 나중에
                // 로그를 보고 "이 언론사 셀렉터를 다시 확인해야 한다"는 것을 알 수 있게 합니다.
                throw new ArticleFetchException("Selector matched nothing (site layout may have changed): "
                        + source + " / " + selector);
            }

            // text(): 태그(<div>, <p>, <img> 등)는 전부 제거하고 사람이 읽는 글자만 남깁니다.
            return body.text();
        } catch (ArticleFetchException e) {
            // 위에서 우리가 이미 의미를 붙여 던진 예외는 그대로 다시 던집니다(불필요하게
            // 한 번 더 감싸지 않기 위함).
            throw e;
        } catch (Exception e) {
            // 그 외(네트워크 오류, 타임아웃, HTML 파싱 실패 등)는 전부 ArticleFetchException으로
            // 통일해서 던집니다. RssFetcher가 RSS 실패를 RssFetchException으로 통일하는 것과
            // 같은 이유입니다 — 호출하는 쪽이 실패 종류를 일일이 구분하지 않아도 되게 하기 위함.
            throw new ArticleFetchException("Failed to fetch article content: " + url, e);
        }
    }
}
