package ue.edu.co.splitbill.network;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ue.edu.co.splitbill.domain.ExpenseCategory;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.Message;
import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.QuickSplitShare;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.network.dto.ExpenseDto;
import ue.edu.co.splitbill.network.dto.GroupDto;
import ue.edu.co.splitbill.network.dto.MemberRequest;
import ue.edu.co.splitbill.network.dto.MessageDto;
import ue.edu.co.splitbill.network.dto.QuickSplitDto;
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

    /** La pertenencia de la persona al grupo, tal como la tiene el servidor. */
    public static GroupMember toMember(String groupId, UserDto dto) {
        GroupMember member = new GroupMember(groupId, dto.getId(), SyncStatus.SYNCED);
        member.setStatus(dto.isActive() ? DatabaseContract.STATUS_ACTIVE : DatabaseContract.STATUS_INACTIVE);
        return member;
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
        ExpenseDto dto = new ExpenseDto(expense.getId(), expense.getGroupId(), expense.getPayerId(),
                expense.getDescription(), expense.getAmountCents(), expense.getSplitType().name(),
                formatDate(expense.getDate()), shareDtos);
        dto.setCategory(expense.getCategory().name());
        return dto;
    }

    public static Expense toEntity(ExpenseDto dto) {
        Expense expense = new Expense();
        expense.setId(dto.getId());
        expense.setGroupId(dto.getGroupId());
        expense.setPayerId(dto.getPayerId());
        expense.setDescription(dto.getDescription());
        expense.setAmountCents(dto.getAmountCents());
        expense.setSplitType(SplitType.valueOf(dto.getSplitType()));
        expense.setCategory(ExpenseCategory.fromName(dto.getCategory()));
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
    /** Lo que se manda al escribir: el servidor pone el grupo y a quien lo escribio (la sesion). */
    public static MessageDto toDto(Message message) {
        return new MessageDto(message.getId(), message.getText(), formatDate(message.getSentAt()));
    }

    public static Message toEntity(MessageDto dto) {
        Message message = new Message();
        message.setId(dto.getId());
        message.setGroupId(dto.getGroupId());
        message.setSenderId(dto.getSenderId());
        message.setSenderNames(dto.getSenderNames());
        message.setText(dto.getText());
        message.setSentAt(parseDate(dto.getSentAt()));
        message.setSyncStatus(SyncStatus.SYNCED);
        return message;
    }

    /** Cuenta rapida con sus partes, en el orden en que se escribieron. */
    public static QuickSplitDto toDto(QuickSplit quickSplit, List<QuickSplitShare> shares) {
        List<QuickSplitDto.Share> shareDtos = new ArrayList<>(shares.size());
        for (QuickSplitShare share : shares) {
            shareDtos.add(new QuickSplitDto.Share(share.getName(), share.getAmountCents()));
        }
        QuickSplitDto dto = new QuickSplitDto();
        dto.setId(quickSplit.getId());
        dto.setDescription(quickSplit.getDescription());
        dto.setSubtotalCents(quickSplit.getSubtotalCents());
        dto.setTipPercent(new BigDecimal(quickSplit.getTipPercent()));
        dto.setTotalCents(quickSplit.getTotalCents());
        dto.setSplitType(quickSplit.getSplitType().name());
        dto.setDate(formatDate(quickSplit.getDate()));
        dto.setShares(shareDtos);
        return dto;
    }

    public static QuickSplit toEntity(QuickSplitDto dto) {
        QuickSplit quickSplit = new QuickSplit();
        quickSplit.setId(dto.getId());
        quickSplit.setDescription(dto.getDescription());
        quickSplit.setSubtotalCents(dto.getSubtotalCents());
        quickSplit.setTipPercent(dto.getTipPercent() == null ? "0"
                : dto.getTipPercent().stripTrailingZeros().toPlainString());
        quickSplit.setTotalCents(dto.getTotalCents());
        quickSplit.setSplitType(SplitType.valueOf(dto.getSplitType()));
        quickSplit.setDate(parseDate(dto.getDate()));
        quickSplit.setSyncStatus(SyncStatus.SYNCED);
        return quickSplit;
    }

    /** Las partes llegan en orden; su lugar en la lista es su posicion. */
    public static List<QuickSplitShare> toQuickSplitShares(QuickSplitDto dto) {
        List<QuickSplitShare> shares = new ArrayList<>(dto.getShares().size());
        for (int i = 0; i < dto.getShares().size(); i++) {
            QuickSplitShare share = new QuickSplitShare();
            share.setQuickSplitId(dto.getId());
            share.setPosition(i);
            share.setName(dto.getShares().get(i).getName());
            share.setAmountCents(dto.getShares().get(i).getAmountCents());
            shares.add(share);
        }
        return shares;
    }

    public static String formatDate(Date date) {
        return date == null ? null : Instant.ofEpochMilli(date.getTime()).toString();
    }

    public static Date parseDate(String text) {
        return text == null ? new Date() : new Date(Instant.parse(text).toEpochMilli());
    }
}
