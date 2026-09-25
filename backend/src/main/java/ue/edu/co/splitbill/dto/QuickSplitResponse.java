package ue.edu.co.splitbill.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.QuickSplitShare;
import ue.edu.co.splitbill.entity.SplitType;

/** Cuenta rapida guardada, con las partes en el orden de la app. */
public record QuickSplitResponse(UUID id, String description, long subtotalCents, BigDecimal tipPercent,
                                 long totalCents, SplitType splitType, Instant date, List<ShareResponse> shares,
                                 Instant createdAt, Instant updatedAt) {

    public record ShareResponse(String name, long amountCents) {
    }

    public static QuickSplitResponse from(QuickSplit quickSplit) {
        List<ShareResponse> shares = new ArrayList<>();
        for (QuickSplitShare share : quickSplit.getShares()) {
            shares.add(new ShareResponse(share.getName(), share.getAmountCents()));
        }
        return new QuickSplitResponse(quickSplit.getId(), quickSplit.getDescription(), quickSplit.getSubtotalCents(),
                quickSplit.getTipPercent(), quickSplit.getTotalCents(), quickSplit.getSplitType(), quickSplit.getDate(),
                shares, quickSplit.getCreatedAt(), quickSplit.getUpdatedAt());
    }
}
