package ue.edu.co.splitbill;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de gastos con el mismo escenario de la prueba manual de la entrega 1:
 * Julian, Diomar, Juan y Sofia; Almuerzo $60.000 y Gasolina $80.000 en partes iguales, y Mercado
 * $40.000 por porcentajes 40/30/20/10. Los montos van en centavos.
 */
class ExpenseControllerTest extends ApiTestSupport {

    private TestUser julian;
    private UUID groupId;
    private UUID diomar;
    private UUID juan;
    private UUID sofia;

    @BeforeEach
    void createGroupWithFourMembers() throws Exception {
        this.julian = register("Julian");
        this.groupId = createGroup(this.julian, "Paseo");
        this.diomar = addMemberByName(this.julian, this.groupId, "Diomar");
        this.juan = addMemberByName(this.julian, this.groupId, "Juan");
        this.sofia = addMemberByName(this.julian, this.groupId, "Sofia");
    }

    @Test
    void theScenarioOfDelivery1AddsUpTo180000() throws Exception {
        doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Almuerzo", 6_000_000L, "EQUAL",
                share(this.julian.id(), 1_500_000L), share(this.diomar, 1_500_000L),
                share(this.juan, 1_500_000L), share(this.sofia, 1_500_000L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shares", hasSize(4)));
        doPost(this.julian, expensesPath(), expenseJson(null, this.diomar, "Gasolina", 8_000_000L, "EQUAL",
                share(this.julian.id(), 2_000_000L), share(this.diomar, 2_000_000L),
                share(this.juan, 2_000_000L), share(this.sofia, 2_000_000L)))
                .andExpect(status().isCreated());
        doPost(this.julian, expensesPath(), expenseJson(null, this.juan, "Mercado", 4_000_000L, "PERCENTAGE",
                share(this.julian.id(), 1_600_000L), share(this.diomar, 1_200_000L),
                share(this.juan, 800_000L), share(this.sofia, 400_000L)))
                .andExpect(status().isCreated());

        String list = body(doGet(this.julian, expensesPath()).andExpect(status().isOk()));
        List<Number> amounts = JsonPath.read(list, "$[*].amountCents");
        assertThat(amounts).hasSize(3);
        assertThat(amounts.stream().mapToLong(Number::longValue).sum()).isEqualTo(18_000_000L);
    }

    @Test
    void sharesThatDoNotAddUpToTheAmountAreRejected() throws Exception {
        doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Almuerzo", 6_000_000L, "EXACT",
                share(this.julian.id(), 3_000_000L), share(this.diomar, 2_999_999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("suman 5999999 centavos")));
    }

