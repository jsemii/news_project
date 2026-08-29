package com.jobnews.scrap;

import com.jobnews.auth.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [전체 흐름에서의 위치] 로그인한 사용자가 관심 뉴스를 저장(스크랩)/취소/조회하는
 * REST API입니다. 이 컨트롤러의 모든 엔드포인트는 config.SecurityConfig에서
 * "/api/scraps/**"를 인증 필요(authenticated)로 막아뒀기 때문에, 비로그인 요청은
 * 여기 도달하기도 전에 403으로 걸러집니다 — 그래서 이 클래스 안에서는 로그인
 * 여부를 따로 검사하지 않고, Authentication의 principal이 항상 실제 User라고
 * 믿고 바로 캐스팅합니다(JwtAuthenticationFilter가 평소 요청에는 항상 순수 User
 * 객체를 principal로 채워 넣으므로 — AuthController처럼 AppPrincipal 래퍼까지
 * 신경 쓸 필요가 없습니다. 그건 OAuth2 로그인 콜백 순간에만 잠깐 나타나는
 * 타입입니다).
 */
@RestController
@RequestMapping("/api/scraps")
@Tag(name = "Scrap", description = "뉴스 스크랩(북마크) 추가/취소/조회 API (로그인 필요)")
public class ScrapController {

    private final ScrapMapper scrapMapper;

    public ScrapController(ScrapMapper scrapMapper) {
        this.scrapMapper = scrapMapper;
    }

    // [무엇을 받아서] 스크랩할 뉴스 id(newsId)와, 로그인한 사용자 정보(Authentication —
    //              스프링이 SecurityContext에서 자동으로 꺼내 주입해줍니다).
    // [무엇을 하고] (사용자, 뉴스) 조합으로 스크랩 행을 새로 만듭니다. 이미 스크랩한
    //              뉴스를 다시 스크랩하면(UNIQUE 제약 위반) DuplicateKeyException이
    //              발생하는데, 이건 에러가 아니라 "이미 스크랩돼 있음"이라는 정상
    //              상태로 취급합니다(같은 버튼을 두 번 눌러도 안전하게 동작하기
    //              위함 — NewsStructuringService가 동시 저장 충돌을 처리하는 것과
    //              같은 패턴, docs/troubleshooting.md 9번 항목 참고). insert
    //              성공/중복 두 경우 모두, 응답에 담을 최종 스크랩 정보는 항상
    //              다시 SELECT해서 만듭니다 — insertScrap의 useGeneratedKeys는 id만
    //              돌려받을 뿐 createdAt(DB의 DEFAULT now()가 채움)은 안 채워주므로,
    //              INSERT 직후 바로 응답을 만들면 createdAt이 비어있게 됩니다.
    // [무엇을 돌려주는지] 200 + 생성(또는 기존) 스크랩 항목.
    @PostMapping
    @Operation(
            summary = "뉴스 스크랩 추가",
            description = "로그인한 사용자가 특정 뉴스를 스크랩합니다. 이미 스크랩한 뉴스를 다시 요청해도 에러 없이 기존 "
                    + "스크랩을 그대로 반환합니다."
    )
    public ResponseEntity<ScrapItem> add(
            @Parameter(description = "스크랩할 뉴스 id") @RequestParam Long newsId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        try {
            scrapMapper.insertScrap(new Scrap(user.getId(), newsId));
        } catch (DuplicateKeyException e) {
            // 이미 스크랩된 뉴스 — 아래에서 기존 행을 그대로 조회해서 반환합니다.
        }
        Scrap scrap = scrapMapper.selectByUserAndNews(user.getId(), newsId);
        return ResponseEntity.ok(toItem(scrap));
    }

    // [무엇을 받아서] 스크랩을 취소할 뉴스 id(경로 변수)와 로그인한 사용자 정보.
    // [무엇을 하고] (사용자, 뉴스) 조합의 스크랩 행을 삭제합니다. 애초에 스크랩한
    //              적이 없어도(0행 삭제) 에러 없이 그냥 끝납니다 — DELETE는 원래
    //              멱등한 동작이라는 REST 관례를 그대로 따릅니다.
    // [무엇을 돌려주는지] 200(본문 없음).
    @DeleteMapping("/{newsId}")
    @Operation(
            summary = "뉴스 스크랩 취소",
            description = "로그인한 사용자의 특정 뉴스 스크랩을 취소합니다. 스크랩한 적이 없어도 에러 없이 200을 반환합니다."
    )
    public ResponseEntity<Void> remove(
            @Parameter(description = "스크랩 취소할 뉴스 id") @PathVariable Long newsId,
            Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        scrapMapper.deleteScrap(user.getId(), newsId);
        return ResponseEntity.ok().build();
    }

    // [무엇을 받아서] 로그인한 사용자 정보.
    // [무엇을 하고] 그 사용자가 스크랩한 뉴스 전체를 최신순으로 조회합니다.
    // [무엇을 돌려주는지] 스크랩 목록(JSON 배열). 하나도 없으면 빈 배열(에러 아님).
    @GetMapping
    @Operation(
            summary = "내 스크랩 목록 조회",
            description = "로그인한 사용자가 스크랩한 뉴스 id 목록을 최신순으로 반환합니다."
    )
    public List<ScrapItem> list(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return scrapMapper.selectByUser(user.getId());
    }

    private ScrapItem toItem(Scrap scrap) {
        return new ScrapItem(scrap.getId(), scrap.getNewsId(), scrap.getCreatedAt());
    }
}
