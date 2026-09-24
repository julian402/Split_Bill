package ue.edu.co.splitbill.ui.adapter;

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
import ue.edu.co.splitbill.dao.ExpenseListItem;

/**
 * Pinta la lista de gastos de la pantalla principal.
 *
 * El adaptador no sabe que hacer cuando alguien pulsa eliminar: avisa a traves de una interfaz y es
 * la Activity la que decide. Asi el adaptador se ocupa solo de mostrar y la Activity solo de
 * coordinar, que es la responsabilidad unica de cada uno.
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    /** Aviso de que el usuario quiere eliminar un gasto. */
    public interface OnExpenseDeleteListener {
        void onExpenseDelete(ExpenseListItem expense);
    }

    /** Aviso de que el usuario toco un gasto para ver su detalle. */
    public interface OnExpenseClickListener {
        void onExpenseClick(ExpenseListItem expense);
    }

    private final List<ExpenseListItem> expenses = new ArrayList<>();
    private final String[] splitTypeLabels;
    private final OnExpenseDeleteListener deleteListener;
    private final OnExpenseClickListener clickListener;

    public ExpenseAdapter(String[] splitTypeLabels, OnExpenseDeleteListener deleteListener,
                          OnExpenseClickListener clickListener) {
        this.splitTypeLabels = splitTypeLabels;
        this.deleteListener = deleteListener;
        this.clickListener = clickListener;
    }

    public void setExpenses(List<ExpenseListItem> expenses) {
        this.expenses.clear();
        if (expenses != null) {
            this.expenses.addAll(expenses);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        holder.bind(this.expenses.get(position));
    }

    @Override
    public int getItemCount() {
        return this.expenses.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDescription;
        private final TextView tvPayer;
        private final TextView tvSplitType;
        private final TextView tvAmount;
        private final Button btnDeleteExpense;

        ExpenseViewHolder(View itemView) {
            super(itemView);
            this.tvDescription = itemView.findViewById(R.id.tvDescription);
            this.tvPayer = itemView.findViewById(R.id.tvPayer);
            this.tvSplitType = itemView.findViewById(R.id.tvSplitType);
            this.tvAmount = itemView.findViewById(R.id.tvAmount);
            this.btnDeleteExpense = itemView.findViewById(R.id.btnDeleteExpense);
        }

        void bind(final ExpenseListItem expense) {
            this.tvDescription.setText(expense.getDescription());
            this.tvPayer.setText(itemView.getContext()
                    .getString(R.string.tvPaidBy, expense.getPayerNames()));
            this.tvSplitType.setText(splitTypeLabels[expense.getSplitType().getPosition()]);
            this.tvAmount.setText(expense.getAmount().format());
            this.btnDeleteExpense.setOnClickListener(view -> deleteListener.onExpenseDelete(expense));
            itemView.setOnClickListener(view -> clickListener.onExpenseClick(expense));
        }
    }
}
