package ue.edu.co.splitbill.model;

import java.util.List;

import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.QuickSplitShare;

/** Una cuenta rapida guardada con lo que le toco a cada persona, en orden. */
public class QuickSplitDetail {

    private final QuickSplit quickSplit;
    private final List<QuickSplitShare> shares;

    public QuickSplitDetail(QuickSplit quickSplit, List<QuickSplitShare> shares) {
        this.quickSplit = quickSplit;
        this.shares = shares;
    }

    public QuickSplit getQuickSplit() {
        return this.quickSplit;
    }

    public List<QuickSplitShare> getShares() {
        return this.shares;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("QuickSplitDetail{");
        sb.append("quickSplit=").append(quickSplit);
        sb.append(", shares=").append(shares.size());
        sb.append('}');
        return sb.toString();
    }
}
