package com.jobnews.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private final NewsMapper newsMapper;

    public NewsService(NewsMapper newsMapper) {
        this.newsMapper = newsMapper;
    }

    /**
     * URL이 이미 저장되어 있으면 건너뛰고, 신규 URL이면 저장한다.
     *
     * @return 실제로 저장했으면 true, 중복이라 건너뛰었으면 false
     */
    public boolean saveIfNew(News news) {
        if (newsMapper.existsByUrl(news.getUrl())) {
            log.debug("Duplicate news skipped: {}", news.getUrl());
            return false;
        }
        newsMapper.insert(news);
        log.info("News saved: {}", news.getUrl());
        return true;
    }
}
