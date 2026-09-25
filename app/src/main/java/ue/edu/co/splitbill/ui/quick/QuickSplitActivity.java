package ue.edu.co.splitbill.ui.quick;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategyFactory;
import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.model.QuickSplitRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.SimpleTextWatcher;
import ue.edu.co.splitbill.ui.adapter.QuickShareAdapter;
import ue.edu.co.splitbill.ui.expense.AddExpenseActivity;
import ue.edu.co.splitbill.ui.scan.ScanReceiptActivity;

/**
 * Cuenta rapida: dividir una cuenta al momento, sin registrar integrantes ni quien pago.
 *
 * Es el caso de la cena entre amigos, donde todavia nadie ha puesto la plata y lo unico que se
 * necesita saber es cuanto le toca a cada uno. Calcular no escribe nada en la base de datos. Despues
 * de calcular, "Guardar cuenta" ofrece tres caminos:
 * - En un grupo: se escoge un grupo con al menos tantos integrantes como personas tiene la cuenta, y
 *   se abre el formulario de gastos con el total y las personas de la cuenta, para marcar quien pago
 *   y quienes participaron (deben ser las mismas personas).
 * - En cuentas rapidas: se guarda sin grupo, con el nombre de cada persona y lo que le toco, y se
 *   sincroniza con el servidor.
 * - Salir sin guardar.
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
    private Button btnSaveQuickSplit;
    private ImageButton btnSavedQuickSplits;
    private RecyclerView rvQuickShares;

    private QuickShareAdapter quickShareAdapter;
    private QuickSplitRepository quickSplitRepository;
    private GroupRepository groupRepository;

    private Money totalWithTip;

    /** El ultimo reparto calculado, en el orden de la lista: es el que se guarda. */
    private List<Share> lastShares;

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

    /** Ya se esta en la cuenta rapida: "Cuenta rapida" en el menu del + no abre otra. */
    @Override
    protected void openQuickSplit(View view) {
        this.etQuickTotal.requestFocus();
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
        this.btnSaveQuickSplit.setOnClickListener(this::showSaveOptions);
        this.btnSavedQuickSplits.setOnClickListener(this::openSavedQuickSplits);
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
            this.lastShares = shares;
            this.btnSaveQuickSplit.setVisibility(View.VISIBLE);
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

    /** "Guardar cuenta": hoja de abajo con los tres caminos (grupo, cuentas rapidas, salir). */
    private void showSaveOptions(View view) {
        if (!hasCalculation()) {
            showToast(R.string.msgCalculateFirst);
            return;
        }
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.sheet_save_quick_split, null);
        TextView tvSummary = content.findViewById(R.id.tvSaveQuickSummary);
        tvSummary.setText(getString(R.string.tvSaveQuickSummary, this.totalWithTip.format(), peopleText()));
        content.findViewById(R.id.optionSaveInGroup).setOnClickListener(option -> {
            sheet.dismiss();
            pickGroupDB();
        });
        content.findViewById(R.id.optionSaveQuickSplit).setOnClickListener(option -> {
            sheet.dismiss();
            askQuickSplitName();
        });
        content.findViewById(R.id.btnDiscardQuickSplit).setOnClickListener(option -> {
            sheet.dismiss();
            finish();
        });
        sheet.setContentView(content);
        sheet.show();
    }

    private boolean hasCalculation() {
        return this.quickShareAdapter.hasResults() && this.totalWithTip != null && this.lastShares != null;
    }

    /** "4 personas". */
    private String peopleText() {
        int people = this.quickShareAdapter.getPeopleCount();
        return getResources().getQuantityString(R.plurals.tvPersonCount, people, people);
    }

    // ------------------------------------------------------------------ en un grupo

    private void pickGroupDB() {
        this.groupRepository.getGroups(new UiCallback<List<GroupListItem>>() {
            @Override
            protected void onData(List<GroupListItem> data) {
                showGroupPicker(data);
            }
        });
    }

    /**
     * Solo sirve un grupo que tenga a todas las personas de la cuenta: si la cuenta es entre 5 y el
     * grupo tiene 3 integrantes, no hay a quien asignarle las otras dos partes.
     */
    private void showGroupPicker(final List<GroupListItem> groups) {
        int needed = Math.max(this.quickShareAdapter.getPeopleCount(), MIN_MEMBERS);
        boolean anyFits = false;
        String[] names = new String[groups.size()];
        for (int i = 0; i < groups.size(); i++) {
            names[i] = groups.get(i).getName();
            anyFits = anyFits || groups.get(i).getMemberCount() >= needed;
        }
        if (!anyFits) {
            showToast(getString(R.string.msgNoGroupFitsQuick,
                    getResources().getQuantityString(R.plurals.tvGroupMembers, needed, needed)));
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dlgPickGroupTitle)
                .setItems(names, (dialog, which) -> chooseGroup(groups.get(which)))
                .setNegativeButton(R.string.btnCancel, null)
                .show();
    }

    private void chooseGroup(GroupListItem group) {
        if (group.getMemberCount() < MIN_MEMBERS) {
            showToast(getString(R.string.msgGroupNeedsMembers, group.getName()));
            return;
        }
        int people = this.quickShareAdapter.getPeopleCount();
        if (group.getMemberCount() < people) {
            showToast(getString(R.string.msgGroupTooSmallForQuick, peopleText(), group.getName(),
                    getResources().getQuantityString(R.plurals.tvGroupMembers, group.getMemberCount(),
                            group.getMemberCount())));
            return;
        }
        this.groupRepository.switchGroup(group.getGroupId());
        openExpenseForm();
    }

    /**
     * Abre el formulario de gastos con el total (propina incluida), la descripcion y las personas de
     * la cuenta: sus nombres, lo que le toco a cada una y como se dividio. Alli se marca quien pago y
     * quienes participaron, que es lo que la cuenta rapida no sabe.
     */
    private void openExpenseForm() {
        long[] amounts = new long[this.lastShares.size()];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = this.lastShares.get(i).getAmount().getCents();
        }
        Intent intent = new Intent(this, AddExpenseActivity.class);
        intent.putExtra(AddExpenseActivity.EXTRA_DESCRIPTION, this.scannedMerchant != null
                ? this.scannedMerchant : getString(R.string.quickExpenseDescription));
        intent.putExtra(AddExpenseActivity.EXTRA_AMOUNT_CENTS, this.totalWithTip.getCents());
        intent.putStringArrayListExtra(AddExpenseActivity.EXTRA_QUICK_NAMES,
                new ArrayList<>(this.quickShareAdapter.getDisplayNames()));
        intent.putExtra(AddExpenseActivity.EXTRA_QUICK_AMOUNTS, amounts);
        intent.putExtra(AddExpenseActivity.EXTRA_QUICK_SPLIT_TYPE,
                SplitType.fromPosition(this.spSplitType.getSelectedItemPosition()).name());
        startActivity(intent);
    }

    // ------------------------------------------------------------------ en cuentas rapidas

    /** Pide el nombre con el que se guarda; propone el del comercio escaneado o "Cuenta rapida". */
    private void askQuickSplitName() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_quick_split_name, null);
        final EditText etName = content.findViewById(R.id.etQuickSplitName);
        String suggested = this.scannedMerchant != null ? this.scannedMerchant
                : getString(R.string.quickExpenseDescription);
        etName.setText(suggested);
        etName.setSelection(suggested.length());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dlgQuickSplitNameTitle)
                .setView(content)
                .setNegativeButton(R.string.btnCancel, null)
                .setPositiveButton(R.string.btnSave,
                        (dialog, which) -> saveQuickSplitDB(etName.getText().toString().trim()))
                .show();
    }

    //metodo para guardar la cuenta rapida en la db
    private void saveQuickSplitDB(final String name) {
        if (!hasCalculation()) {
            showToast(R.string.msgCalculateFirst);
            return;
        }
        QuickSplit quickSplit;
        try {
            quickSplit = new QuickSplit(name, Money.of(this.etQuickTotal.getText().toString()),
                    readTipPercentage(), this.totalWithTip,
                    SplitType.fromPosition(this.spSplitType.getSelectedItemPosition()));
        } catch (IllegalArgumentException e) {
            showToast(e.getMessage());
            return;
        }
        showLoading();
        this.quickSplitRepository.saveQuickSplit(quickSplit, this.quickShareAdapter.getDisplayNames(),
                this.lastShares, new UiCallback<QuickSplit>() {
                    @Override
                    protected void onData(QuickSplit data) {
                        showToast(getString(R.string.msgQuickSplitSaved, data.getDescription()));
                        //se ve en su lista; la cuenta rapida ya cumplio y se cierra
                        startActivity(new Intent(QuickSplitActivity.this, SavedQuickSplitsActivity.class));
                        finish();
                    }
                });
    }

    private void openSavedQuickSplits(View view) {
        startActivity(new Intent(this, SavedQuickSplitsActivity.class));
    }

    private void clearResults() {
        this.quickShareAdapter.clearResults();
        hideSaveButton();
    }

    private void hideSaveButton() {
        this.lastShares = null;
        this.btnSaveQuickSplit.setVisibility(View.GONE);
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
        this.btnSaveQuickSplit = findViewById(R.id.btnSaveQuickSplit);
        this.btnSavedQuickSplits = findViewById(R.id.btnSavedQuickSplits);
        this.quickSplitRepository = getServiceLocator().getQuickSplitRepository();
        this.groupRepository = getServiceLocator().getGroupRepository();
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
