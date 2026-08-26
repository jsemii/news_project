package com.jobnews.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * [전체 흐름에서의 위치] "AI 뉴스 구조화" 단계에서 OpenAI 분석 결과(AiAnalysisResult)를
 * news_analysis + news_industry + news_job_analysis 3개 테이블에 나눠 저장하는 역할만
 * 전담하는 클래스입니다. NewsStructuringService(Phase 5의 오케스트레이터)가 아니라
 * 이 클래스를 따로 둔 이유는 아래 @Transactional 설명을 참고하세요.
 */
@Component
public class NewsAnalysisSaver {

    private static final Logger log = LoggerFactory.getLogger(NewsAnalysisSaver.class);

    private final NewsAnalysisMapper newsAnalysisMapper;

    public NewsAnalysisSaver(NewsAnalysisMapper newsAnalysisMapper) {
        this.newsAnalysisMapper = newsAnalysisMapper;
    }

    /**
     * [무엇을 받아서] 분석 대상 뉴스의 id, 1단계 결과(GeneralAnalysisResult), 2단계 결과
     *              (JobAnalysisResult 목록, 항상 3개)를 받습니다.
     * [무엇을 하고] 공통 요약 1행 + 산업 태그 N행 + 직무별 분석 3행, 총 최대 3번의 INSERT를
     *              실행합니다.
     * [무엇을 돌려주는지] 반환값 없음.
     * [왜 필요한지] 하나의 뉴스 분석 결과가 여러 테이블에 걸쳐 저장되는데, 만약 공통 요약은
     *              저장됐는데 직무별 분석 저장 중 오류가 나면 "반쪽짜리" 데이터가 남게 됩니다.
     *              이런 데이터는 이후 "이미 분석됨"으로 착각되어(existsByNewsId가 true를
     *              반환) 다시는 재시도되지 않는 심각한 문제로 이어집니다. 1단계와 2단계
     *              LLM 호출이 완전히 끝난 뒤(NewsStructuringService에서) 이 메서드를 한 번만
     *              호출하는 것도 같은 이유입니다 — 2단계가 실패하면 1단계 결과도 아예
     *              저장하지 않고 통째로 버립니다.
     */
    // @Transactional: 이 메서드 안의 모든 INSERT를 "전부 성공하거나 전부 취소되거나" 둘 중
    // 하나로 묶어주는 어노테이션입니다. 중간에 하나라도 실패하면 이미 실행된 INSERT까지
    // 전부 롤백(취소)됩니다. 안 쓰면 위에서 설명한 "반쪽짜리 저장" 문제가 그대로 발생합니다.
    // ⚠️ 이 메서드를 NewsStructuringService 안에 두지 않고 별도 클래스(빈)로 분리한 이유:
    // Spring의 @Transactional은 프록시(가짜 대리인 객체)가 메서드 호출을 가로채는 방식으로
    // 동작하는데, 같은 클래스 안에서 "this.save(...)"처럼 자기 자신을 호출하면 프록시를
    // 거치지 않아 @Transactional이 조용히 무시됩니다(널리 알려진 스프링 함정). 그래서 저장
    // 로직만 별도 빈으로 떼어내, NewsStructuringService가 "다른 빈을 호출"하는 형태로
    // 만들어서 이 문제를 피했습니다.
    @Transactional
    public void save(Long newsId, GeneralAnalysisResult general, List<JobAnalysisResult> jobs) {
        // importance_reason은 DB에 저장하지 않고(요구사항: importance_score 컬럼만 추가),
        // 점수를 왜 그렇게 매겼는지 나중에 사람이 확인할 수 있도록 로그로만 남깁니다.
        log.info("[newsId={}] importance_score={} reason={}",
                newsId, general.getImportanceScore(), general.getImportanceReason());

        newsAnalysisMapper.insertAnalysis(
                new NewsAnalysis(newsId, general.getSummary(), general.getImportanceScore()));

        // if: AI가 관련 산업을 하나도 못 찾았다면(빈 리스트) insertIndustries를 호출하지
        // 않습니다. 빈 리스트로 그 메서드를 부르면 "VALUES" 뒤에 아무것도 없는 잘못된
        // SQL이 만들어지기 때문입니다.
        if (!general.getIndustries().isEmpty()) {
            List<NewsIndustry> industries = general.getIndustries().stream()
                    .map(industry -> new NewsIndustry(newsId, industry))
                    .toList();
            newsAnalysisMapper.insertIndustries(industries);
        }

        List<NewsJobAnalysis> jobAnalyses = jobs.stream()
                .map(job -> new NewsJobAnalysis(newsId, job.getJob(), job.getWhyItMatters(), job.getKeySkills(),
                        job.getImportanceScore()))
                .toList();
        newsAnalysisMapper.insertJobAnalyses(jobAnalyses);
    }
}
