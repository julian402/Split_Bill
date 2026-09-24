package ue.edu.co.splitbill.network;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.network.dto.ExpenseDto;
import ue.edu.co.splitbill.network.dto.GroupDto;
import ue.edu.co.splitbill.network.dto.MemberRequest;
import ue.edu.co.splitbill.network.dto.ShareDto;
import ue.edu.co.splitbill.network.dto.UserDto;

/**
 * Traduce entre las entidades de Room y los objetos que viajan por la red.
 *
 * Se mantienen separados a proposito: las entidades tienen campos que solo existen en el celular
 * (sync_status) y los DTO tienen el formato que espera el servidor (fechas como texto ISO-8601).
 * Todo lo que llega del servidor se guarda como SYNCED: ya esta igual en los dos lados.
 */
public final class ApiMapper {

    private ApiMapper() {
        //impide crear objetos de esta clase
    }

    public static GroupDto toDto(Group group) {
        return new GroupDto(group.getId(), group.getName(), group.getCurrency(), null);
    }

    public static Group toEntity(GroupDto dto) {
        Group group = new Group(dto.getName(), dto.getCurrency());
        group.setId(dto.getId());
        group.setOwnerId(dto.getOwnerId());
        group.setSyncStatus(SyncStatus.SYNCED);
        return group;
    }

    /** Un integrante local se sube por nombre, conservando su UUID. */
    public static MemberRequest toMemberRequest(User user) {
        String phone = user.getPhone() == null || user.getPhone().trim().isEmpty() ? null : user.getPhone().trim();
        return new MemberRequest(user.getId(), user.getNames(), null, phone);
    }

    public static User toEntity(UserDto dto) {
        User user = new User(dto.getNames(), dto.getEmail(), dto.getPhone());
        user.setId(dto.getId());
        user.setStatus(dto.isActive() ? DatabaseContract.STATUS_ACTIVE : DatabaseContract.STATUS_INACTIVE);
        user.setSyncStatus(SyncStatus.SYNCED);
        return user;
    }

    public static ExpenseDto toDto(Expense expense, List<ExpenseShare> shares) {
        List<ShareDto> shareDtos = new ArrayList<>(shares.size());
        for (ExpenseShare share : shares) {
            shareDtos.add(new ShareDto(share.getUserId(), share.getAmountCents()));
        }
        return new ExpenseDto(expense.getId(), expense.getGroupId(), expense.getPayerId(),
                expense.getDescription(), expense.getAmountCents(), expense.getSplitType().name(),
                formatDate(expense.getDate()), shareDtos);
    }

    public static Expense toEntity(ExpenseDto dto) {
        Expense expense = new Expense();
        expense.setId(dto.getId());
        expense.setGroupId(dto.getGroupId());
        expense.setPayerId(dto.getPayerId());
        expense.setDescription(dto.getDescription());
        expense.setAmountCents(dto.getAmountCents());
        expense.setSplitType(SplitType.valueOf(dto.getSplitType()));
        expense.setDate(parseDate(dto.getDate()));
        expense.setSyncStatus(SyncStatus.SYNCED);
        return expense;
    }

    public static List<ExpenseShare> toShares(ExpenseDto dto) {
        List<ExpenseShare> shares = new ArrayList<>(dto.getShares().size());
        for (ShareDto shareDto : dto.getShares()) {
            ExpenseShare share = new ExpenseShare();
            share.setExpenseId(dto.getId());
            share.setUserId(shareDto.getUserId());
            share.setAmountCents(shareDto.getAmountCents());
            shares.add(share);
        }
        return shares;
    }

    /** Fecha en formato ISO-8601 en UTC, por ejemplo 2026-09-24T13:55:21.123Z. */
    public static String formatDate(Date date) {
        return date == null ? null : Instant.ofEpochMilli(date.getTime()).toString();
    }

    public static Date parseDate(String text) {
        return text == null ? new Date() : new Date(Instant.parse(text).toEpochMilli());
    }
}
