package com.jobnews.collector;

import com.jobnews.news.News;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "수집" 단계의 핵심 실행부입니다. RSS 소스 하나(예: 전자신문)를
 * 받아서, 실제로 인터넷을 통해 그 주소의 RSS XML을 가져오고(HTTP 요청),
 * 그 XML을 사람이 읽을 수 있는 기사 목록(News 객체 리스트)으로 바꿔주는 일을 합니다.
 * 이 클래스는 "가져오고 변환하는 것"까지만 하고, 실패 시 재시도나 여러 소스를
 * 순회하는 일은 NewsCollectorService가 담당합니다.
 */
// @Component: RssFetcher 객체를 스프링이 만들어서 관리하게 하는 표시입니다.
// 안 쓰면 NewsCollectorService 생성자에서 이 클래스를 자동으로 주입받을 수 없습니다.
@Component
public class RssFetcher {

    // RSS 서버 응답을 무한정 기다리지 않도록 최대 대기 시간을 10초로 못박아 둔 값입니다.
    // 이 값이 없으면 응답이 느리거나 멈춘 서버 때문에 수집 전체가 영원히 멈춰버릴 수 있습니다.
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    // config 패키지(WebClientConfig)가 미리 만들어둔 HTTP 클라이언트를 주입받아 사용합니다.
    private final WebClient webClient;

    // [무엇을 받아서] 스프링이 자동으로 만들어준 WebClient를 받습니다(생성자 주입).
    // [무엇을 하고 돌려주는지] 받은 것을 필드에 저장합니다.
    public RssFetcher(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * [무엇을 받아서] RSS 소스 하나(이름 + 주소, 예: "전자신문" + Section901.xml 주소)를 받습니다.
     * [무엇을 하고]
     *   1) WebClient로 해당 주소에 HTTP GET 요청을 보내 RSS XML을 바이트(byte[])로 받아옵니다.
     *   2) Rome 라이브러리(XmlReader + SyndFeedInput)로 그 XML을 파싱해서 기사 목록을 얻습니다.
     *      XmlReader를 쓰는 이유는, RSS 파일마다 인코딩(UTF-8, EUC-KR 등)이 다를 수 있는데
     *      이 클래스가 알아서 올바른 인코딩을 감지해서 한글이 깨지지 않게 읽어주기 때문입니다.
     *   3) 파싱된 기사(SyndEntry) 하나하나를 이 프로젝트의 News 객체로 변환합니다.
     * [무엇을 돌려주는지] 변환된 News 객체들의 목록(List)을 돌려줍니다.
     * [왜 필요한지] "RSS 원본을 가져와서 우리 시스템이 이해하는 형태(News)로 바꾸는" 이
     *              변환 과정이 없으면, 이후 단계(중복 체크·저장)가 RSS의 원래 형식을
     *              직접 이해해야 해서 코드가 뒤섞이게 됩니다.
     * [에러 처리 의도] try/catch로 네트워크 오류, 타임아웃, XML 파싱 오류 등 어떤 이유로 실패하든
     *              전부 RssFetchException 하나로 통일해서 다시 던집니다. 호출하는 쪽
     *              (NewsCollectorService)이 실패 원인을 종류별로 나눠 처리할 필요 없이,
     *              "실패하면 재시도한다"는 규칙 하나만 적용하면 되도록 하기 위한 의도입니다.
     */
    public List<News> fetch(RssSourceProperties.Source source) {
        try {
            byte[] body = webClient.get()
                    .uri(source.getUrl())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    // block(TIMEOUT): 원래 WebClient는 "비동기(요청을 보내놓고 다른 일을 하다가
                    // 응답이 오면 처리)" 방식인데, 여기서는 순서대로(동기적으로) 처리하는 편이
                    // 코드가 단순해서 block()으로 "응답이 올 때까지 여기서 기다린다"고 명시했습니다.
                    // TIMEOUT(10초)을 넘기면 예외를 던지고 아래 catch로 넘어갑니다.
                    .block(TIMEOUT);

            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new ByteArrayInputStream(body)));

            return feed.getEntries().stream()
                    .map(entry -> toNews(entry, source.getName()))
                    .toList();
        } catch (Exception e) {
            // 원인이 무엇이든(네트워크 오류, 타임아웃, XML 파싱 실패 등) 여기서 한 번 잡아서
            // 우리 프로젝트의 RssFetchException으로 바꿔 다시 던집니다.
            throw new RssFetchException("Failed to fetch RSS: " + source.getUrl(), e);
        }
    }

    // [무엇을 받아서] Rome 라이브러리가 파싱한 기사 하나(SyndEntry)와, 이 기사가 어느
    //              소스에서 왔는지(sourceName)를 받습니다.
    // [무엇을 하고] Rome의 형식을 우리 프로젝트의 News 형식으로 옮겨 담습니다.
    //              description은 RSS에 요약이 없을 수도 있어 null 체크를 하고,
    //              publishedAt도 RSS에 발행일이 없을 수 있어 null 체크를 합니다.
    //              날짜는 Rome이 주는 java.util.Date를 이 프로젝트가 쓰는
    //              LocalDateTime(더 다루기 쉬운 최신 날짜 타입)으로 변환합니다.
    // [무엇을 돌려주는지] 변환이 끝난 News 객체 하나를 돌려줍니다.
    private News toNews(SyndEntry entry, String sourceName) {
        String description = entry.getDescription() != null ? entry.getDescription().getValue() : null;
        LocalDateTime publishedAt = entry.getPublishedDate() != null
                ? entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;

        return new News(entry.getLink(), entry.getTitle(), description, sourceName, publishedAt);
    }
}
