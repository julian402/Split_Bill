package ue.edu.co.splitbill;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Chat de un grupo compartido entre Julian y Diomar, cada uno con su cuenta. */
class MessageControllerTest extends ApiTestSupport {

    private TestUser julian;
    private TestUser diomar;
    private UUID groupId;

    @BeforeEach
    void createSharedGroup() throws Exception {
        this.julian = register("Julian");
        this.diomar = register("Diomar");
        this.groupId = createGroup(this.julian, "Paseo");
        doPost(this.julian, "/api/groups/" + this.groupId + "/members", """
                {"email": "%s"}
                """.formatted(this.diomar.email())).andExpect(status().isCreated());
    }

    @Test
    void whatOneMemberWritesTheOtherReadsWithTheSenderName() throws Exception {
        doPost(this.julian, messagesPath(), message(null, "¿Quién lleva el carbón?"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderNames").value("Julian"));
        doPost(this.diomar, messagesPath(), message(null, "Yo lo llevo"))
                .andExpect(status().isCreated());

        doGet(this.diomar, messagesPath())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].text").value("¿Quién lleva el carbón?"))
                .andExpect(jsonPath("$[0].senderId").value(this.julian.id().toString()))
                .andExpect(jsonPath("$[1].senderNames").value("Diomar"));
    }

    @Test
    void sinceBringsOnlyWhatArrivedLater() throws Exception {
        String first = body(doPost(this.julian, messagesPath(), message(null, "Hola"))
                .andExpect(status().isCreated()));
        Instant createdAt = Instant.parse(JsonPath.read(first, "$.createdAt"));
        doPost(this.diomar, messagesPath(), message(null, "Qué más")).andExpect(status().isCreated());

        doGet(this.julian, messagesPath() + "?since=" + createdAt)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].text").value("Qué más"));
    }

    @Test
    void resendingTheSameMessageDoesNotDuplicateIt() throws Exception {
        UUID id = UUID.randomUUID();
        doPost(this.julian, messagesPath(), message(id, "Hola")).andExpect(status().isCreated());
        doPost(this.julian, messagesPath(), message(id, "Hola"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
        doGet(this.julian, messagesPath()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void anEmptyMessageIsRejected() throws Exception {
        doPost(this.julian, messagesPath(), message(null, "   ")).andExpect(status().isBadRequest());
    }

    @Test
    void someoneOutsideTheGroupCanNeitherReadNorWrite() throws Exception {
        TestUser outsider = register("Extrano");
        doGet(outsider, messagesPath()).andExpect(status().isNotFound());
        doPost(outsider, messagesPath(), message(null, "Hola")).andExpect(status().isNotFound());
    }

    private String messagesPath() {
        return "/api/groups/" + this.groupId + "/messages";
    }

    private static String message(UUID id, String text) {
        String idField = id == null ? "" : "\"id\": \"" + id + "\", ";
        return "{" + idField + "\"text\": \"" + text + "\"}";
    }
}
