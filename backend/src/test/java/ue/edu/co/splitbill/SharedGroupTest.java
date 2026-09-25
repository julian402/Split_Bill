package ue.edu.co.splitbill;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Un grupo compartido entre dos cuentas, cada una en su celular: Julian crea el grupo y Diomar entra
 * con su propia cuenta, invitado por email o vinculado despues de haber sido agregado por nombre.
 */
class SharedGroupTest extends ApiTestSupport {

    private TestUser julian;
    private TestUser diomar;
    private UUID groupId;

    @BeforeEach
    void createGroup() throws Exception {
        this.julian = register("Julian");
        this.diomar = register("Diomar");
        this.groupId = createGroup(this.julian, "Paseo");
    }

    @Test
    void anInvitedAccountSeesTheGroupAndItsExpenses() throws Exception {
        doPost(this.julian, membersPath(), """
                {"email": "%s"}
                """.formatted(this.diomar.email()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(this.diomar.id().toString()))
                .andExpect(jsonPath("$.registered").value(true));
        doPost(this.julian, expensesPath(), lunch(this.julian.id(), this.diomar.id()))
                .andExpect(status().isCreated());

        doGet(this.diomar, "/api/groups")
                .andExpect(jsonPath("$[*].id", hasItem(this.groupId.toString())));
        doGet(this.diomar, expensesPath())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /** Diomar le paga a Julian lo que le debia; Julian ve el pago en su lista. */
    @Test
    void aPaymentMadeByOneAccountIsSeenByTheOther() throws Exception {
        doPost(this.julian, membersPath(), """
                {"email": "%s"}
                """.formatted(this.diomar.email())).andExpect(status().isCreated());
        doPost(this.julian, expensesPath(), lunch(this.julian.id(), this.diomar.id())).andExpect(status().isCreated());

        doPost(this.diomar, expensesPath(), """
                {"payerId": "%s", "description": "Pago a Julian", "amountCents": 3000000, "splitType": "EXACT",
                 "category": "PAYMENT", "shares": [{"userId": "%s", "amountCents": 3000000}]}
                """.formatted(this.diomar.id(), this.julian.id()))
                .andExpect(status().isCreated());

        doGet(this.julian, expensesPath())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].category", hasItem("PAYMENT")));
    }

    /**
     * Julian ya habia agregado a "Diomar" por nombre y registrado un gasto que Diomar pago. Al
     * vincularlo con la cuenta de Diomar, el gasto pasa a esa cuenta y Diomar ve el grupo.
     */
    @Test
    void linkingMovesTheNameOnlyMemberToTheAccount() throws Exception {
        UUID byName = addMemberByName(this.julian, this.groupId, "Diomar");
        doPost(this.julian, expensesPath(), lunch(byName, this.julian.id())).andExpect(status().isCreated());

        doPost(this.julian, membersPath() + "/" + byName + "/link", """
                {"email": "%s"}
                """.formatted(this.diomar.email()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(this.diomar.id().toString()));

        String expenses = body(doGet(this.diomar, expensesPath()).andExpect(status().isOk()));
        assertThat((String) JsonPath.read(expenses, "$[0].payerId"))
                .isEqualTo(this.diomar.id().toString());
        doGet(this.julian, membersPath())
                .andExpect(jsonPath("$[*].id", hasItem(this.diomar.id().toString())))
                .andExpect(jsonPath("$[*].id", not(hasItem(byName.toString()))));
    }

    @Test
    void linkingNeedsAnExistingAccountAndAMemberWithoutOne() throws Exception {
        UUID byName = addMemberByName(this.julian, this.groupId, "Diomar");
        doPost(this.julian, membersPath() + "/" + byName + "/link", """
                {"email": "nadie@test.com"}
                """).andExpect(status().isNotFound());

        doPost(this.julian, membersPath(), """
                {"email": "%s"}
                """.formatted(this.diomar.email())).andExpect(status().isCreated());
        doPost(this.julian, membersPath() + "/" + this.diomar.id() + "/link", """
                {"email": "%s"}
                """.formatted(this.diomar.email())).andExpect(status().isConflict());
    }

    private String membersPath() {
        return "/api/groups/" + this.groupId + "/members";
    }

    private String expensesPath() {
        return "/api/groups/" + this.groupId + "/expenses";
    }

    /** Almuerzo de $60.000 que pago payer, a partes iguales con other. */
    private static String lunch(UUID payer, UUID other) {
        return """
                {"payerId": "%s", "description": "Almuerzo", "amountCents": 6000000, "splitType": "EQUAL",
                 "shares": [{"userId": "%s", "amountCents": 3000000}, {"userId": "%s", "amountCents": 3000000}]}
                """.formatted(payer, payer, other);
    }
}
