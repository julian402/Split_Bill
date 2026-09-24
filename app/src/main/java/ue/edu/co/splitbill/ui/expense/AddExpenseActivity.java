package ue.edu.co.splitbill.ui.expense;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.ShareListItem;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.model.ExpenseDetail;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.ParticipantAdapter;
import ue.edu.co.splitbill.ui.group.MainActivity;
import ue.edu.co.splitbill.ui.group.MembersActivity;

/**
 * Pantalla para registrar un gasto o editar uno que ya existe.
 *
 * Es el punto donde se aprecia el polimorfismo del proyecto: esta clase no conoce ninguna de las
 * estrategias de division. Solo traduce lo que escogio el usuario en el Spinner a un SplitType y se
 * lo entrega al repositorio junto con los participantes y los valores digitados. Quien decide como
 * repartir es SplitStrategyFactory, asi que agregar una cuarta forma de dividir no obliga a tocar
 * esta pantalla: basta con agregar la clase nueva y el texto correspondiente en strings.xml.
 *
 * Si llega EXTRA_EXPENSE_ID, la pantalla abre en modo edicion: el mismo formulario, lleno con el gasto.
 * En los dos modos, si el usuario intenta salir con cambios sin guardar, se le pregunta antes.
 */
public class AddExpenseActivity extends BaseActivity {

    /** Descripcion con la que llega el formulario ya lleno, por ejemplo desde la cuenta rapida. */
    public static final String EXTRA_DESCRIPTION = "extraDescription";

    /** Monto en centavos con el que llega el formulario ya lleno. */
    public static final String EXTRA_AMOUNT_CENTS = "extraAmountCents";

    /** Id del gasto a editar. Si no llega, la pantalla registra un gasto nuevo. */
    public static final String EXTRA_EXPENSE_ID = "extraEditExpenseId";

    /** Un gasto no se puede repartir si no hay al menos dos integrantes. */
    private static final int MIN_MEMBERS = 2;

    private static final String KEY_INITIAL_SNAPSHOT = "initialSnapshot";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private EditText etDescription;
    private EditText etAmount;
    private Spinner spPayer;
    private Spinner spSplitType;
    private RecyclerView rvParticipants;
    private Button btnSaveExpense;

    private ParticipantAdapter participantAdapter;
    private ArrayAdapter<User> payerAdapter;
    private ExpenseRepository expenseRepository;
    private UserRepository userRepository;
    private String groupId;
    private String editingExpenseId;

    private Expense expense;
    private SplitRequest splitRequest;

    /** Estado del adaptador guardado antes de girar el celular, pendiente de restaurar. */
    private Bundle pendingParticipantState;

