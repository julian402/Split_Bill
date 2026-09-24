package ue.edu.co.splitbill;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupControllerTest extends ApiTestSupport {

    @Test
    void creatorIsOwnerAndMember() throws Exception {
        TestUser julian = register("Julian");
        UUID groupId = createGroup(julian, "Paseo");

        doGet(julian, "/api/groups/" + groupId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(julian.id().toString()))
                .andExpect(jsonPath("$.currency").value("COP"));
        doGet(julian, "/api/groups")
                .andExpect(jsonPath("$[*].id", hasItem(groupId.toString())));
        doGet(julian, "/api/groups/" + groupId + "/members")
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(julian.id().toString()));
    }

    @Test
    void outsiderCannotSeeTheGroup() throws Exception {
        TestUser julian = register("Julian");
        TestUser outsider = register("Extrano");
        UUID groupId = createGroup(julian, "Paseo");

        doGet(outsider, "/api/groups/" + groupId).andExpect(status().isNotFound());
        doGet(outsider, "/api/groups/" + groupId + "/members").andExpect(status().isNotFound());
        doGet(outsider, "/api/groups")
                .andExpect(jsonPath("$[*].id", not(hasItem(groupId.toString()))));
    }

    @Test
    void membersCanBeAddedByNameOrByEmail() throws Exception {
        TestUser julian = register("Julian");
        TestUser diomar = register("Diomar");
        UUID groupId = createGroup(julian, "Paseo");

        addMemberByName(julian, groupId, "Juan");
        addMemberByName(julian, groupId, "Sofia");
        doPost(julian, "/api/groups/" + groupId + "/members", """
                {"email": "%s"}
                """.formatted(diomar.email()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registered").value(true));

        doGet(julian, "/api/groups/" + groupId + "/members")
                .andExpect(jsonPath("$", hasSize(4)));
        //Diomar ya puede ver el grupo desde su propia cuenta
        doGet(diomar, "/api/groups")
                .andExpect(jsonPath("$[*].id", hasItem(groupId.toString())));
    }

    @Test
    void addingAMemberWithoutNameOrEmailIsRejected() throws Exception {
        TestUser julian = register("Julian");
        UUID groupId = createGroup(julian, "Paseo");
        doPost(julian, "/api/groups/" + groupId + "/members", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Indique el nombre o el email del integrante"));
    }

    @Test
    void onlyTheOwnerCanEditOrDelete() throws Exception {
        TestUser julian = register("Julian");
        TestUser diomar = register("Diomar");
        UUID groupId = createGroup(julian, "Paseo");
        doPost(julian, "/api/groups/" + groupId + "/members", """
                {"email": "%s"}
                """.formatted(diomar.email())).andExpect(status().isCreated());

        doPut(diomar, "/api/groups/" + groupId, """
                {"name": "Cambiado"}
                """).andExpect(status().isForbidden());
        doDelete(diomar, "/api/groups/" + groupId).andExpect(status().isForbidden());

        doPut(julian, "/api/groups/" + groupId, """
                {"name": "Paseo a Villa de Leyva", "currency": "USD"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Paseo a Villa de Leyva"))
                .andExpect(jsonPath("$.currency").value("USD"));
        doDelete(julian, "/api/groups/" + groupId).andExpect(status().isNoContent());
        doGet(julian, "/api/groups/" + groupId).andExpect(status().isNotFound());
    }

    @Test
    void ownerRemovesMembersButNotThemselves() throws Exception {
        TestUser julian = register("Julian");
        UUID groupId = createGroup(julian, "Paseo");
        UUID juan = addMemberByName(julian, groupId, "Juan");

        doDelete(julian, "/api/groups/" + groupId + "/members/" + julian.id())
                .andExpect(status().isBadRequest());
        doDelete(julian, "/api/groups/" + groupId + "/members/" + juan)
                .andExpect(status().isNoContent());
        doGet(julian, "/api/groups/" + groupId + "/members")
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void repeatingACreateWithTheSameIdDoesNotDuplicate() throws Exception {
        TestUser julian = register("Julian");
        UUID groupId = UUID.randomUUID();
        String json = """
                {"id": "%s", "name": "Paseo"}
                """.formatted(groupId);

        doPost(julian, "/api/groups", json).andExpect(status().isCreated());
        doPost(julian, "/api/groups", json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(groupId.toString()));
        doGet(julian, "/api/groups").andExpect(jsonPath("$", hasSize(1)));

        //otra persona no puede apropiarse de ese id
        TestUser outsider = register("Extrano");
        doPost(outsider, "/api/groups", json).andExpect(status().isConflict());
    }

    @Test
    void invalidCurrencyIsRejected() throws Exception {
        TestUser julian = register("Julian");
        doPost(julian, "/api/groups", """
                {"name": "Paseo", "currency": "pesos"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.currency").exists());
    }
}
