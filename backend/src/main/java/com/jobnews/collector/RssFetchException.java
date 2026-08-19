package com.jobnews.collector;

/**
 * [전체 흐름에서의 위치] "수집" 단계에서 RSS를 가져오다가 실패했을 때 사용하는
 * 전용 예외(에러) 클래스입니다. 네트워크 오류든, XML 파싱 오류든, 원인이 무엇이든
 * RssFetcher는 이 하나의 예외 타입으로 통일해서 던지고, NewsCollectorService는
 * 이 예외 하나만 잡으면 "재시도가 필요한 상황"임을 알 수 있게 됩니다.
 * (에러 종류마다 재시도 처리 코드를 따로 쓰지 않아도 되게 해주는 역할입니다.)
 */
// RuntimeException을 상속했다는 것은 "unchecked 예외(=메서드 시그니처에 throws를
// 안 붙여도 되는 예외)"라는 뜻입니다. 호출하는 쪽(NewsCollectorService)에서
// 반드시 try-catch로 감싸야만 컴파일되게 강제하지 않고, 필요한 곳에서 선택적으로
// 잡아 처리할 수 있게 하기 위함입니다.
public class RssFetchException extends RuntimeException {

    // [무엇을 받아서] 에러 메시지(message)와 원래 발생했던 예외(cause, 예: 네트워크 타임아웃,
    //              XML 파싱 실패 등)를 받습니다.
    // [무엇을 하고] 부모 클래스(RuntimeException)의 생성자에 그대로 전달해서, "왜 실패했는지"에
    //              대한 정보(원인 예외 포함)를 잃지 않고 그대로 감싸서 보관합니다.
    // [왜 필요한지] cause를 함께 넘겨야, 나중에 로그에 원본 에러(네트워크 문제인지 파싱
    //              문제인지)까지 같이 찍혀서 문제를 추적하기 쉬워집니다.
    public RssFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
