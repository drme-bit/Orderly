package dev.drme.users.config;

import dev.drme.users.domain.UserStatus;
import dev.drme.users.dto.LocationDto;
import dev.drme.users.dto.UserRequestDto;
import dev.drme.users.dto.UserResponseDto;
import dev.drme.users.facade.UserFacade;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, SecurityConfigTest.InternalProbeConfig.class})
@TestPropertySource(properties = "orderly.internal.token=test-only-internal-token-with-32-bytes")
class SecurityConfigTest {

    private static final String TOKEN = "test-only-internal-token-with-32-bytes";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserFacade userFacade;

    @Test
    void internalRequestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/internal/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/internal/probe"));
    }

    @Test
    void internalRequestWithWrongTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/internal/probe").header(InternalTokenFilter.HEADER, "wrong-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void internalRequestWithValidTokenIsAllowedAndSkipsCsrf() throws Exception {
        mockMvc.perform(post("/internal/probe").header(InternalTokenFilter.HEADER, TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void publicMutationWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void publicMutationWithCsrfCookieAndHeaderIsAllowed() throws Exception {
        Cookie csrfCookie = obtainCsrfCookie();

        when(userFacade.createUser(any(UserRequestDto.class))).thenReturn(
                new UserResponseDto(
                        UUID.randomUUID(),
                        "John",
                        null,
                        "Doe",
                        "john@example.com",
                        new LocationDto("ES", "Madrid"),
                        UserStatus.ACTIVE
                )
        );

        mockMvc.perform(post("/api/v1/users")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    private Cookie obtainCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users")).andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie, "CSRF cookie was not set");
        return cookie;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class InternalProbeConfig {

        @Bean
        InternalProbeController internalProbeController() {
            return new InternalProbeController();
        }
    }

    @RestController
    static class InternalProbeController {

        @GetMapping("/internal/probe")
        String get() {
            return "ok";
        }

        @PostMapping("/internal/probe")
        String post() {
            return "ok";
        }
    }
}
