package ue.edu.co.splitbill.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Balance;
import ue.edu.co.splitbill.model.SettlementResult;

/**
 * Pinta el saldo neto de cada integrante: verde si le deben, rojo si debe, gris si esta al dia.
 */
public class BalanceAdapter extends RecyclerView.Adapter<BalanceAdapter.BalanceViewHolder> {

    private final List<Balance> balances = new ArrayList<>();
    private SettlementResult settlement;

    public void setSettlement(SettlementResult settlement) {
        this.settlement = settlement;
        this.balances.clear();
        if (settlement != null) {
            this.balances.addAll(settlement.getBalances());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BalanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_balance, parent, false);
        return new BalanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BalanceViewHolder holder, int position) {
        holder.bind(this.balances.get(position));
    }

    @Override
    public int getItemCount() {
        return this.balances.size();
    }

    class BalanceViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvBalanceName;
        private final TextView tvBalanceState;
        private final TextView tvBalanceAmount;

        BalanceViewHolder(View itemView) {
            super(itemView);
            this.tvBalanceName = itemView.findViewById(R.id.tvBalanceName);
            this.tvBalanceState = itemView.findViewById(R.id.tvBalanceState);
            this.tvBalanceAmount = itemView.findViewById(R.id.tvBalanceAmount);
        }

        void bind(Balance balance) {
            this.tvBalanceName.setText(settlement.getUserName(balance.getUserId()));

            int stateResourceId;
            int colorResourceId;
            if (balance.isCreditor()) {
                stateResourceId = R.string.tvCreditor;
                colorResourceId = R.color.colorCreditor;
            } else if (balance.isDebtor()) {
                stateResourceId = R.string.tvDebtor;
                colorResourceId = R.color.colorDebtor;
            } else {
                stateResourceId = R.string.tvSettled;
                colorResourceId = R.color.colorSettledBalance;
            }

            int color = ContextCompat.getColor(itemView.getContext(), colorResourceId);
            this.tvBalanceState.setText(stateResourceId);
            this.tvBalanceState.setTextColor(color);
            this.tvBalanceAmount.setTextColor(color);
            //Se muestra el valor absoluto: el estado ya dice si debe o le deben
            this.tvBalanceAmount.setText(balance.getAmount().abs().format());
        }
    }
}
