package ue.edu.co.splitbill.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import ue.edu.co.splitbill.domain.ExpenseCategory;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.network.dto.ExpenseDto;
import ue.edu.co.splitbill.network.dto.MemberRequest;
import ue.edu.co.splitbill.network.dto.ShareDto;
import ue.edu.co.splitbill.network.dto.UserDto;

/**
 * Pruebas de la traduccion entre Room y la red. Son Java puro: corren sin emulador.
 */
public class ApiMapperTest {

    private static final String GROUP_ID = "7b0f5a3e-0000-4000-8000-000000000001";
    private static final String JULIAN = "7b0f5a3e-0000-4000-8000-00000000000a";
    private static final String DIOMAR = "7b0f5a3e-0000-4000-8000-00000000000b";

    @Test
    public void expenseKeepsIdAmountsAndDateWhenGoingToTheServerAndBack() {
        Expense expense = new Expense(GROUP_ID, JULIAN, "Almuerzo", Money.ofCents(6_000_000L), SplitType.EQUAL);
        expense.setDate(new Date(1_790_000_000_123L));
        List<ExpenseShare> shares = Arrays.asList(share(expense, JULIAN, 3_000_000L), share(expense, DIOMAR, 3_000_000L));

        ExpenseDto dto = ApiMapper.toDto(expense, shares);
        Expense back = ApiMapper.toEntity(dto);

        assertEquals(expense.getId(), dto.getId());
        assertEquals("EQUAL", dto.getSplitType());
        assertEquals(2, dto.getShares().size());
        assertEquals(expense.getId(), back.getId());
        assertEquals(6_000_000L, back.getAmountCents());
        assertEquals(SplitType.EQUAL, back.getSplitType());
        assertEquals(expense.getDate(), back.getDate());
    }

    @Test
    public void theCategoryTravelsAndAnOldServerWithoutItMeansOther() {
        Expense expense = new Expense(GROUP_ID, JULIAN, "Taxi", Money.ofCents(1_000L), SplitType.EXACT);
        expense.setCategory(ExpenseCategory.TRANSPORT);
        ExpenseDto dto = ApiMapper.toDto(expense, Arrays.asList(share(expense, JULIAN, 1_000L)));
        assertEquals("TRANSPORT", dto.getCategory());
        assertEquals(ExpenseCategory.TRANSPORT, ApiMapper.toEntity(dto).getCategory());

        dto.setCategory(null);
        assertEquals(ExpenseCategory.OTHER, ApiMapper.toEntity(dto).getCategory());
    }

    @Test
    public void whatComesFromTheServerIsAlreadySynced() {
        Expense expense = ApiMapper.toEntity(new ExpenseDto("e1", GROUP_ID, JULIAN, "Taxi", 1_000L, "EXACT",
                "2026-09-24T13:55:21Z", Arrays.asList(new ShareDto(JULIAN, 1_000L))));
        assertEquals(SyncStatus.SYNCED, expense.getSyncStatus());
        assertEquals(DatabaseContract.STATUS_ACTIVE, expense.getStatus());
    }

    @Test
    public void sharesPointToTheirExpense() {
        ExpenseDto dto = new ExpenseDto("e1", GROUP_ID, JULIAN, "Taxi", 1_000L, "EXACT", "2026-09-24T13:55:21Z",
                Arrays.asList(new ShareDto(JULIAN, 600L), new ShareDto(DIOMAR, 400L)));
        List<ExpenseShare> shares = ApiMapper.toShares(dto);
        assertEquals(2, shares.size());
        assertEquals("e1", shares.get(0).getExpenseId());
        assertEquals(DIOMAR, shares.get(1).getUserId());
        assertEquals(400L, shares.get(1).getAmountCents());
    }

    /** El servidor (Java Instant) puede mandar microsegundos; se conservan los milisegundos. */
    @Test
    public void serverDatesWithMicrosecondsAreParsed() {
        Date date = ApiMapper.parseDate("2026-09-24T13:55:21.123456Z");
        assertEquals(123L, date.getTime() % 1000);
        assertEquals("2026-09-24T13:55:21.123Z", ApiMapper.formatDate(date));
    }

    @Test
    public void memberRemovedFromTheGroupArrivesInactive() {
        User user = ApiMapper.toEntity(new UserDto(DIOMAR, "Diomar", null, null, false, false));
        assertEquals(DatabaseContract.STATUS_INACTIVE, user.getStatus());
        assertFalse(user.isActive());
        assertEquals(SyncStatus.SYNCED, user.getSyncStatus());
    }

    @Test
    public void localMemberIsSentWithItsOwnIdAndWithoutEmail() {
        User user = new User("Sofia", null, null);
        MemberRequest request = ApiMapper.toMemberRequest(user);
        assertEquals(user.getId(), request.getId());
        assertEquals("Sofia", request.getNames());
        assertNull(request.getEmail());
    }

    @Test
    public void onlyClientErrorsAreDiscarded() {
        assertTrue(new ApiException(400, "partes").isPermanent());
        assertTrue(new ApiException(409, "conflicto").isPermanent());
        assertFalse(new ApiException(401, "token").isPermanent());
        assertFalse(new ApiException(429, "espera").isPermanent());
        assertFalse(new ApiException(500, "caido").isPermanent());
    }

    private static ExpenseShare share(Expense expense, String userId, long cents) {
        ExpenseShare share = new ExpenseShare();
        share.setExpenseId(expense.getId());
        share.setUserId(userId);
        share.setAmountCents(cents);
        return share;
    }
}