    /** Como estaba el formulario al abrirse; si al salir es distinto, hay cambios sin guardar. */
    private String initialSnapshot;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_add_expense;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Se guarda para restaurarlo cuando termine de cargar la lista de integrantes
        this.pendingParticipantState = savedInstanceState;
        if (savedInstanceState != null) {
            this.initialSnapshot = savedInstanceState.getString(KEY_INITIAL_SNAPSHOT);
        } else if (!isEditing()) {
            prefillFromIntent();
        }
    }

    private boolean isEditing() {
        return this.editingExpenseId != null;
    }

    /**
     * Llena el formulario cuando la pantalla se abre desde la cuenta rapida.
     * Solo se hace la primera vez: al girar el celular manda lo que el usuario ya habia escrito.
     */
    private void prefillFromIntent() {
        String description = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        if (description != null) {
            this.etDescription.setText(description);
            this.etDescription.setSelection(description.length());
        }
        long amountCents = getIntent().getLongExtra(EXTRA_AMOUNT_CENTS, 0L);
        if (amountCents > 0) {
            this.etAmount.setText(toPlainAmount(amountCents));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.participantAdapter.saveState(outState);
        outState.putString(KEY_INITIAL_SNAPSHOT, this.initialSnapshot);
    }

    @Override
    protected void initListeners() {
        this.btnSaveExpense.setOnClickListener(this::saveExpenseDB);
        this.spSplitType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Al cambiar el tipo de division aparecen o desaparecen los campos de valor
                participantAdapter.setSplitType(SplitType.fromPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //no se usa
            }
        });

        //Tanto el boton atras del celular como la flecha de la barra pasan por aqui
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExitIfChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        listMembersDB();
    }

    private void listMembersDB() {
        showLoading();
        this.userRepository.getActiveUsers(new UiCallback<List<User>>() {
            @Override
            protected void onData(List<User> data) {
                if (data.size() < MIN_MEMBERS && !isEditing()) {
                    //No hay entre quienes repartir: se lleva al usuario a crear integrantes. Se le pasa
                    //lo que traia este formulario (por ejemplo, el total de la cuenta rapida) para que,
                    //al volver con el boton "Continuar con el gasto", no tenga que escribirlo otra vez.
                    showToast(R.string.msgNeedTwoMembers);
                    Intent intent = new Intent(AddExpenseActivity.this, MembersActivity.class);
                    intent.putExtras(getIntent());
                    intent.putExtra(MembersActivity.EXTRA_CONTINUE_TO_EXPENSE, true);
                    startActivity(intent);
                    finish();
                    return;
                }
                payerAdapter.clear();
                payerAdapter.addAll(data);
                participantAdapter.setParticipants(data);

                //Si venimos de girar el celular, se devuelve lo marcado y lo digitado
                if (pendingParticipantState != null) {
                    participantAdapter.restoreState(pendingParticipantState);
                    pendingParticipantState = null;
                } else if (initialSnapshot == null) {
                    if (isEditing()) {
                        loadExpenseToEditDB();
                    } else {
                        initialSnapshot = takeSnapshot();
                    }
                }
            }
        });
    }

    /** Modo edicion: trae el gasto y sus partes y llena el formulario con ellos. */
    private void loadExpenseToEditDB() {
        showLoading();
        this.expenseRepository.getExpenseDetail(this.editingExpenseId, new UiCallback<ExpenseDetail>() {
            @Override
            protected void onData(ExpenseDetail data) {
                fillForm(data);
                initialSnapshot = takeSnapshot();
            }

            @Override
            public void onError(String message) {
                super.onError(message);
                finish();
            }
        });
    }

    private void fillForm(ExpenseDetail detail) {
        Expense current = detail.getExpense();
        this.etDescription.setText(current.getDescription());
        this.etAmount.setText(toPlainAmount(current.getAmountCents()));
        for (int i = 0; i < this.payerAdapter.getCount(); i++) {
            if (this.payerAdapter.getItem(i).getId().equals(current.getPayerId())) {
                this.spPayer.setSelection(i);
                break;
            }
        }
        this.spSplitType.setSelection(current.getSplitType().getPosition());
        this.participantAdapter.setSplitType(current.getSplitType());

        Set<String> ids = new LinkedHashSet<>();
        for (ShareListItem share : detail.getShares()) {
            ids.add(share.getUserId());
        }
        this.participantAdapter.preload(ids, valuesFor(current, detail.getShares()));
    }

    /**
     * Valor que se habia digitado para cada participante, reconstruido desde su parte en centavos:
     * el monto exacto, o el porcentaje del total. Los porcentajes se redondean a dos decimales y la
     * diferencia de redondeo se le suma al mayor, para que sigan sumando exactamente 100.
     */
    private Map<String, String> valuesFor(Expense current, List<ShareListItem> shares) {
        Map<String, String> values = new LinkedHashMap<>();
        if (current.getSplitType() == SplitType.EXACT) {
            for (ShareListItem share : shares) {
                values.put(share.getUserId(), toPlainAmount(share.getAmountCents()));
            }
        } else if (current.getSplitType() == SplitType.PERCENTAGE && current.getAmountCents() > 0) {
            Map<String, BigDecimal> percents = new LinkedHashMap<>();
            BigDecimal sum = BigDecimal.ZERO;
            String largestId = null;
            for (ShareListItem share : shares) {
                BigDecimal percent = BigDecimal.valueOf(share.getAmountCents()).multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(current.getAmountCents()), 2, RoundingMode.HALF_UP);
                percents.put(share.getUserId(), percent);
                sum = sum.add(percent);
                if (largestId == null || percent.compareTo(percents.get(largestId)) > 0) {
                    largestId = share.getUserId();
                }
            }
            if (largestId != null) {
                percents.put(largestId, percents.get(largestId).add(ONE_HUNDRED.subtract(sum)));
            }
            for (Map.Entry<String, BigDecimal> entry : percents.entrySet()) {
                values.put(entry.getKey(), entry.getValue().stripTrailingZeros().toPlainString());
            }
        }
        return values;
    }

    //metodo para insertar o actualizar en la db
    private void saveExpenseDB(View view) {
        try {
            getData();
        } catch (IllegalArgumentException e) {
            //Errores de digitacion: se avisan de una vez, sin ir a la base de datos
            showToast(e.getMessage());
            return;
        }

        showLoading();
        this.btnSaveExpense.setEnabled(false);
        UiCallback<Expense> callback = new UiCallback<Expense>() {
            @Override
            protected void onData(Expense data) {
                if (isEditing()) {
                    //se vuelve al detalle del gasto, que se recarga solo con los datos nuevos
                    showToast(R.string.msgExpenseUpdated);
                    finish();
                } else {
                    showToast(R.string.msgExpenseSaved);
                    goToExpenseList();
                }
            }

            @Override
            public void onError(String message) {
                super.onError(message);
                btnSaveExpense.setEnabled(true);
            }
        };
        if (isEditing()) {
            this.expenseRepository.updateExpense(this.expense, this.splitRequest, callback);
        } else {
            this.expenseRepository.insertExpense(this.expense, this.splitRequest, callback);
        }
    }

    /**
     * Despues de guardar se vuelve a la lista de gastos, donde el gasto nuevo aparece de primero.
     * CLEAR_TOP cierra lo que haya encima de MainActivity: si el gasto venia de la cuenta rapida, esa
     * pantalla tambien se cierra y no queda la tentacion de guardarlo dos veces.
     */
    private void goToExpenseList() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /** Si el formulario cambio desde que se abrio, se pregunta antes de salir y perder lo escrito. */
    private void confirmExitIfChanged() {
        if (this.initialSnapshot == null || this.initialSnapshot.equals(takeSnapshot())) {
            finish();
            return;
        }
        confirm(getString(R.string.dlgDiscardTitle), getString(R.string.dlgDiscardMessage),
                R.string.btnDiscard, this::finish);
    }

    private String takeSnapshot() {
        return this.etDescription.getText().toString() + '|' + this.etAmount.getText().toString() + '|'
                + this.spPayer.getSelectedItemPosition() + '|' + this.spSplitType.getSelectedItemPosition()
                + '|' + this.participantAdapter.snapshot();
    }

    /** 264000 y no 264000.00: stripTrailingZeros quita los centavos cuando son cero. */
    private static String toPlainAmount(long cents) {
        return Money.ofCents(cents).toBigDecimal().stripTrailingZeros().toPlainString();
    }

    //metodo para capturar la data del activity y hacer validaciones
    private void getData() {
        String description = this.etDescription.getText().toString().trim();
        Money amount = Money.of(this.etAmount.getText().toString());

        User payer = (User) this.spPayer.getSelectedItem();
        if (payer == null) {
            throw new IllegalArgumentException(getString(R.string.msgNeedTwoMembers));
        }

        SplitType splitType = SplitType.fromPosition(this.spSplitType.getSelectedItemPosition());

        List<String> participantIds = this.participantAdapter.getSelectedUserIds();
        if (participantIds.isEmpty()) {
            throw new IllegalArgumentException(getString(R.string.msgNeedOneParticipant));
        }
        Map<String, BigDecimal> values = this.participantAdapter.getTypedValues();

        this.expense = new Expense(this.groupId, payer.getId(), description, amount, splitType);
        if (isEditing()) {
            //mismo id: se actualiza el gasto existente en vez de crear otro
            this.expense.setId(this.editingExpenseId);
        }
        this.splitRequest = new SplitRequest(amount, participantIds, values);
    }

    @Override
    protected void initObjects() {
        this.etDescription = findViewById(R.id.etDescription);
        this.etAmount = findViewById(R.id.etAmount);
        this.spPayer = findViewById(R.id.spPayer);
        this.spSplitType = findViewById(R.id.spSplitType);
        this.rvParticipants = findViewById(R.id.rvParticipants);
        this.btnSaveExpense = findViewById(R.id.btnSaveExpense);

        this.groupId = getServiceLocator().getSessionManager().getCurrentGroupId();
        this.editingExpenseId = getIntent().getStringExtra(EXTRA_EXPENSE_ID);
        this.expenseRepository = getServiceLocator().getExpenseRepository();
        this.userRepository = getServiceLocator().getUserRepository();

        if (isEditing()) {
            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle(R.string.tvTitleEditExpense);
            this.btnSaveExpense.setText(R.string.btnSaveChanges);
        }

        //Spinner de pagadores: se apoya en el toString() de User, que devuelve el nombre
        this.payerAdapter = new ArrayAdapter<>(this, R.layout.item_spinner);
        this.payerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        this.spPayer.setAdapter(this.payerAdapter);

        //Spinner de tipos de division: el orden del arreglo coincide con el del enum SplitType
        ArrayAdapter<CharSequence> splitTypeAdapter = ArrayAdapter.createFromResource(
                this, R.array.splitTypes, R.layout.item_spinner);
        splitTypeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        this.spSplitType.setAdapter(splitTypeAdapter);

        this.participantAdapter = new ParticipantAdapter();
        this.rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        this.rvParticipants.setAdapter(this.participantAdapter);
    }
}
