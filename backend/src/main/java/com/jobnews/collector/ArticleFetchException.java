package com.jobnews.collector;

/**
 * [전체 흐름에서의 위치] "수집" 단계 중 "기사 원문 크롤링"이 실패했을 때 사용하는
 * 전용 예외 클래스입니다. RssFetchException과 같은 역할을 하되, 실패 대상이
 * RSS 피드가 아니라 "개별 기사 웹페이지 크롤링"이라는 점이 다릅니다. 이 예외를
 * 별도 타입으로 둔 이유는, NewsCollectorService가 "RSS 자체를 못 가져온 심각한
 * 실패(재시도 필요)"와 "기사 원문 하나를 크롤링하지 못한 가벼운 실패(재시도 없이
 * content_raw만 비우고 넘어가면 되는 실패)"를 서로 다르게 처리해야 하기 때문입니다.
 */
public class ArticleFetchException extends RuntimeException {

    // [무엇을 받아서] 에러 메시지와 원인 예외(cause)를 받습니다.
    // [왜 필요한지] cause를 함께 보관해야, 나중에 로그에서 "타임아웃 때문인지",
    //              "셀렉터가 안 맞아서인지"를 구분할 수 있습니다.
    public ArticleFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    // [무엇을 받아서] 에러 메시지만 받습니다(원인이 되는 예외가 따로 없는 경우, 예: 이
    //              언론사에 등록된 셀렉터가 없는 설정 누락 상황).
    public ArticleFetchException(String message) {
        super(message);
    }
}
