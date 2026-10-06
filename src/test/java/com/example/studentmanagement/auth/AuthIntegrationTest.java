package com.example.studentmanagement.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-secret-with-more-than-32-bytes-for-hs256",
        "app.seed.admin.username=test-admin",
        "app.seed.admin.email=test-admin@example.com",
        "app.seed.admin.password=AdminTest123",
        "app.seed.teacher.username=test-teacher",
        "app.seed.teacher.email=test-teacher@example.com",
        "app.seed.teacher.password=TeacherTest123"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final AtomicInteger USERS = new AtomicInteger();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerReturnsPasswordFreeStudentResponse() throws Exception {
        MvcResult result = register(newCredentials()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertNotNull(result.getResponse().getContentAsString());
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        Credentials credentials = newCredentials();
        register(credentials).andExpect(status().isCreated());
        register(credentials).andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"a","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void loginReturnsBearerTokens() throws Exception {
        Credentials credentials = newCredentials();
        register(credentials).andExpect(status().isCreated());
        login(credentials.username(), credentials.password())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void badPasswordReturnsGenericUnauthorizedResponse() throws Exception {
        login("test-admin", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void validAccessTokenCanCallProtectedEndpoint() throws Exception {
        Credentials credentials = newCredentials();
        register(credentials).andExpect(status().isCreated());
        String accessToken = accessToken(login(credentials.username(), credentials.password()).andReturn());
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(credentials.username()));
    }

    @Test
    void studentCannotListUsers() throws Exception {
        Credentials credentials = newCredentials();
        register(credentials).andExpect(status().isCreated());
        String accessToken = accessToken(login(credentials.username(), credentials.password()).andReturn());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminCanListUsers() throws Exception {
        String accessToken = accessToken(login("test-admin", "AdminTest123").andReturn());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void refreshRotatesRefreshAndAccessTokens() throws Exception {
        Credentials credentials = newCredentials();
        register(credentials).andExpect(status().isCreated());
        String oldRefreshToken = refreshToken(login(credentials.username(), credentials.password()).andReturn());
        MvcResult rotated = refresh(oldRefreshToken).andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        org.junit.jupiter.api.Assertions.assertNotEquals(oldRefreshToken, refreshToken(rotated));
        refresh(oldRefreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenCannotAuthenticateAsBearer() throws Exception {
        String refreshToken = refreshToken(login("test-admin", "AdminTest123").andReturn());
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutThenRefreshReturnsUnauthorized() throws Exception {
        String refreshToken = refreshToken(login("test-admin", "AdminTest123").andReturn());
        mockMvc.perform(post("/api/auth/logout").contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isOk());
        refresh(refreshToken).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutOfUnknownTokenIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/auth/logout").contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"unknown-token\"}"))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions register(Credentials credentials)
            throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"%s"}
                        """.formatted(credentials.username(), credentials.email(), credentials.password())));
    }

    private org.springframework.test.web.servlet.ResultActions login(String username, String password)
            throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(APPLICATION_JSON)
                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)));
    }

    private String accessToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String refreshToken(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.refreshToken");
    }

    private Credentials newCredentials() {
        int id = USERS.incrementAndGet();
        return new Credentials("student" + id, "student" + id + "@example.com", "StudentPass" + id + "8");
    }

    private record Credentials(String username, String email, String password) {
    }
}