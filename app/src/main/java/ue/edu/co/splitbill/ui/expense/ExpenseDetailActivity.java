package ue.edu.co.splitbill.ui.expense;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Locale;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.model.ExpenseDetail;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.Categories;
import ue.edu.co.splitbill.ui.adapter.ExpenseShareAdapter;

/**
 * Detalle de un gasto: cuanto fue, quien pago, cuando, como se dividio y cuanto le toca a cada uno.
 *
 * Se abre al tocar un gasto en la pantalla principal. Lee de la base de datos del celular, asi que
 * funciona igual sin conexion. Si mientras esta abierta llega una sincronizacion que borra el gasto
 * (otro integrante lo elimino), la pantalla se cierra sola.
 */
public class ExpenseDetailActivity extends BaseActivity implements SyncListener {

    /** Id del gasto a mostrar. Lo mandan el grupo, el inicio y la actividad. */
    public static final String EXTRA_EXPENSE_ID = "extraExpenseId";

    private static final Locale DATE_LOCALE = Locale.forLanguageTag("es-CO");

    private TextView tvDetailDescription;
    private TextView tvDetailAmount;
    private TextView tvDetailPayer;
    private TextView tvDetailMeta;
    private TextView tvDetailSyncState;
    private TextView tvDetailCategory;
    private ImageView ivHeroDecoIcon;
    private RecyclerView rvShares;
    private Button btnEditExpense;
    private Button btnDeleteExpense;

    private ExpenseShareAdapter shareAdapter;
    private ExpenseRepository expenseRepository;
    private SyncManager syncManager;
    private String expenseId;
    private String description;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_expense_detail;
    }

    @Override
    protected void initListeners() {
        this.btnEditExpense.setOnClickListener(this::openEdit);
        this.btnDeleteExpense.setOnClickListener(this::confirmDelete);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDetailDB();
        this.syncManager.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.syncManager.removeListener(this);
    }

    private void loadDetailDB() {
        showLoading();
        this.expenseRepository.getExpenseDetail(this.expenseId, new UiCallback<ExpenseDetail>() {
            @Override
            protected void onData(ExpenseDetail data) {
                showDetail(data);
            }

            @Override
            public void onError(String message) {
                //el gasto ya no existe: no hay nada que mostrar, se vuelve a la lista
                super.onError(message);
                finish();
            }
        });
    }

    private void showDetail(ExpenseDetail detail) {
        Expense expense = detail.getExpense();
        this.description = expense.getDescription();
        String[] splitTypes = getResources().getStringArray(R.array.splitTypes);
        //en espanol de Colombia, igual que los montos (Money), aunque el celular este en otro idioma
        String date = DateFormat.getDateInstance(DateFormat.LONG, DATE_LOCALE).format(expense.getDate());

        this.tvDetailCategory.setText(Categories.getName(this, expense.getCategory()));
        this.ivHeroDecoIcon.setImageResource(Categories.getIcon(expense.getCategory()));
        //un pago no se edita: si estuvo mal, se elimina y se vuelve a marcar desde la liquidacion
        this.btnEditExpense.setVisibility(expense.isPayment() ? View.GONE : View.VISIBLE);
        this.tvDetailDescription.setText(expense.getDescription());
        this.tvDetailAmount.setText(expense.getAmount().format());
        this.tvDetailPayer.setText(getString(R.string.tvPaidBy, detail.getPayerNames()));
        this.tvDetailMeta.setText(getString(R.string.tvDetailMeta, date,
                splitTypes[expense.getSplitType().getPosition()]));
        this.tvDetailSyncState.setVisibility(
                expense.getSyncStatus() == SyncStatus.SYNCED ? View.GONE : View.VISIBLE);
        this.shareAdapter.setDetail(expense, detail.getPayerNames(), detail.getShares());
    }

    /** Abre el mismo formulario de "Nuevo gasto", pero lleno con este gasto. Al guardar vuelve aqui. */
    private void openEdit(View view) {
        Intent intent = new Intent(this, AddExpenseActivity.class);
        intent.putExtra(AddExpenseActivity.EXTRA_EXPENSE_ID, this.expenseId);
        startActivity(intent);
    }

    private void confirmDelete(View view) {
        confirm(getString(R.string.dlgDeleteExpenseTitle, this.description),
                getString(R.string.dlgDeleteExpenseMessage),
                R.string.btnDelete,
                this::deleteExpenseDB);
    }

    private void deleteExpenseDB() {
        showLoading();
        this.expenseRepository.deleteExpense(this.expenseId, new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                showToast(R.string.msgExpenseDeleted);
                finish();
            }
        });
    }

    @Override
    public void onSyncStarted() {
        //no se muestra nada: la pantalla principal ya indica el estado de la sincronizacion
    }

    @Override
    public void onSyncFinished(SyncResult result) {
        if (isAlive() && result.isSynced()) {
            loadDetailDB();
        }
    }

    @Override
    protected void initObjects() {
        this.tvDetailDescription = findViewById(R.id.tvDetailDescription);
        this.tvDetailAmount = findViewById(R.id.tvDetailAmount);
        this.tvDetailPayer = findViewById(R.id.tvDetailPayer);
        this.tvDetailMeta = findViewById(R.id.tvDetailMeta);
        this.tvDetailSyncState = findViewById(R.id.tvDetailSyncState);
        this.tvDetailCategory = findViewById(R.id.tvDetailCategory);
        this.ivHeroDecoIcon = findViewById(R.id.ivHeroDecoIcon);
        this.rvShares = findViewById(R.id.rvShares);
        this.btnEditExpense = findViewById(R.id.btnEditExpense);
        this.btnDeleteExpense = findViewById(R.id.btnDeleteExpense);

        this.expenseId = getIntent().getStringExtra(EXTRA_EXPENSE_ID);
        this.expenseRepository = getServiceLocator().getExpenseRepository();
        this.syncManager = getServiceLocator().getSyncManager();

        this.shareAdapter = new ExpenseShareAdapter();
        this.rvShares.setLayoutManager(new LinearLayoutManager(this));
        this.rvShares.setAdapter(this.shareAdapter);
    }
}
