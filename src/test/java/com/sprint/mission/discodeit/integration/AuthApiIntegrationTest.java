package com.sprint.mission.discodeit.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.service.UserService;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserService userService;

  @Test
  @DisplayName("로그인 API 통합 테스트 - 성공")
  void login_Success() throws Exception {
    UserCreateRequest userRequest = new UserCreateRequest(
        "loginuser",
        "login@example.com",
        "Password1!"
    );
    userService.create(userRequest, Optional.empty());

    performLogin("loginuser", "Password1!")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.username", is("loginuser")))
        .andExpect(jsonPath("$.email", is("login@example.com")));
  }

  @Test
  @DisplayName("로그인 API 통합 테스트 - 실패, 존재하지 않는 사용자")
  void login_Failure_UserNotFound() throws Exception {
    performLogin("nonexistentuser", "Password1!")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status", is(401)));
  }

  @Test
  @DisplayName("로그인 API 통합 테스트 - 실패, 잘못된 비밀번호")
  void login_Failure_InvalidCredentials() throws Exception {
    UserCreateRequest userRequest = new UserCreateRequest(
        "loginuser2",
        "login2@example.com",
        "Password1!"
    );
    userService.create(userRequest, Optional.empty());

    performLogin("loginuser2", "WrongPassword1!")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status", is(401)));
  }

  @Test
  @DisplayName("로그인 API 통합 테스트 - 실패, 빈 인증 값")
  void login_Failure_InvalidRequest() throws Exception {
    performLogin("", "")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status", is(401)));
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 API 통합 테스트 - 성공")
  void me_Success() throws Exception {
    UserCreateRequest userRequest = new UserCreateRequest(
        "meuser",
        "me@example.com",
        "Password1!"
    );
    userService.create(userRequest, Optional.empty());

    MvcResult loginResult = performLogin("meuser", "Password1!")
        .andExpect(status().isOk())
        .andReturn();
    MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
    assertThat(session).isNotNull();

    mockMvc.perform(get("/api/auth/me")
            .session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.username", is("meuser")))
        .andExpect(jsonPath("$.email", is("me@example.com")));
  }

  @Test
  @DisplayName("현재 사용자 정보 조회 API 통합 테스트 - 인증되지 않은 요청")
  void me_Failure_Unauthenticated() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Remember-me 체크 로그인 시 remember-me 쿠키가 발급된다")
  void login_WithRememberMe_IssuesRememberMeCookie() throws Exception {
    UserCreateRequest userRequest = new UserCreateRequest(
        "rememberuser",
        "remember@example.com",
        "Password1!"
    );
    userService.create(userRequest, Optional.empty());

    MvcResult loginResult = performLogin("rememberuser", "Password1!", true)
        .andExpect(status().isOk())
        .andReturn();

    Cookie rememberMeCookie = loginResult.getResponse().getCookie("remember-me");
    assertThat(rememberMeCookie).isNotNull();
    assertThat(rememberMeCookie.getMaxAge()).isGreaterThan(0);
  }

  @Test
  @DisplayName("JSESSIONID 없이 remember-me 쿠키만으로 현재 사용자 정보를 조회할 수 있다")
  void me_WithRememberMeCookieAndNoSession_Success() throws Exception {
    UserCreateRequest userRequest = new UserCreateRequest(
        "remembermeuser",
        "remember-me@example.com",
        "Password1!"
    );
    userService.create(userRequest, Optional.empty());

    MvcResult loginResult = performLogin("remembermeuser", "Password1!", true)
        .andExpect(status().isOk())
        .andReturn();
    Cookie rememberMeCookie = loginResult.getResponse().getCookie("remember-me");
    assertThat(rememberMeCookie).isNotNull();

    mockMvc.perform(get("/api/auth/me")
            .cookie(rememberMeCookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", notNullValue()))
        .andExpect(jsonPath("$.username", is("remembermeuser")))
        .andExpect(jsonPath("$.email", is("remember-me@example.com")))
        .andExpect(jsonPath("$.online", is(true)));
  }

  @Test
  @DisplayName("로그아웃 API 통합 테스트 - 성공")
  void logout_Success() throws Exception {
    UserCreateRequest userRequest = new UserCreateRequest(
        "logoutuser",
        "logout@example.com",
        "Password1!"
    );
    userService.create(userRequest, Optional.empty());

    MvcResult loginResult = performLogin("logoutuser", "Password1!")
        .andExpect(status().isOk())
        .andReturn();
    MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
    assertThat(session).isNotNull();

    Cookie csrfCookie = getCsrfCookie(session);

    mockMvc.perform(post("/api/auth/logout")
            .session(session)
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue()))
        .andExpect(status().isNoContent());

    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  @DisplayName("Remember-me 로그인 후 로그아웃하면 remember-me 쿠키가 만료된다")
  void logout_WithRememberMe_ExpiresRememberMeCookie() throws Exception {
    UserCreateRequest userRequest = new UserCreateRequest(
        "rememberlogoutuser",
        "remember-logout@example.com",
        "Password1!"
    );
    userService.create(userRequest, Optional.empty());

    MvcResult loginResult = performLogin("rememberlogoutuser", "Password1!", true)
        .andExpect(status().isOk())
        .andReturn();
    MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
    Cookie rememberMeCookie = loginResult.getResponse().getCookie("remember-me");
    assertThat(session).isNotNull();
    assertThat(rememberMeCookie).isNotNull();

    Cookie csrfCookie = getCsrfCookie(session);

    MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
            .session(session)
            .cookie(csrfCookie, rememberMeCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue()))
        .andExpect(status().isNoContent())
        .andReturn();

    Cookie expiredRememberMeCookie = logoutResult.getResponse().getCookie("remember-me");
    assertThat(expiredRememberMeCookie).isNotNull();
    assertThat(expiredRememberMeCookie.getMaxAge()).isZero();
  }

  private ResultActions performLogin(String username, String password) throws Exception {
    return performLogin(username, password, false);
  }

  private ResultActions performLogin(String username, String password, boolean rememberMe)
      throws Exception {
    Cookie csrfCookie = getCsrfCookie();

    return mockMvc.perform(multipart("/api/auth/login")
        .cookie(csrfCookie)
        .header("X-XSRF-TOKEN", csrfCookie.getValue())
        .param("username", username)
        .param("password", password)
        .param("remember-me", Boolean.toString(rememberMe)));
  }

  private Cookie getCsrfCookie() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/auth/csrf-token"))
        .andExpect(status().isNonAuthoritativeInformation())
        .andReturn();

    Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrfCookie).isNotNull();
    return csrfCookie;
  }

  private Cookie getCsrfCookie(MockHttpSession session) throws Exception {
    MvcResult result = mockMvc.perform(get("/api/auth/csrf-token")
            .session(session))
        .andExpect(status().isNonAuthoritativeInformation())
        .andReturn();

    Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrfCookie).isNotNull();
    return csrfCookie;
  }
}
