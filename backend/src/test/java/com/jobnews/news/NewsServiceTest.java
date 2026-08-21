package com.jobnews.news;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NewsServiceTest {

    @Autowired
    private NewsService newsService;

    @Test
    void savesNewUrlAndSkipsDuplicateUrl() {
        News news = new News(
                "https://example.com/news/test-article",
                "테스트 기사",
                "테스트소스",
                LocalDateTime.now()
        );

        boolean firstSave = newsService.saveIfNew(news);
        boolean secondSave = newsService.saveIfNew(news);

        assertThat(firstSave).isTrue();
        assertThat(secondSave).isFalse();
    }
}
