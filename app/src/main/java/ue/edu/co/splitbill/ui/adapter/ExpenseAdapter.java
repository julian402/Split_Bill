package ue.edu.co.splitbill.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.ActivityItem;
import ue.edu.co.splitbill.dao.ExpenseListItem;
import ue.edu.co.splitbill.ui.Categories;
import ue.edu.co.splitbill.ui.DateText;

/**
 * Pinta listas de gastos y pagos: la del grupo, la actividad reciente del inicio y la pantalla de
 * actividad. Si la fila es un ActivityItem (de cualquier grupo) muestra tambien el nombre del grupo.
 *
 * El adaptador no sabe que hacer cuando alguien toca un gasto: avisa a traves de una interfaz y es
 * la Activity la que decide. Asi el adaptador se ocupa solo de mostrar y la Activity solo de
 * coordinar, que es la responsabilidad unica de cada uno.
 */
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    /** Aviso de que el usuario toco un gasto para ver su detalle. */
    public interface OnExpenseClickListener {
        void onExpenseClick(ExpenseListItem expense);
    }

    private final List<ExpenseListItem> expenses = new ArrayList<>();
    private final String[] splitTypeLabels;
    private final OnExpenseClickListener clickListener;
    private final boolean showDayHeaders;

    /**
     * @param showDayHeaders true en la pantalla de actividad: antes del primer gasto de cada dia
     *                       aparece "Hoy", "Ayer" o la fecha
     */
    public ExpenseAdapter(String[] splitTypeLabels, OnExpenseClickListener clickListener, boolean showDayHeaders) {
        this.splitTypeLabels = splitTypeLabels;
        this.clickListener = clickListener;
        this.showDayHeaders = showDayHeaders;
    }

    public void setExpenses(List<? extends ExpenseListItem> expenses) {
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
        holder.bind(this.expenses.get(position), position);
    }

    @Override
    public int getItemCount() {
        return this.expenses.size();
    }

    /** El primer gasto de cada dia lleva el encabezado del dia. */
    private boolean startsNewDay(int position) {
        if (position == 0) {
            return true;
        }
        return DateText.daysBetween(this.expenses.get(position).getDate(),
                this.expenses.get(position - 1).getDate()) != 0;
    }

    /** El ultimo de cada dia (o de la lista) no lleva la linea divisoria de abajo. */
    private boolean endsDay(int position) {
        if (position == this.expenses.size() - 1) {
            return true;
        }
        return this.showDayHeaders && startsNewDay(position + 1);
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvDayHeader;
        private final View rowExpense;
        private final ImageView ivCategory;
        private final TextView tvDescription;
        private final TextView tvPayer;
        private final TextView tvDate;
        private final TextView tvAmount;
        private final TextView tvSplitType;
        private final View vDivider;

        ExpenseViewHolder(View itemView) {
            super(itemView);
            this.tvDayHeader = itemView.findViewById(R.id.tvDayHeader);
            this.rowExpense = itemView.findViewById(R.id.rowExpense);
            this.ivCategory = itemView.findViewById(R.id.ivCategory);
            this.tvDescription = itemView.findViewById(R.id.tvDescription);
            this.tvPayer = itemView.findViewById(R.id.tvPayer);
            this.tvDate = itemView.findViewById(R.id.tvDate);
            this.tvAmount = itemView.findViewById(R.id.tvAmount);
            this.tvSplitType = itemView.findViewById(R.id.tvSplitType);
            this.vDivider = itemView.findViewById(R.id.vDivider);
        }

        void bind(final ExpenseListItem expense, int position) {
            Context context = itemView.getContext();
            boolean newDay = showDayHeaders && startsNewDay(position);
            this.tvDayHeader.setVisibility(newDay ? View.VISIBLE : View.GONE);
            this.tvDayHeader.setText(DateText.day(context, expense.getDate()));
            this.vDivider.setVisibility(endsDay(position) ? View.GONE : View.VISIBLE);

            this.ivCategory.setImageResource(Categories.getIcon(expense.getCategory()));
            this.tvAmount.setText(expense.getAmount().format());
            this.tvDate.setText(DateText.format(context, expense.getDate()));

            String groupName = expense instanceof ActivityItem ? ((ActivityItem) expense).getGroupName() : null;
            if (expense.isPayment()) {
                //"Luis le pago a Andres": un pago no es un gasto, se cuenta distinto
                this.tvDescription.setText(context.getString(R.string.tvPaymentRow,
                        expense.getPayerNames(), expense.getPayeeNames()));
                this.tvPayer.setText(groupName == null ? context.getString(R.string.tvPaymentRegistered) : groupName);
                this.tvSplitType.setText(R.string.tvCategoryPayment);
                this.tvSplitType.setTextColor(ContextCompat.getColor(context, R.color.colorCreditor));
                this.tvSplitType.setBackgroundTintList(ContextCompat.getColorStateList(context,
                        R.color.colorCreditorContainer));
            } else {
                this.tvDescription.setText(expense.getDescription());
                String paidBy = context.getString(R.string.tvPaidBy, expense.getPayerNames());
                this.tvPayer.setText(groupName == null ? paidBy
                        : context.getString(R.string.tvDetailMeta, paidBy, groupName));
                this.tvSplitType.setText(splitTypeLabels[expense.getSplitType().getPosition()]);
                this.tvSplitType.setTextColor(ContextCompat.getColor(context, R.color.colorPrimary));
                this.tvSplitType.setBackgroundTintList(ContextCompat.getColorStateList(context,
                        R.color.colorIconCircle));
            }
            this.rowExpense.setOnClickListener(view -> clickListener.onExpenseClick(expense));
        }
    }
}
