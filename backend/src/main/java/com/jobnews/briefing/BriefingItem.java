package com.jobnews.briefing;

import com.jobnews.ai.JobAnalysisResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 API(BriefingController)가 실제로
 * 클라이언트(프론트엔드)에게 JSON으로 돌려주는 응답 항목입니다. 기사 원문은 애초에
 * 어떤 테이블에도 저장되지 않으므로 여기 포함될 수도 없고, AI가 사실관계를 재구성해
 * 만든 summary(짧은 재요약)만 내보냅니다(저작권 리스크 완화).
 * jobInsight는 사용자가 특정 직무(IT전산/데이터분석/백엔드)를 선택했을 때만 채워지는
 * "그 직무 관점의 재해석"입니다(핵심기능3). 직무를 선택하지 않은 일반 모드에서는
 * null입니다. ai 패키지의 JobAnalysisResult를 그대로 재사용합니다 — job/whyItMatters/
 * keySkills 모양이 정확히 같고, SummaryReviewItem에서도 같은 재사용 방식을 씁니다.
 */
public class BriefingItem {

    private final Long newsId;
    private final String title;
    private final String url;
    private final LocalDateTime publishedAt;
    private final String summary;
    private final int importanceScore;
    private final List<String> industries;
    private final JobAnalysisResult jobInsight;
    // 직무 탭에서만 채워지는 플래그입니다(일반 모드는 null). true면 그 직무의
    // importance_score가 briefing.job-highlight-min-score 이상이라 1순위로 뽑힌
    // 뉴스라는 뜻 — 프론트에서 ⭐ 표시에 씁니다. getter 이름을 관례적인 isJobHighlighted()가
    // 아니라 getIsJobHighlighted()로 지은 이유: boolean getter의 "is" 접두사는 Jackson이
    // JSON 프로퍼티명에서 자동으로 벗겨내는 관례가 있어서(isJobHighlighted() → JSON 키
    // "jobHighlighted"), 응답 JSON 키를 정확히 "isJobHighlighted"로 만들려면 getter
    // 이름 자체에 "get"을 붙여야 합니다.
    private final Boolean isJobHighlighted;

    public BriefingItem(Long newsId, String title, String url, LocalDateTime publishedAt,
                         String summary, int importanceScore, List<String> industries,
                         JobAnalysisResult jobInsight, Boolean isJobHighlighted) {
        this.newsId = newsId;
        this.title = title;
        this.url = url;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.importanceScore = importanceScore;
        this.industries = industries;
        this.jobInsight = jobInsight;
        this.isJobHighlighted = isJobHighlighted;
    }

    public Long getNewsId() {
        return newsId;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getSummary() {
        return summary;
    }

    public int getImportanceScore() {
        return importanceScore;
    }

    public List<String> getIndustries() {
        return industries;
    }

    public JobAnalysisResult getJobInsight() {
        return jobInsight;
    }

    public Boolean getIsJobHighlighted() {
        return isJobHighlighted;
    }
}
