package ue.edu.co.splitbill.ui.group;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.ExpenseListItem;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.ExpenseAdapter;
import ue.edu.co.splitbill.ui.expense.AddExpenseActivity;
import ue.edu.co.splitbill.ui.quick.QuickSplitActivity;
import ue.edu.co.splitbill.ui.settle.SettlementActivity;

/**
 * Pantalla principal: el total del grupo, la lista de gastos y los accesos a las demas pantallas.
 *
 * En esta entrega la aplicacion trabaja siempre sobre el grupo sembrado por defecto. Cuando entre
 * la pantalla de varios grupos, lo unico que cambia es de donde sale groupId.
 */
public class MainActivity extends BaseActivity implements ExpenseAdapter.OnExpenseDeleteListener {

    /** Un gasto no se puede repartir si no hay al menos dos integrantes. */
    private static final int MIN_MEMBERS = 2;

    private TextView tvTotal;
    private TextView tvEmptyExpenses;
    private RecyclerView rvExpenses;
    private Button btnAddExpense;
    private Button btnSettle;
    private Button btnMembers;
    private Button btnQuickSplit;

    private ExpenseAdapter expenseAdapter;
    private ExpenseRepository expenseRepository;
    private UserRepository userRepository;
    private String groupId;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initListeners() {
        this.btnAddExpense.setOnClickListener(this::openAddExpense);
        this.btnSettle.setOnClickListener(this::openSettlement);
        this.btnMembers.setOnClickListener(this::openMembers);
        this.btnQuickSplit.setOnClickListener(this::openQuickSplit);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Al volver de registrar un gasto la lista se refresca sola
        listExpensesDB();
        loadTotalDB();
    }

    private void listExpensesDB() {
        showLoading();
        this.expenseRepository.getActiveExpenses(this.groupId, new UiCallback<List<ExpenseListItem>>() {
            @Override
            protected void onData(List<ExpenseListItem> data) {
                expenseAdapter.setExpenses(data);
                tvEmptyExpenses.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadTotalDB() {
        this.expenseRepository.getTotalExpenses(this.groupId, new UiCallback<Money>() {
            @Override
            protected void onData(Money data) {
                tvTotal.setText(data.format());
            }
        });
    }

    @Override
    public void onExpenseDelete(ExpenseListItem expense) {
        showLoading();
        this.expenseRepository.deleteExpense(expense.getExpenseId(), new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                listExpensesDB();
                loadTotalDB();
            }
        });
    }

    /** Antes de abrir el formulario se verifica que haya con quien repartir el gasto. */
    private void openAddExpense(View view) {
        this.userRepository.countActiveUsers(new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                if (data < MIN_MEMBERS) {
                    showToast(R.string.msgNeedTwoMembers);
                    openMembers(null);
                    return;
                }
                startActivity(new Intent(MainActivity.this, AddExpenseActivity.class));
            }
        });
    }

    private void openSettlement(View view) {
        startActivity(new Intent(this, SettlementActivity.class));
    }

    private void openMembers(View view) {
        startActivity(new Intent(this, MembersActivity.class));
    }

    /** La cuenta rapida no necesita integrantes registrados: se abre siempre. */
    private void openQuickSplit(View view) {
        startActivity(new Intent(this, QuickSplitActivity.class));
    }

    @Override
    protected void initObjects() {
        this.tvTotal = findViewById(R.id.tvTotal);
        this.tvEmptyExpenses = findViewById(R.id.tvEmptyExpenses);
        this.rvExpenses = findViewById(R.id.rvExpenses);
        this.btnAddExpense = findViewById(R.id.btnAddExpense);
        this.btnSettle = findViewById(R.id.btnSettle);
        this.btnMembers = findViewById(R.id.btnMembers);
        this.btnQuickSplit = findViewById(R.id.btnQuickSplit);

        this.groupId = DatabaseContract.DEFAULT_GROUP_ID;
        this.expenseRepository = getServiceLocator().getExpenseRepository();
        this.userRepository = getServiceLocator().getUserRepository();

        this.expenseAdapter = new ExpenseAdapter(
                getResources().getStringArray(R.array.splitTypes), this);
        this.rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        this.rvExpenses.setAdapter(this.expenseAdapter);

        this.tvTotal.setText(Money.ZERO.format());
    }
}
