package ue.edu.co.splitbill;

import com.jayway.jsonpath.JsonPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de la API: levanta la aplicacion completa contra un PostgreSQL real en Docker
 * (Testcontainers) y ofrece atajos para registrar usuarios y hacer peticiones con su token.
 *
 * Cada prueba registra usuarios con emails unicos, asi que las pruebas no dependen unas de otras
 * aunque compartan la misma base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    /** Usuario registrado para una prueba: su id, su email y su token. */
    protected record TestUser(UUID id, String email, String token) {
    }

    protected TestUser register(String names) throws Exception {
        String email = names.toLowerCase().replace(' ', '.') + "." + UUID.randomUUID() + "@test.com";
        String body = """
                {"names": "%s", "email": "%s", "password": "clave-segura-123"}
                """.formatted(names, email);
        String response = this.mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(response, "$.user.id"));
        return new TestUser(id, email, JsonPath.read(response, "$.token"));
    }

    protected ResultActions doGet(TestUser user, String path) throws Exception {
        return this.mockMvc.perform(withToken(get(path), user));
    }

    protected ResultActions doPost(TestUser user, String path, String json) throws Exception {
        return this.mockMvc.perform(withToken(post(path), user).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions doPut(TestUser user, String path, String json) throws Exception {
        return this.mockMvc.perform(withToken(put(path), user).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions doDelete(TestUser user, String path) throws Exception {
        return this.mockMvc.perform(withToken(delete(path), user));
    }

    /** Crea un grupo y devuelve su id. */
    protected UUID createGroup(TestUser owner, String name) throws Exception {
        String response = doPost(owner, "/api/groups", """
                {"name": "%s"}
                """.formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    /** Agrega un integrante sin cuenta y devuelve su id. */
    protected UUID addMemberByName(TestUser user, UUID groupId, String names) throws Exception {
        String response = doPost(user, "/api/groups/" + groupId + "/members", """
                {"names": "%s"}
                """.formatted(names))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    protected static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, TestUser user) {
        return request.header("Authorization", "Bearer " + user.token());
    }
}
