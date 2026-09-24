package ue.edu.co.splitbill.ui.expense;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.ParticipantAdapter;
import ue.edu.co.splitbill.ui.group.MembersActivity;

/**
 * Pantalla para registrar un gasto.
 *
 * Es el punto donde se aprecia el polimorfismo del proyecto: esta clase no conoce ninguna de las
 * estrategias de division. Solo traduce lo que escogio el usuario en el Spinner a un SplitType y se
 * lo entrega al repositorio junto con los participantes y los valores digitados. Quien decide como
 * repartir es SplitStrategyFactory, asi que agregar una cuarta forma de dividir no obliga a tocar
 * esta pantalla: basta con agregar la clase nueva y el texto correspondiente en strings.xml.
 */
public class AddExpenseActivity extends BaseActivity {

    /** Descripcion con la que llega el formulario ya lleno, por ejemplo desde la cuenta rapida. */
    public static final String EXTRA_DESCRIPTION = "extraDescription";

    /** Monto en centavos con el que llega el formulario ya lleno. */
    public static final String EXTRA_AMOUNT_CENTS = "extraAmountCents";

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

    private Expense expense;
    private SplitRequest splitRequest;

    /** Estado del adaptador guardado antes de girar el celular, pendiente de restaurar. */
    private Bundle pendingParticipantState;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_add_expense;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Se guarda para restaurarlo cuando termine de cargar la lista de integrantes
        this.pendingParticipantState = savedInstanceState;
        if (savedInstanceState == null) {
            prefillFromIntent();
        }
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
            //stripTrailingZeros evita que un monto sin centavos llegue escrito como 264000.00
            this.etAmount.setText(Money.ofCents(amountCents)
                    .toBigDecimal()
                    .stripTrailingZeros()
                    .toPlainString());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.participantAdapter.saveState(outState);
    }

    @Override
    protected void initListeners() {
        this.btnSaveExpense.setOnClickListener(this::addExpenseDB);
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
                if (data.isEmpty()) {
                    //Sin integrantes no hay entre quienes repartir: se lleva al usuario a crearlos
                    showToast(R.string.msgNeedTwoMembers);
                    startActivity(new Intent(AddExpenseActivity.this, MembersActivity.class));
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
                }
            }
        });
    }

    //metodo para insertar en la db
    private void addExpenseDB(View view) {
        try {
            getData();
        } catch (IllegalArgumentException e) {
            //Errores de digitacion: se avisan de una vez, sin ir a la base de datos
            showToast(e.getMessage());
            return;
        }

        showLoading();
        this.expenseRepository.insertExpense(this.expense, this.splitRequest, new UiCallback<Expense>() {
            @Override
            protected void onData(Expense data) {
                showToast(R.string.msgExpenseSaved);
                finish();
            }
        });
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
        this.expenseRepository = getServiceLocator().getExpenseRepository();
        this.userRepository = getServiceLocator().getUserRepository();

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
