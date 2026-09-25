package ue.edu.co.splitbill;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends ApiTestSupport {

    @Test
    void registerReturnsTokenAndNeverThePassword() throws Exception {
        this.mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"names": "Julian", "email": "julian.registro@test.com", "password": "clave-segura-123"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400))
                .andExpect(jsonPath("$.user.email").value("julian.registro@test.com"))
                .andExpect(jsonPath("$.user.registered").value(true))
                .andExpect(content().string(not(containsString("clave-segura-123"))))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void registerWithSameEmailInOtherCaseIsConflict() throws Exception {
        TestUser user = register("Diomar");
        this.mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"names": "Otro", "email": "%s", "password": "clave-segura-123"}
                        """.formatted(user.email().toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya existe una cuenta con ese email"));
    }

    @Test
    void registerWithInvalidFieldsReportsEachField() throws Exception {
        this.mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"names": "J", "email": "no-es-un-email", "password": "corta"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.names").exists())
                .andExpect(jsonPath("$.errors.email").value("El email no es válido"))
                .andExpect(jsonPath("$.errors.password").value("La contraseña debe tener entre 8 y 72 caracteres"));
    }

    @Test
    void loginWithCorrectPasswordReturnsToken() throws Exception {
        TestUser user = register("Juan");
        this.mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "clave-segura-123"}
                        """.formatted(user.email())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(user.id().toString()));
    }

    @Test
    void loginFailsWithTheSameMessageForWrongPasswordAndUnknownEmail() throws Exception {
        TestUser user = register("Sofia");
        this.mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "%s", "password": "otra-clave-999"}
                        """.formatted(user.email())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"));
        this.mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email": "nadie@test.com", "password": "clave-segura-123"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email o contraseña incorrectos"));
    }

    @Test
    void protectedRoutesRequireAValidToken() throws Exception {
        this.mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer token-inventado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanReadAndEditOwnProfile() throws Exception {
        TestUser user = register("Julian");
        doGet(user, "/api/users/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()));
        doPut(user, "/api/users/me", """
                {"names": "Julian Corredor", "phone": "3001234567"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").value("Julian Corredor"))
                .andExpect(jsonPath("$.phone").value("3001234567"));
    }

    @Test
    void swaggerIsPublic() throws Exception {
        this.mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SplitBill API"));
    }

    /** Render revisa este endpoint para saber que el despliegue arranco: no debe pedir token. */
    @Test
    void healthDoesNotNeedAToken() throws Exception {
        this.mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
