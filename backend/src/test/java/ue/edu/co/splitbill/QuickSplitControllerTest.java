package ue.edu.co.splitbill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cuentas rapidas guardadas: la cena de $100.000 con 10 % de propina entre 4 ($27.500 cada uno).
 * Los montos van en centavos.
 */
class QuickSplitControllerTest extends ApiTestSupport {

    private static final String PATH = "/api/quick-splits";

    private TestUser julian;

    @BeforeEach
    void registerUser() throws Exception {
        this.julian = register("Julian");
    }

    @Test
    void aSavedQuickSplitIsListedWithItsSharesInOrder() throws Exception {
        doPost(this.julian, PATH, dinnerJson(null, 2_750_000L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalCents").value(11_000_000))
                .andExpect(jsonPath("$.tipPercent").value(10));

        doGet(this.julian, PATH)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Cena"))
                .andExpect(jsonPath("$[0].shares", hasSize(4)))
                .andExpect(jsonPath("$[0].shares[0].name").value("Ana"))
                .andExpect(jsonPath("$[0].shares[3].name").value("Persona 4"));
    }

    @Test
    void sharesThatDoNotAddUpToTheTotalAreRejected() throws Exception {
        doPost(this.julian, PATH, dinnerJson(null, 2_749_999L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("la cuenta es de 11000000 centavos")));
    }

    @Test
    void resendingTheSameQuickSplitDoesNotDuplicateIt() throws Exception {
        UUID id = UUID.randomUUID();
        doPost(this.julian, PATH, dinnerJson(id, 2_750_000L)).andExpect(status().isCreated());
        doPost(this.julian, PATH, dinnerJson(id, 2_750_000L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()));
        doGet(this.julian, PATH).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void eachPersonOnlySeesAndDeletesTheirOwn() throws Exception {
        UUID id = UUID.randomUUID();
        doPost(this.julian, PATH, dinnerJson(id, 2_750_000L)).andExpect(status().isCreated());
        TestUser sofia = register("Sofia");

        doGet(sofia, PATH).andExpect(jsonPath("$", hasSize(0)));
        doDelete(sofia, PATH + "/" + id).andExpect(status().isNotFound());

        doDelete(this.julian, PATH + "/" + id).andExpect(status().isNoContent());
        doGet(this.julian, PATH).andExpect(jsonPath("$", hasSize(0)));
    }

    /** $100.000 + 10 % entre Ana y tres personas sin nombre; la ultima parte cambia segun la prueba. */
    private static String dinnerJson(UUID id, long lastShareCents) {
        String idField = id == null ? "" : "\"id\": \"" + id + "\", ";
        return """
                {%s"description": "Cena", "subtotalCents": 10000000, "tipPercent": 10,
                 "totalCents": 11000000, "splitType": "EQUAL",
                 "shares": [{"name": "Ana", "amountCents": 2750000}, {"name": "Persona 2", "amountCents": 2750000},
                            {"name": "Persona 3", "amountCents": 2750000}, {"name": "Persona 4", "amountCents": %d}]}
                """.formatted(idField, lastShareCents);
    }
}
