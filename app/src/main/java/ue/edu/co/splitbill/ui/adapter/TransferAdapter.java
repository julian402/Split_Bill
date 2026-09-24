package ue.edu.co.splitbill.ui.adapter;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    /** Botones de cada transferencia: compartirla (por WhatsApp, por ejemplo) o marcarla como pagada. */
    public interface OnTransferActionListener {
        void onShareTransfer(Transfer transfer);

        void onMarkPaid(Transfer transfer);
    }

    private final OnTransferActionListener listener;

    public TransferAdapter(OnTransferActionListener listener) {
        this.listener = listener;
    }

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
        private final Button btnShareTransfer;
        private final Button btnMarkPaid;

        TransferViewHolder(View itemView) {
            super(itemView);
            this.tvTransferDescription = itemView.findViewById(R.id.tvTransferDescription);
            this.tvTransferAmount = itemView.findViewById(R.id.tvTransferAmount);
            this.btnShareTransfer = itemView.findViewById(R.id.btnShareTransfer);
            this.btnMarkPaid = itemView.findViewById(R.id.btnMarkPaid);
        }

        void bind(final Transfer transfer) {
            String from = settlement.getUserName(transfer.getFromUserId());
            String to = settlement.getUserName(transfer.getToUserId());
            String text = itemView.getContext().getString(R.string.tvOwes, from, to);
            //los nombres en negrita, como en el diseno: "Luis le paga a Andres"
            SpannableString styled = new SpannableString(text);
            styled.setSpan(new StyleSpan(Typeface.BOLD), 0, from.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            styled.setSpan(new StyleSpan(Typeface.BOLD), text.length() - to.length(), text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            this.tvTransferDescription.setText(styled);
            this.tvTransferAmount.setText(transfer.getAmount().format());
            this.btnShareTransfer.setOnClickListener(view -> listener.onShareTransfer(transfer));
            this.btnMarkPaid.setOnClickListener(view -> listener.onMarkPaid(transfer));
        }
    }
}
