package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계에서 OpenAI 호출이나 응답 처리가 실패했을
 * 때 사용하는 전용 예외입니다. RssFetchException(RSS 수집 실패), ArticleFetchException
 * (원문 크롤링 실패)과 같은 역할을, "AI 분석" 단계에 대해 담당합니다. 네트워크 오류,
 * OpenAI의 오류 응답, 예상한 형식과 다른 JSON 응답 등 원인이 무엇이든 이 예외 하나로
 * 통일해서, NewsStructuringService가 "실패하면 재시도한다"는 규칙 하나만 적용하면
 * 되도록 합니다.
 */
public class AiStructureException extends RuntimeException {

    public AiStructureException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiStructureException(String message) {
        super(message);
    }
}
