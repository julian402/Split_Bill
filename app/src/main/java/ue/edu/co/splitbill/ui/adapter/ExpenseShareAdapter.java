package ue.edu.co.splitbill.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.ShareListItem;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.ui.Avatar;

/**
 * Pinta como se repartio un gasto: una fila por participante con su parte.
 *
 * Quien pago aparece en verde ("Pago el gasto"); los demas en rojo, con a quien le deben. Si el gasto
 * se dividio por porcentajes, debajo del monto se muestra el porcentaje de cada uno.
 */
public class ExpenseShareAdapter extends RecyclerView.Adapter<ExpenseShareAdapter.ShareViewHolder> {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final List<ShareListItem> shares = new ArrayList<>();
    private String payerId;
    private String payerNames;
    private long totalCents;
    private boolean showPercent;

    public void setDetail(Expense expense, String payerNames, List<ShareListItem> shares) {
        this.payerId = expense.getPayerId();
        this.payerNames = payerNames;
        this.totalCents = expense.getAmountCents();
        this.showPercent = expense.getSplitType() == SplitType.PERCENTAGE;
        this.shares.clear();
        if (shares != null) {
            this.shares.addAll(shares);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense_share, parent, false);
        return new ShareViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShareViewHolder holder, int position) {
        holder.bind(this.shares.get(position));
    }

    @Override
    public int getItemCount() {
        return this.shares.size();
    }

    /** Porcentaje del total que representa la parte, con maximo dos decimales: 40 %, 33,33 %. */
    private String formatPercent(long shareCents) {
        if (this.totalCents <= 0) {
            return "";
        }
        BigDecimal percent = BigDecimal.valueOf(shareCents).multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(this.totalCents), 2, RoundingMode.HALF_UP);
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        return format.format(percent) + " %";
    }

    class ShareViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvShareInitials;
        private final TextView tvShareName;
        private final TextView tvShareState;
        private final TextView tvShareAmount;
        private final TextView tvSharePercent;

        ShareViewHolder(View itemView) {
            super(itemView);
            this.tvShareInitials = itemView.findViewById(R.id.tvShareInitials);
            this.tvShareName = itemView.findViewById(R.id.tvShareName);
            this.tvShareState = itemView.findViewById(R.id.tvShareState);
            this.tvShareAmount = itemView.findViewById(R.id.tvShareAmount);
            this.tvSharePercent = itemView.findViewById(R.id.tvSharePercent);
        }

        void bind(ShareListItem share) {
            Avatar.bind(this.tvShareInitials, share.getNames());
            this.tvShareName.setText(share.getNames());
            this.tvShareAmount.setText(share.getAmount().format());

            String state;
            int colorResourceId;
            int containerResourceId;
            if (share.getUserId().equals(payerId)) {
                state = itemView.getContext().getString(R.string.tvSharePaid);
                colorResourceId = R.color.colorCreditor;
                containerResourceId = R.color.colorCreditorContainer;
            } else if (share.getAmountCents() == 0) {
                state = itemView.getContext().getString(R.string.tvShareNothing);
                colorResourceId = R.color.colorSettledBalance;
                containerResourceId = R.color.colorSettledContainer;
            } else {
                state = itemView.getContext().getString(R.string.tvShareOwes, payerNames);
                colorResourceId = R.color.colorDebtor;
                containerResourceId = R.color.colorDebtorContainer;
            }
            //La etiqueta lleva un fondo suave del mismo color que el texto, como en la liquidacion
            this.tvShareState.setText(state);
            this.tvShareState.setTextColor(ContextCompat.getColor(itemView.getContext(), colorResourceId));
            ViewCompat.setBackgroundTintList(this.tvShareState, ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), containerResourceId)));

            this.tvSharePercent.setVisibility(showPercent ? View.VISIBLE : View.GONE);
            if (showPercent) {
                this.tvSharePercent.setText(formatPercent(share.getAmountCents()));
            }
        }
    }
}
