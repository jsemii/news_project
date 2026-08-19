package com.jobnews.ai;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화"를 한 번 실행한 결과를 요약한 DTO입니다.
 * 수동 트리거 API(StructuringController)가 호출자에게 "무슨 일이 있었는지"를
 * 바로 알려주기 위해 사용합니다. 스케줄러가 자동 실행할 때는 이 값을 그냥 로그로만
 * 남기고, 사람이 직접 호출했을 때(POST /api/structuring/run)는 HTTP 응답으로도 돌려줍니다.
 */
public class StructuringSummary {

    private final int totalFound;
    private final int filteredOut;
    private final int succeeded;
    private final int failed;
    // 이번 배치(openai.batch-size)를 처리하고 나서도 아직 분석되지 않은 채 남아있는
    // 뉴스의 전체 개수입니다. 0이 아니면, 남은 백로그를 처리하기 위해 이 API를 다시
    // 호출하거나 다음 스케줄 실행을 기다리면 됩니다.
    private final int remainingBacklog;

    public StructuringSummary(int totalFound, int filteredOut, int succeeded, int failed, int remainingBacklog) {
        this.totalFound = totalFound;
        this.filteredOut = filteredOut;
        this.succeeded = succeeded;
        this.failed = failed;
        this.remainingBacklog = remainingBacklog;
    }

    public int getTotalFound() {
        return totalFound;
    }

    public int getFilteredOut() {
        return filteredOut;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public int getRemainingBacklog() {
        return remainingBacklog;
    }
}
