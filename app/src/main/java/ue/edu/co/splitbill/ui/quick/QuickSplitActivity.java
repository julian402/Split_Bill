package ue.edu.co.splitbill.ui.quick;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategyFactory;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.SimpleTextWatcher;
import ue.edu.co.splitbill.ui.adapter.QuickShareAdapter;
import ue.edu.co.splitbill.ui.expense.AddExpenseActivity;
import ue.edu.co.splitbill.ui.scan.ScanReceiptActivity;

/**
 * Cuenta rapida: dividir una cuenta al momento, sin registrar integrantes ni quien pago.
 *
 * Es el caso de la cena entre amigos, donde todavia nadie ha puesto la plata y lo unico que se
 * necesita saber es cuanto le toca a cada uno. No escribe nada en la base de datos: es una
 * calculadora. Si despues resulta que si hay que registrar el gasto, el boton de guardar abre el
 * formulario normal con la descripcion y el total ya llenos.
 *
 * Toda la matematica es la misma del resto de la aplicacion: las tres SplitStrategy y la clase Money
 * se reutilizan tal cual. Esta pantalla no aporta una sola regla de negocio nueva, y esa es
 * precisamente la prueba de que el dominio quedo bien separado de la interfaz.
 */
public class QuickSplitActivity extends BaseActivity {

    private static final int MIN_PEOPLE = 1;
    private static final int MAX_PEOPLE = 50;
    private static final int DEFAULT_PEOPLE = 4;
    private static final String DEFAULT_TIP = "10";

    private ImageButton btnScanReceipt;
    private View rowSplitType;
    private EditText etQuickTotal;
    private EditText etTipPercentage;
    private TextView tvTotalWithTip;
    private TextView tvPeopleCount;
    private Button btnMinus;
    private Button btnPlus;
    private Spinner spSplitType;
    private Button btnCalculate;
    private Button btnSaveAsExpense;
    private RecyclerView rvQuickShares;

    private QuickShareAdapter quickShareAdapter;

    private Money totalWithTip;

    /** Nombre del comercio de la factura escaneada: se usa como descripcion al guardar el gasto. */
    private String scannedMerchant;

    /** Abre el escaner y recibe el total que el usuario confirmo. */
    private ActivityResultLauncher<Intent> scanLauncher;

