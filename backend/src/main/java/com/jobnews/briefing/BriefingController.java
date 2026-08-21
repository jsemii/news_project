package com.jobnews.briefing;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * [전체 흐름에서의 위치] "직무별 맞춤 브리핑" 조회 단계의 REST API입니다. 지금은
 * 핵심기능3(직무별 브리핑) 전체가 아니라, 요구사항 검증을 위한 최소 형태입니다 —
 * 직무 필터 없이 "오늘 분석된 뉴스 중 중요도 상위 N건"만 돌려줍니다. 나중에
 * 핵심기능3을 만들 때 이 엔드포인트에 직무별 필터(job 파라미터 등)가 추가될 예정입니다.
 */
@RestController
@RequestMapping("/api/briefings")
@Tag(name = "Briefing", description = "중요도순 뉴스 브리핑 조회 API (최소 형태, 직무 필터는 핵심기능3에서 추가 예정)")
public class BriefingController {

    private final BriefingMapper briefingMapper;
    private final BriefingProperties briefingProperties;

    public BriefingController(BriefingMapper briefingMapper, BriefingProperties briefingProperties) {
        this.briefingMapper = briefingMapper;
        this.briefingProperties = briefingProperties;
    }

    // [무엇을 받아서] 요청 파라미터 없음(개수는 application.yml의 briefing.top-n을 그대로 씀).
    // [무엇을 하고] BriefingMapper로 오늘 분석된 뉴스를 중요도 순으로 top-n건 가져오고,
    //              각 행의 콤마 문자열(industriesCsv)을 List&lt;String&gt;으로 쪼개서
    //              BriefingItem(응답용 DTO)으로 바꿉니다.
    // [무엇을 돌려주는지] 중요도 내림차순으로 정렬된 브리핑 목록(JSON 배열).
    // @GetMapping: 데이터를 "조회"만 하고 서버 상태를 바꾸지 않는 요청이라 GET을 씁니다
    // (수동 트리거가 POST인 것과 대비됩니다).
    @GetMapping
    @Operation(
            summary = "오늘의 중요도순 브리핑 조회",
            description = "오늘 분석된 뉴스 중 importance_score 내림차순으로 상위 N건(application.yml의 briefing.top-n, 기본 10건)을 반환합니다."
    )
    public List<BriefingItem> list() {
        List<BriefingRow> rows = briefingMapper.selectTopBriefings(briefingProperties.getTopN());
        return rows.stream().map(this::toItem).toList();
    }

    private BriefingItem toItem(BriefingRow row) {
        List<String> industries = row.getIndustriesCsv() == null
                ? List.of()
                : Arrays.asList(row.getIndustriesCsv().split(","));

        return new BriefingItem(
                row.getNewsId(),
                row.getTitle(),
                row.getUrl(),
                row.getPublishedAt(),
                row.getSummary(),
                row.getImportanceScore(),
                industries
        );
    }
}
