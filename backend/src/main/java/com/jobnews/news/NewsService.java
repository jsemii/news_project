package com.jobnews.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * [전체 흐름에서의 위치] "저장" 단계의 비즈니스 로직(=단순 DB 명령이 아니라, "저장해도
 * 되는지 먼저 판단한다"는 업무 규칙)을 담당하는 클래스입니다. collector 패키지의
 * NewsCollectorService가 RSS에서 기사를 가져온 뒤, 기사 하나하나를 이 클래스의
 * saveIfNew()에 넘겨서 "저장할지 말지"를 맡깁니다.
 */
// @Service: "이 클래스는 비즈니스 로직을 담당하는 서비스 계층 객체다"라는 표시이자,
// 스프링이 이 클래스의 객체를 하나 만들어서 컨테이너에 등록하게 만드는 어노테이션입니다.
// 안 쓰면 다른 클래스(NewsCollectorService 등)의 생성자에 NewsService를 자동으로
// 주입할 수 없어서 앱 시작 시 에러가 납니다.
@Service
public class NewsService {

    // 로그(실행 중 무슨 일이 있었는지 기록을 남기는 것)를 남기기 위한 도구입니다.
    // 화면에 직접 출력(System.out.println)하는 대신 이걸 쓰면, 나중에 로그 레벨(info/debug 등)로
    // 필요한 것만 골라 보거나 파일로 저장하는 것이 가능해집니다.
    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    // DB에 실제로 접근하는 창구(NewsMapper)를 필드로 갖고 있습니다.
    private final NewsMapper newsMapper;

    // [무엇을 받아서] 스프링이 자동으로 만들어준 NewsMapper 구현체를 받습니다(생성자 주입).
    // [무엇을 하고] 받은 것을 필드에 저장해서, 이 클래스의 다른 메서드에서 쓸 수 있게 합니다.
    // [왜 필요한지] new NewsService(...)로 직접 만들지 않고 스프링이 대신 만들어주는 방식을
    //              쓰면, NewsMapper의 실제 구현이 바뀌어도(예: 나중에 캐시를 추가해도) 이
    //              코드를 고칠 필요가 없습니다.
    public NewsService(NewsMapper newsMapper) {
        this.newsMapper = newsMapper;
    }

    /**
     * [무엇을 받아서] 저장하고 싶은 뉴스 기사 하나(News 객체)를 받습니다.
     * [무엇을 하고] "URL 중복 제거" 비즈니스 규칙을 적용합니다 — 먼저 이 기사의 url이
     *              이미 DB에 있는지 확인하고, 있으면 저장을 건너뛰고, 없으면 새로 저장합니다.
     *              (실제 중복 판정은 NewsMapper.existsByUrl → NewsMapper.xml의
     *              SELECT EXISTS 쿼리가, 저장은 news 테이블의 url UNIQUE 제약이 이중으로
     *              보장합니다.)
     * [무엇을 돌려주는지] 실제로 새로 저장했으면 true, 중복이라 건너뛰었으면 false.
     * [왜 필요한지] 같은 RSS를 30분마다 반복해서 수집하기 때문에, 이미 본 기사를 그대로
     *              다시 저장하면 DB에 같은 기사가 계속 쌓이게 됩니다. 이를 막기 위한
     *              핵심 로직입니다.
     */
    public boolean saveIfNew(News news) {
        // if: url이 이미 존재하면(중복이면) 저장하지 않고 바로 false를 돌려주고 끝냅니다.
        // "일단 저장하고 나중에 중복을 지우는" 방식 대신 "저장 전에 먼저 확인하는" 방식을
        // 택한 이유는, DB의 url UNIQUE 제약과 함께 이중으로 안전하게 막기 위해서입니다.
        if (newsMapper.existsByUrl(news.getUrl())) {
            log.debug("Duplicate news skipped: {}", news.getUrl());
            return false;
        }
        newsMapper.insert(news);
        log.info("News saved: {}", news.getUrl());
        return true;
    }

    /**
     * [무엇을 받아서] 확인하고 싶은 기사의 url을 받습니다.
     * [무엇을 하고] 그 url이 이미 news 테이블에 저장되어 있는지만 확인합니다(저장은 하지 않음).
     * [무엇을 돌려주는지] 이미 있으면 true, 없으면 false.
     * [왜 필요한지] collector 패키지가 "이미 저장된 기사면 원문 크롤링 자체를 하지 않는다"는
     *              최적화를 하기 위해, 크롤링 전에 미리 신규 여부를 판단할 방법이 필요합니다.
     *              (최종 저장은 여전히 saveIfNew()가 담당하며, 그 안에서도 한 번 더
     *              중복을 확인하므로 이중 안전장치가 유지됩니다.)
     */
    public boolean exists(String url) {
        return newsMapper.existsByUrl(url);
    }
}
