package com.jobnews.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * [전체 흐름에서의 위치] 프론트엔드가 "지금 로그인돼 있는지, 누구인지"를 확인하고
 * 로그아웃할 수 있게 해주는 API입니다. 로그인 자체(/oauth2/authorization/{provider})는
 * Spring Security가 자동으로 처리하므로 여기 없습니다 — 이 컨트롤러는 "로그인 이후"만
 * 다룹니다.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "로그인 상태 확인 및 로그아웃 API")
public class AuthController {

    // [무엇을 받아서] 요청 파라미터 없음. Authentication은 JwtAuthenticationFilter(또는
    //              방금 로그인 직후라면 OAuth2 인증 자체)가 SecurityContext에 채워둔
    //              값을 스프링이 자동으로 주입해줍니다.
    // [무엇을 하고] principal이 우리 AppPrincipal(방금 로그인 직후, GitHub/Google
    //              어느 쪽이든) 또는 User(JwtAuthenticationFilter가 채운 평소 상태)
    //              중 하나면 로그인된 것으로 봅니다. 둘 다 아니면(비로그인 사용자는
    //              principal이 "anonymousUser"라는 문자열입니다) 로그인 안 된 것입니다.
    // [무엇을 돌려주는지] 로그인 상태면 200 + {id, email, name, role}, 아니면 401(본문
    //              없음) — 이 프로젝트의 다른 "데이터 없음" 상태(204)와 의미가 달라서
    //              (여기는 "인증 안 됨"이 목적) 표준적인 401을 그대로 씁니다. 프론트는
    //              response.ok만 보면 됩니다.
    @GetMapping("/me")
    @Operation(
            summary = "현재 로그인 상태 조회",
            description = "로그인돼 있으면 200과 함께 {id, email, name, role}을 반환합니다. role로 프론트가 관리자 여부를 "
                    + "판단할 수 있습니다. 로그인 안 돼 있으면 401을 반환합니다(에러가 아니라 정상적인 비로그인 상태)."
    )
    public ResponseEntity<CurrentUserItem> me(Authentication authentication) {
        User user = extractAppUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new CurrentUserItem(user.getId(), user.getEmail(), user.getName(),
                user.getRole().name()));
    }

    // [무엇을 받아서] 요청/응답 객체(쿠키를 지우기 위해 필요).
    // [무엇을 하고] 로그인 쿠키를 만료(Max-Age=0)시켜서 브라우저가 더 이상 이 쿠키를
    //              보내지 않게 합니다. JWT는 서버에 상태가 없으므로 "서버가 즉시
    //              무효화"하는 개념은 없습니다 — 쿠키를 지우는 것으로 충분하다고
    //              판단했습니다(JwtProperties 주석 참고).
    // [무엇을 돌려주는지] 200(본문 없음).
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그인 쿠키를 지웁니다.")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtService.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }

    private User extractAppUser(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppPrincipal appPrincipal) {
            return appPrincipal.getAppUser();
        }
        if (principal instanceof User user) {
            return user;
        }
        return null;
    }
}
