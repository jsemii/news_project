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

@Component
public class RssFetcher {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public RssFetcher(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<News> fetch(RssSourceProperties.Source source) {
        try {
            byte[] body = webClient.get()
                    .uri(source.getUrl())
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block(TIMEOUT);

            SyndFeed feed = new SyndFeedInput().build(new XmlReader(new ByteArrayInputStream(body)));

            return feed.getEntries().stream()
                    .map(entry -> toNews(entry, source.getName()))
                    .toList();
        } catch (Exception e) {
            throw new RssFetchException("Failed to fetch RSS: " + source.getUrl(), e);
        }
    }

    private News toNews(SyndEntry entry, String sourceName) {
        String description = entry.getDescription() != null ? entry.getDescription().getValue() : null;
        LocalDateTime publishedAt = entry.getPublishedDate() != null
                ? entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : null;

        return new News(entry.getLink(), entry.getTitle(), description, sourceName, publishedAt);
    }
}
