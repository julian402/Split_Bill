package ue.edu.co.splitbill.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.SplitType;

public record ExpenseResponse(UUID id, UUID groupId, UUID payerId, String description, long amountCents,
                              SplitType splitType, Instant date, List<ShareResponse> shares,
                              Instant createdAt, Instant updatedAt) {

    public record ShareResponse(UUID userId, long amountCents) {
    }

    public static ExpenseResponse from(Expense expense) {
        List<ShareResponse> shares = new ArrayList<>();
        for (ExpenseShare share : expense.getShares()) {
            shares.add(new ShareResponse(share.getUserId(), share.getAmountCents()));
        }
        return new ExpenseResponse(expense.getId(), expense.getGroupId(), expense.getPayerId(),
                expense.getDescription(), expense.getAmountCents(), expense.getSplitType(), expense.getDate(),
                shares, expense.getCreatedAt(), expense.getUpdatedAt());
    }
}
