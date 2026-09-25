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
import ue.edu.co.splitbill.dao.QuickSplitListItem;
import ue.edu.co.splitbill.ui.DateText;

/** Lista de cuentas rapidas guardadas: nombre, dia, cuantas personas y total. */
public class SavedQuickSplitAdapter extends RecyclerView.Adapter<SavedQuickSplitAdapter.SavedQuickSplitViewHolder> {

    /** Quien quiere enterarse cuando se toca una cuenta. */
    public interface OnQuickSplitClickListener {
        void onQuickSplitClick(QuickSplitListItem quickSplit);
    }

    private final List<QuickSplitListItem> quickSplits = new ArrayList<>();
    private final OnQuickSplitClickListener listener;

    public SavedQuickSplitAdapter(OnQuickSplitClickListener listener) {
        this.listener = listener;
    }

    public void setQuickSplits(List<QuickSplitListItem> quickSplits) {
        this.quickSplits.clear();
        if (quickSplits != null) {
            this.quickSplits.addAll(quickSplits);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavedQuickSplitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_quick_split, parent, false);
        return new SavedQuickSplitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedQuickSplitViewHolder holder, int position) {
        holder.bind(this.quickSplits.get(position));
    }

    @Override
    public int getItemCount() {
        return this.quickSplits.size();
    }

    class SavedQuickSplitViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvQuickSplitName;
        private final TextView tvQuickSplitInfo;
        private final TextView tvQuickSplitTotal;

        SavedQuickSplitViewHolder(View itemView) {
            super(itemView);
            this.tvQuickSplitName = itemView.findViewById(R.id.tvQuickSplitName);
            this.tvQuickSplitInfo = itemView.findViewById(R.id.tvQuickSplitInfo);
            this.tvQuickSplitTotal = itemView.findViewById(R.id.tvQuickSplitTotal);
        }

        void bind(final QuickSplitListItem quickSplit) {
            this.tvQuickSplitName.setText(quickSplit.getDescription());
            String people = itemView.getResources().getQuantityString(R.plurals.tvPersonCount,
                    quickSplit.getPeopleCount(), quickSplit.getPeopleCount());
            this.tvQuickSplitInfo.setText(itemView.getContext().getString(R.string.tvQuickSplitInfo,
                    DateText.day(itemView.getContext(), quickSplit.getDate()), people));
            this.tvQuickSplitTotal.setText(quickSplit.getTotal().format());
            itemView.setOnClickListener(view -> listener.onQuickSplitClick(quickSplit));
        }
    }
}