    @Test
    void participantsMustBelongToTheGroup() throws Exception {
        TestUser outsider = register("Extrano");
        doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Almuerzo", 1_000_000L, "EXACT",
                share(this.julian.id(), 500_000L), share(outsider.id(), 500_000L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail",
                        containsString("deben ser integrantes del grupo")));
    }

    @Test
    void theSameParticipantCannotAppearTwice() throws Exception {
        doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Almuerzo", 1_000_000L, "EXACT",
                share(this.diomar, 500_000L), share(this.diomar, 500_000L)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendingTheSameExpenseDoesNotDuplicateIt() throws Exception {
        UUID expenseId = UUID.randomUUID();
        String json = expenseJson(expenseId, this.julian.id(), "Almuerzo", 1_000_000L, "EQUAL",
                share(this.julian.id(), 500_000L), share(this.diomar, 500_000L));

        doPost(this.julian, expensesPath(), json).andExpect(status().isCreated());
        doPost(this.julian, expensesPath(), json)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expenseId.toString()));
        doGet(this.julian, expensesPath()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void updateReplacesTheShares() throws Exception {
        String created = body(doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Almuerzo",
                900_000L, "EQUAL",
                share(this.julian.id(), 300_000L), share(this.diomar, 300_000L), share(this.juan, 300_000L)))
                .andExpect(status().isCreated()));
        String expenseId = JsonPath.read(created, "$.id");

        //Juan sale del gasto, Sofia entra, y a Julian le cambia su parte
        doPut(this.julian, expensesPath() + "/" + expenseId, expenseJson(null, this.julian.id(), "Almuerzo corregido",
                1_000_000L, "EXACT",
                share(this.julian.id(), 400_000L), share(this.diomar, 300_000L), share(this.sofia, 300_000L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Almuerzo corregido"))
                .andExpect(jsonPath("$.amountCents").value(1_000_000))
                .andExpect(jsonPath("$.shares", hasSize(3)))
                .andExpect(jsonPath("$.shares[?(@.userId == '" + this.juan + "')]", hasSize(0)))
                .andExpect(jsonPath("$.shares[?(@.userId == '" + this.sofia + "')].amountCents").value(300_000));
    }

    @Test
    void deletedExpensesDisappear() throws Exception {
        String created = body(doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Almuerzo",
                1_000_000L, "EQUAL", share(this.julian.id(), 500_000L), share(this.diomar, 500_000L)))
                .andExpect(status().isCreated()));
        String expenseId = JsonPath.read(created, "$.id");

        doDelete(this.julian, expensesPath() + "/" + expenseId).andExpect(status().isNoContent());
        doGet(this.julian, expensesPath()).andExpect(jsonPath("$", hasSize(0)));
        doGet(this.julian, expensesPath() + "/" + expenseId).andExpect(status().isNotFound());
    }

    @Test
    void outsiderCannotSeeOrCreateExpenses() throws Exception {
        TestUser outsider = register("Extrano");
        doGet(outsider, expensesPath()).andExpect(status().isNotFound());
        doPost(outsider, expensesPath(), expenseJson(null, outsider.id(), "Intruso", 100L, "EQUAL",
                share(outsider.id(), 100L)))
                .andExpect(status().isNotFound());
    }

    /** "Soy yo": Julian dice que el integrante "Juan" es el. Todo lo de Juan pasa a Julian. */
    @Test
    void claimingAMemberMovesTheirExpensesAndMergesTheirShares() throws Exception {
        //Juan pago un almuerzo que se repartio entre Julian y Juan
        String almuerzo = JsonPath.read(body(doPost(this.julian, expensesPath(), expenseJson(null, this.juan,
                "Almuerzo", 1_000_000L, "EQUAL", share(this.julian.id(), 500_000L), share(this.juan, 500_000L)))
                .andExpect(status().isCreated())), "$.id");
        //Diomar pago un taxi que se repartio entre Diomar y Juan
        String taxi = JsonPath.read(body(doPost(this.julian, expensesPath(), expenseJson(null, this.diomar,
                "Taxi", 300_000L, "EXACT", share(this.diomar, 100_000L), share(this.juan, 200_000L)))
                .andExpect(status().isCreated())), "$.id");

        doPost(this.julian, "/api/groups/" + this.groupId + "/members/" + this.juan + "/claim", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(this.julian.id().toString()));

        //el almuerzo ahora lo pago Julian, y su parte y la de Juan se sumaron en una sola
        doGet(this.julian, expensesPath() + "/" + almuerzo)
                .andExpect(jsonPath("$.payerId").value(this.julian.id().toString()))
                .andExpect(jsonPath("$.amountCents").value(1_000_000))
                .andExpect(jsonPath("$.shares", hasSize(1)))
                .andExpect(jsonPath("$.shares[0].amountCents").value(1_000_000));
        //en el taxi la parte de Juan paso a Julian
        doGet(this.julian, expensesPath() + "/" + taxi)
                .andExpect(jsonPath("$.shares[?(@.userId == '" + this.julian.id() + "')].amountCents").value(200_000));
        //Juan ya no aparece como integrante
        doGet(this.julian, "/api/groups/" + this.groupId + "/members")
                .andExpect(jsonPath("$[?(@.id == '" + this.juan + "')]", hasSize(0)));
    }

    @Test
    void aMemberWithTheirOwnAccountCannotBeClaimed() throws Exception {
        TestUser sofiaAccount = register("Sofia Reyes");
        doPost(this.julian, "/api/groups/" + this.groupId + "/members", """
                {"email": "%s"}
                """.formatted(sofiaAccount.email())).andExpect(status().isCreated());

        doPost(this.julian, "/api/groups/" + this.groupId + "/members/" + sofiaAccount.id() + "/claim", "")
                .andExpect(status().isConflict());
    }

    @Test
    void anOutsiderCannotClaimMembers() throws Exception {
        TestUser outsider = register("Extrano");
        doPost(outsider, "/api/groups/" + this.groupId + "/members/" + this.juan + "/claim", "")
                .andExpect(status().isNotFound());
    }

    /** Sincronizacion incremental: solo lo que cambio despues de la fecha, incluidos los borrados. */
    @Test
    void updatedSinceReturnsOnlyWhatChangedIncludingDeletions() throws Exception {
        String viejo = JsonPath.read(body(doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(),
                "Viejo", 100L, "EXACT", share(this.julian.id(), 100L))).andExpect(status().isCreated())), "$.id");
        String borrado = JsonPath.read(body(doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(),
                "Borrado", 100L, "EXACT", share(this.julian.id(), 100L))).andExpect(status().isCreated())), "$.id");
        Thread.sleep(20);
        java.time.Instant since = java.time.Instant.now();
        Thread.sleep(20);
        doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Nuevo", 100L, "EXACT",
                share(this.julian.id(), 100L))).andExpect(status().isCreated());
        doDelete(this.julian, expensesPath() + "/" + borrado).andExpect(status().isNoContent());

        doGet(this.julian, expensesPath() + "?updatedSince=" + since)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.description == 'Nuevo')].active").value(true))
                .andExpect(jsonPath("$[?(@.id == '" + borrado + "')].active").value(false))
                .andExpect(jsonPath("$[?(@.id == '" + viejo + "')]", hasSize(0)));
    }

    /** Rediseno: cada gasto tiene categoria; si la app no la manda, queda como OTHER. */
    @Test
    void theCategoryIsSavedAndDefaultsToOther() throws Exception {
        doPost(this.julian, expensesPath(), withCategory(expenseJson(null, this.julian.id(), "Almuerzo", 100L, "EXACT",
                share(this.julian.id(), 100L)), "FOOD"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("FOOD"));
        doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Sin categoria", 100L, "EXACT",
                share(this.julian.id(), 100L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("OTHER"));
    }

    @Test
    void anUnknownCategoryIsRejected() throws Exception {
        doPost(this.julian, expensesPath(), withCategory(expenseJson(null, this.julian.id(), "Almuerzo", 100L, "EXACT",
                share(this.julian.id(), 100L)), "JOYAS"))
                .andExpect(status().isBadRequest());
    }

    /**
     * "Marcar como pagado": Diomar le paga a Julian lo que le debe del almuerzo. Es un gasto PAYMENT
     * que paga Diomar y cuya unica parte es de Julian, con las mismas reglas de cualquier gasto.
     */
    @Test
    void aPaymentBetweenMembersIsAnExpenseWithASingleShare() throws Exception {
        doPost(this.julian, expensesPath(), expenseJson(null, this.julian.id(), "Almuerzo", 6_000_000L, "EQUAL",
                share(this.julian.id(), 1_500_000L), share(this.diomar, 1_500_000L),
                share(this.juan, 1_500_000L), share(this.sofia, 1_500_000L)))
                .andExpect(status().isCreated());
        doPost(this.julian, expensesPath(), withCategory(expenseJson(null, this.diomar, "Pago a Julian", 1_500_000L,
                "EXACT", share(this.julian.id(), 1_500_000L)), "PAYMENT"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("PAYMENT"))
                .andExpect(jsonPath("$.shares", hasSize(1)));
    }

    private String expensesPath() {
        return "/api/groups/" + this.groupId + "/expenses";
    }

    private static String share(UUID userId, long amountCents) {
        return """
                {"userId": "%s", "amountCents": %d}""".formatted(userId, amountCents);
    }

    private static String expenseJson(UUID id, UUID payerId, String description, long amountCents, String splitType,
                                      String... shares) {
        String idField = id == null ? "" : "\"id\": \"" + id + "\", ";
        return """
                {%s"payerId": "%s", "description": "%s", "amountCents": %d, "splitType": "%s", "shares": [%s]}
                """.formatted(idField, payerId, description, amountCents, splitType, String.join(", ", shares));
    }

    private static String withCategory(String expenseJson, String category) {
        return expenseJson.replaceFirst("\\{", "{\"category\": \"" + category + "\", ");
    }
}
