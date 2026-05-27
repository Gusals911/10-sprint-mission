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
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
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

    MvcResult loginResult = performLogin("loginuser", "Password1!")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.userDto.id", notNullValue()))
        .andExpect(jsonPath("$.userDto.username", is("loginuser")))
        .andExpect(jsonPath("$.userDto.email", is("login@example.com")))
        .andReturn();

    assertThat(loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
        .anySatisfy(cookie -> assertThat(cookie).contains("REFRESH_TOKEN="));
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
  @DisplayName("리프레시 토큰으로 액세스 토큰을 재발급한다")
  void refresh_Success() throws Exception {
    UserCreateRequest userRequest = new UserCreateRequest(
        "refreshuser",
        "refresh@example.com",
        "Password1!"
    );
    userService.create(userRequest, Optional.empty());

    MvcResult loginResult = performLogin("refreshuser", "Password1!")
        .andExpect(status().isOk())
        .andReturn();
    Cookie refreshTokenCookie = loginResult.getResponse().getCookie("REFRESH_TOKEN");
    assertThat(refreshTokenCookie).isNotNull();
    Cookie csrfCookie = getCsrfCookie();

    MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
            .cookie(refreshTokenCookie, csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken", notNullValue()))
        .andExpect(jsonPath("$.userDto.id", notNullValue()))
        .andExpect(jsonPath("$.userDto.username", is("refreshuser")))
        .andExpect(jsonPath("$.userDto.email", is("refresh@example.com")))
        .andReturn();

    assertThat(refreshResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
        .anySatisfy(cookie -> assertThat(cookie).contains("REFRESH_TOKEN="));
  }

  @Test
  @DisplayName("리프레시 토큰이 없으면 401을 반환한다")
  void refresh_Failure_MissingRefreshToken() throws Exception {
    Cookie csrfCookie = getCsrfCookie();

    mockMvc.perform(post("/api/auth/refresh")
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status", is(401)));
  }

  @Test
  @DisplayName("로그아웃 API 통합 테스트 - 성공")
  void logout_Success() throws Exception {
    Cookie csrfCookie = getCsrfCookie();

    mockMvc.perform(post("/api/auth/logout")
            .cookie(csrfCookie)
            .header("X-XSRF-TOKEN", csrfCookie.getValue()))
        .andExpect(status().isNoContent());
  }

  private ResultActions performLogin(String username, String password) throws Exception {
    Cookie csrfCookie = getCsrfCookie();

    return mockMvc.perform(multipart("/api/auth/login")
        .cookie(csrfCookie)
        .header("X-XSRF-TOKEN", csrfCookie.getValue())
        .param("username", username)
        .param("password", password));
  }

  private Cookie getCsrfCookie() throws Exception {
    MvcResult result = mockMvc.perform(get("/api/auth/csrf-token"))
        .andExpect(status().isNonAuthoritativeInformation())
        .andReturn();

    Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
    assertThat(csrfCookie).isNotNull();
    return csrfCookie;
  }
}