    @Override
    protected int getNavItem() {
        return R.id.navHome;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_quick_split;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.quickShareAdapter.restoreState(savedInstanceState);
        }
        showPeopleCount();
        updateTotalWithTip();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.quickShareAdapter.saveState(outState);
    }

    @Override
    protected void initListeners() {
        this.btnMinus.setOnClickListener(this::removePerson);
        this.btnPlus.setOnClickListener(this::addPerson);
        this.btnCalculate.setOnClickListener(this::calculateSplit);
        this.btnSaveAsExpense.setOnClickListener(this::saveAsExpense);
        this.btnScanReceipt.setOnClickListener(this::scanReceipt);
        this.rowSplitType.setOnClickListener(view -> this.spSplitType.performClick());

        //El total con propina se recalcula mientras el usuario escribe
        SimpleTextWatcher recalcular = new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateTotalWithTip();
                clearResults();
            }
        };
        this.etQuickTotal.addTextChangedListener(recalcular);
        this.etTipPercentage.addTextChangedListener(recalcular);

        this.spSplitType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                quickShareAdapter.setSplitType(SplitType.fromPosition(position));
                hideSaveButton();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //no se usa
            }
        });
    }

    private void scanReceipt(View view) {
        this.scanLauncher.launch(new Intent(this, ScanReceiptActivity.class));
    }

    /** El total escaneado queda en el campo; la propina y el reparto se recalculan solos. */
    private void onReceiptScanned(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            return;
        }
        long cents = result.getData().getLongExtra(ScanReceiptActivity.EXTRA_AMOUNT_CENTS, 0L);
        this.scannedMerchant = result.getData().getStringExtra(ScanReceiptActivity.EXTRA_MERCHANT);
        this.etQuickTotal.setText(Money.ofCents(cents).toBigDecimal().stripTrailingZeros().toPlainString());
        showToast(R.string.msgAmountScanned);
    }

    private void addPerson(View view) {
        if (this.quickShareAdapter.getPeopleCount() >= MAX_PEOPLE) {
            showToast(getString(R.string.msgMaxPeople, MAX_PEOPLE));
            return;
        }
        this.quickShareAdapter.setPeopleCount(this.quickShareAdapter.getPeopleCount() + 1);
        showPeopleCount();
        hideSaveButton();
    }

    private void removePerson(View view) {
        if (this.quickShareAdapter.getPeopleCount() <= MIN_PEOPLE) {
            showToast(R.string.msgMinPeople);
            return;
        }
        this.quickShareAdapter.setPeopleCount(this.quickShareAdapter.getPeopleCount() - 1);
        showPeopleCount();
        hideSaveButton();
    }

    //metodo que aplica la estrategia y muestra el reparto
    private void calculateSplit(View view) {
        try {
            getData();

            SplitRequest request = new SplitRequest(
                    this.totalWithTip,
                    this.quickShareAdapter.getParticipantIds(),
                    this.quickShareAdapter.getTypedValues());

            //La pantalla no conoce las clases concretas: le pide la estrategia a la fabrica
            SplitType splitType = SplitType.fromPosition(this.spSplitType.getSelectedItemPosition());
            List<Share> shares = SplitStrategyFactory.create(splitType).split(request);

            this.quickShareAdapter.setResults(shares);
            this.btnSaveAsExpense.setVisibility(View.VISIBLE);
        } catch (IllegalArgumentException e) {
            clearResults();
            showToast(e.getMessage());
        }
    }

    //metodo para capturar la data del activity y hacer validaciones
    private void getData() {
        Money total = Money.of(this.etQuickTotal.getText().toString());
        this.totalWithTip = total.plusPercentage(readTipPercentage());
    }

    private BigDecimal readTipPercentage() {
        String typed = this.etTipPercentage.getText().toString().trim();
        if (typed.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(typed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propina no es un número válido");
        }
    }

    /** Muestra el total con propina mientras el usuario escribe, sin molestar con mensajes. */
    private void updateTotalWithTip() {
        try {
            Money total = Money.of(this.etQuickTotal.getText().toString());
            this.totalWithTip = total.plusPercentage(readTipPercentage());
            this.tvTotalWithTip.setText(getString(R.string.tvTotalWithTip, this.totalWithTip.format()));
        } catch (IllegalArgumentException e) {
            //Todavia no hay un monto valido: no se muestra nada
            this.totalWithTip = null;
            this.tvTotalWithTip.setText("");
        }
    }

    /**
     * Pasa la cuenta al formulario de gastos con la descripcion y el total ya llenos.
     *
     * Alli el usuario escoge quien pago y entre que integrantes registrados se reparte, que es
     * informacion que la cuenta rapida no tiene porque no pide nombres reales.
     */
    private void saveAsExpense(View view) {
        if (!this.quickShareAdapter.hasResults() || this.totalWithTip == null) {
            showToast(R.string.msgCalculateFirst);
            return;
        }
        Intent intent = new Intent(this, AddExpenseActivity.class);
        intent.putExtra(AddExpenseActivity.EXTRA_DESCRIPTION, this.scannedMerchant != null
                ? this.scannedMerchant : getString(R.string.quickExpenseDescription));
        intent.putExtra(AddExpenseActivity.EXTRA_AMOUNT_CENTS, this.totalWithTip.getCents());
        startActivity(intent);
    }

    private void clearResults() {
        this.quickShareAdapter.clearResults();
        hideSaveButton();
    }

    private void hideSaveButton() {
        this.btnSaveAsExpense.setVisibility(View.GONE);
    }

    private void showPeopleCount() {
        this.tvPeopleCount.setText(String.valueOf(this.quickShareAdapter.getPeopleCount()));
    }

    @Override
    protected void initObjects() {
        this.btnScanReceipt = findViewById(R.id.btnScanReceipt);
        this.rowSplitType = findViewById(R.id.rowSplitType);
        this.etQuickTotal = findViewById(R.id.etQuickTotal);
        this.etTipPercentage = findViewById(R.id.etTipPercentage);
        this.tvTotalWithTip = findViewById(R.id.tvTotalWithTip);
        this.tvPeopleCount = findViewById(R.id.tvPeopleCount);
        this.btnMinus = findViewById(R.id.btnMinus);
        this.btnPlus = findViewById(R.id.btnPlus);
        this.spSplitType = findViewById(R.id.spSplitType);
        this.btnCalculate = findViewById(R.id.btnCalculate);
        this.btnSaveAsExpense = findViewById(R.id.btnSaveAsExpense);
        this.rvQuickShares = findViewById(R.id.rvQuickShares);

        this.etTipPercentage.setText(DEFAULT_TIP);
        this.scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::onReceiptScanned);

        //Spinner de tipos de division: el mismo arreglo que usa la pantalla de gastos
        ArrayAdapter<CharSequence> splitTypeAdapter = ArrayAdapter.createFromResource(
                this, R.array.splitTypes, R.layout.item_spinner_plain);
        splitTypeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        this.spSplitType.setAdapter(splitTypeAdapter);

        this.quickShareAdapter = new QuickShareAdapter();
        this.quickShareAdapter.setPeopleCount(DEFAULT_PEOPLE);
        this.rvQuickShares.setLayoutManager(new LinearLayoutManager(this));
        this.rvQuickShares.setAdapter(this.quickShareAdapter);
    }
}
