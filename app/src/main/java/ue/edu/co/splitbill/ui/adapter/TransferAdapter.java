package ue.edu.co.splitbill.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Transfer;
import ue.edu.co.splitbill.model.SettlementResult;

/**
 * Pinta las transferencias que propone el algoritmo de liquidacion.
 */
public class TransferAdapter extends RecyclerView.Adapter<TransferAdapter.TransferViewHolder> {

    private final List<Transfer> transfers = new ArrayList<>();
    private SettlementResult settlement;

    public void setSettlement(SettlementResult settlement) {
        this.settlement = settlement;
        this.transfers.clear();
        if (settlement != null) {
            this.transfers.addAll(settlement.getTransfers());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transfer, parent, false);
        return new TransferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransferViewHolder holder, int position) {
        holder.bind(this.transfers.get(position));
    }

    @Override
    public int getItemCount() {
        return this.transfers.size();
    }

    class TransferViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTransferDescription;
        private final TextView tvTransferAmount;

        TransferViewHolder(View itemView) {
            super(itemView);
            this.tvTransferDescription = itemView.findViewById(R.id.tvTransferDescription);
            this.tvTransferAmount = itemView.findViewById(R.id.tvTransferAmount);
        }

        void bind(Transfer transfer) {
            this.tvTransferDescription.setText(itemView.getContext().getString(
                    R.string.tvOwes,
                    settlement.getUserName(transfer.getFromUserId()),
                    settlement.getUserName(transfer.getToUserId())));
            this.tvTransferAmount.setText(transfer.getAmount().format());
        }
    }
}
