package com.darass.darass.auth.oauth.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.darass.darass.AcceptanceTest;
import com.darass.darass.auth.oauth.api.domain.KaKaoOAuthProvider;
import com.darass.darass.auth.oauth.api.domain.OAuthProviderFactory;
import com.darass.darass.auth.oauth.dto.AccessTokenResponse;
import com.darass.darass.auth.oauth.dto.TokenRequest;
import com.darass.darass.auth.oauth.infrastructure.JwtTokenProvider;
import com.darass.darass.exception.ExceptionWithMessageAndCode;
import com.darass.darass.exception.dto.ExceptionResponse;
import com.darass.darass.user.domain.SocialLoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Auth 인수 테스트")
public class AuthAcceptanceTest extends AcceptanceTest {

    @SpyBean(name = "jwtTokenProvider")
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private OAuthProviderFactory oAuthProviderFactory;

    @MockBean
    private KaKaoOAuthProvider kaKaoOAuthProvider;

    private String authorizationCode;

    private SocialLoginUser socialLoginUser;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        authorizationCode = "2FAF32IGO332IRFIJF3213";
        socialLoginUser = SocialLoginUser
            .builder()
            .nickName("우기")
            .oauthId("6752453")
            .oauthProvider(KaKaoOAuthProvider.NAME)
            .email("jujubebat@kakao.com")
            .build();
    }

    @DisplayName("oauth 인가 코드를 통해 회원가입 또는 로그인을 진행하고 refreshToken과 accessToken을 발급 받는다.")
    @Test
    public void login_success() throws Exception {
        //given
        given(oAuthProviderFactory.getOAuthProvider(any())).willReturn(kaKaoOAuthProvider);
        given(kaKaoOAuthProvider.requestSocialLoginUser(any())).willReturn(socialLoginUser);

        //when
        ResultActions resultActions = 토큰_발급_요청(KaKaoOAuthProvider.NAME, authorizationCode);

        //then
        토큰_발급됨(resultActions);
    }

    @DisplayName("유효하지 않은 인가 코드를 보낼 경우 refreshToken과 accessToken을 발급 받지 못한다.")
    @Test
    public void login_fail() throws Exception {
        //given
        given(oAuthProviderFactory.getOAuthProvider(any())).willReturn(kaKaoOAuthProvider);
        given(kaKaoOAuthProvider.requestSocialLoginUser(any()))
            .willThrow(ExceptionWithMessageAndCode.INVALID_OAUTH_AUTHORIZATION_CODE.getException());

        //when
        ResultActions resultActions = 토큰_발급_요청(KaKaoOAuthProvider.NAME, authorizationCode);

        //then
        토큰_발급_실패됨(resultActions);
    }

    @DisplayName("쿠키에 들어있는 refresh 토큰을 통해 accessToken과 refresh 토큰을 재발급 받는다.")
    @Test
    public void refreshToken() throws Exception {
        //given
        given(oAuthProviderFactory.getOAuthProvider(any())).willReturn(kaKaoOAuthProvider);
        given(kaKaoOAuthProvider.requestSocialLoginUser(any())).willReturn(socialLoginUser);

        ResultActions tokenRequestResultActions = 토큰_발급_요청(KaKaoOAuthProvider.NAME, authorizationCode);
        토큰_발급됨(tokenRequestResultActions);

        String jsonResponse = tokenRequestResultActions.andReturn().getResponse().getContentAsString();
        AccessTokenResponse accessTokenResponse = new ObjectMapper().readValue(jsonResponse, AccessTokenResponse.class);
        String accessToken = accessTokenResponse.getAccessToken();

        Cookie cookie = tokenRequestResultActions.andReturn().getResponse().getCookie("refreshToken");
        String refreshToken = cookie.getValue();

        // when
        Thread.sleep(1000);

        ResultActions tokenRefreshResultActions = 토큰_리프레시_요청(cookie);

        엑세스_토큰과_리프레쉬_토큰_재발급됨(accessToken, refreshToken, tokenRefreshResultActions);
    }

    @DisplayName("쿠키에 refresh 토큰이 들어있지 않다면, accessToken과 refresh 토큰을 재발급을 실패한다.")
    @Test
    public void refreshToken_fail() throws Exception {
        //given
        given(oAuthProviderFactory.getOAuthProvider(any())).willReturn(kaKaoOAuthProvider);
        given(kaKaoOAuthProvider.requestSocialLoginUser(any())).willReturn(socialLoginUser);

        Cookie cookie = new Cookie("name", "value");

        // when
        Thread.sleep(1000);

        ResultActions tokenRefreshResultActions = 토큰_리프레시_요청(cookie);

        엑세스_토큰과_리프레쉬_토큰_재발급_실패됨(tokenRefreshResultActions);
    }

    private void 엑세스_토큰과_리프레쉬_토큰_재발급_실패됨(ResultActions tokenRefreshResultActions)
        throws Exception {
        tokenRefreshResultActions.andExpect(status().is5xxServerError());

        토큰_인증_로그인_실패_rest_doc_작성(tokenRefreshResultActions);
    }

    private ResultActions 토큰_리프레시_요청(Cookie cookie) throws Exception {
        return this.mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/login/refresh")
            .cookie(cookie)
            .contentType(MediaType.APPLICATION_JSON));
    }

    private void 엑세스_토큰과_리프레쉬_토큰_재발급됨(String accessToken, String refreshToken, ResultActions tokenRefreshResultActions)
        throws Exception {
        tokenRefreshResultActions.andExpect(status().isOk());
        String jsonResponse = tokenRefreshResultActions.andReturn().getResponse().getContentAsString();
        AccessTokenResponse accessTokenResponse = new ObjectMapper().readValue(jsonResponse, AccessTokenResponse.class);
        String newAccessToken = accessTokenResponse.getAccessToken();

        Cookie cookie = tokenRefreshResultActions.andReturn().getResponse().getCookie("refreshToken");
        String newRefreshToken = cookie.getValue();

        assertThat(accessToken).isNotEqualTo(newAccessToken);
        assertThat(refreshToken).isNotEqualTo(newRefreshToken);

        엑세스_토큰과_리프레쉬_토큰_재발급됨_rest_doc_작성(tokenRefreshResultActions);
    }

    private void 엑세스_토큰과_리프레쉬_토큰_재발급됨_rest_doc_작성(ResultActions resultActions) throws Exception {
        resultActions.andDo(
            document("api/v1/login-refresh/post/success",
                responseFields(
                    fieldWithPath("accessToken").type(JsonFieldType.STRING).description("서버에서 발급해준 엑세스 토큰")
                )
            )
        );
    }

    private ResultActions 토큰_발급_요청(String oauthProviderName, String authorizationCode) throws Exception {
        return this.mockMvc.perform(RestDocumentationRequestBuilders.post("/api/v1/login/oauth")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(new TokenRequest(oauthProviderName, authorizationCode))));
    }

    private void 토큰_발급됨(ResultActions resultActions) throws Exception {
        resultActions.andExpect(status().isOk());
        String jsonResponse = resultActions.andReturn().getResponse().getContentAsString();
        AccessTokenResponse accessTokenResponse = new ObjectMapper().readValue(jsonResponse, AccessTokenResponse.class);
        String accessToken = accessTokenResponse.getAccessToken();

        Cookie cookie = resultActions.andReturn().getResponse().getCookie("refreshToken");

        assertThatCode(() -> jwtTokenProvider.validateRefreshToken(cookie.getValue())).doesNotThrowAnyException();
        assertThat(jwtTokenProvider.getAccessTokenPayload(accessToken)).isEqualTo(socialLoginUser.getId().toString());

        토큰_인증_로그인_rest_doc_작성(resultActions);
    }

    private void 토큰_인증_로그인_rest_doc_작성(ResultActions resultActions) throws Exception {
        resultActions.andDo(
            document("api/v1/auth-login/get/success",
                requestFields(
                    fieldWithPath("oauthProviderName").description("oauth 제공자 이름"),
                    fieldWithPath("authorizationCode").description("oauth 제공자가 발급해준 인가 코드")
                ),
                responseFields(
                    fieldWithPath("accessToken").type(JsonFieldType.STRING).description("서버에서 발급해준 엑세스 토큰")
                )
            )
        );
    }

    private void 토큰_발급_실패됨(ResultActions resultActions) throws Exception {
        resultActions.andExpect(status().isUnauthorized());

        String jsonResponse = resultActions.andReturn().getResponse().getContentAsString();
        ExceptionResponse exceptionResponse = new ObjectMapper().readValue(jsonResponse, ExceptionResponse.class);

        assertThat(exceptionResponse.getMessage())
            .isEqualTo(ExceptionWithMessageAndCode.INVALID_OAUTH_AUTHORIZATION_CODE.findMessage());
        assertThat(exceptionResponse.getCode())
            .isEqualTo(ExceptionWithMessageAndCode.INVALID_OAUTH_AUTHORIZATION_CODE.findCode());

        토큰_인증_로그인_실패_rest_doc_작성(resultActions);
    }

    private void 토큰_인증_로그인_실패_rest_doc_작성(ResultActions resultActions) throws Exception {
        resultActions.andDo(
            document("api/v1/auth-login/get/fail",
                responseFields(
                    fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                    fieldWithPath("code").type(JsonFieldType.NUMBER).description("에러 코드")
                ))
        );
    }

}
