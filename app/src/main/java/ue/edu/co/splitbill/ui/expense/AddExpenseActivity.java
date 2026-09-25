package ue.edu.co.splitbill.ui.expense;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.dao.ShareListItem;
import ue.edu.co.splitbill.domain.ExpenseCategory;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Share;
import ue.edu.co.splitbill.domain.SplitType;
import ue.edu.co.splitbill.domain.split.SplitRequest;
import ue.edu.co.splitbill.domain.split.SplitStrategyFactory;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.model.ExpenseDetail;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.GroupRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.Categories;
import ue.edu.co.splitbill.ui.DateText;
import ue.edu.co.splitbill.ui.SimpleTextWatcher;
import ue.edu.co.splitbill.ui.adapter.ParticipantAdapter;
import ue.edu.co.splitbill.ui.group.GroupDetailActivity;
import ue.edu.co.splitbill.ui.group.GroupFormActivity;
import ue.edu.co.splitbill.ui.scan.ScanReceiptActivity;

/**
 * Pantalla para registrar un gasto o editar uno que ya existe.
 *
 * Es el punto donde se aprecia el polimorfismo del proyecto: esta clase no conoce ninguna de las
 * estrategias de division. Solo traduce lo que escogio el usuario en el Spinner a un SplitType y se
 * lo entrega al repositorio junto con los participantes y los valores digitados. Quien decide como
 * repartir es SplitStrategyFactory, asi que agregar una cuarta forma de dividir no obliga a tocar
 * esta pantalla: basta con agregar la clase nueva y el texto correspondiente en strings.xml.
 *
 * La "Division estimada" usa la misma fabrica mientras el usuario escribe, para mostrar cuanto le
 * toca a cada uno antes de guardar.
 *
 * Un gasto nuevo queda en el grupo actual, pero la tarjeta "Grupo" deja escoger otro: al cambiarlo se
 * cargan sus integrantes y se conserva lo que ya se habia escrito (monto, descripcion, fecha).
 *
 * Si viene de una cuenta rapida (EXTRA_QUICK_NAMES), el gasto debe quedar entre la misma cantidad de
 * personas que la cuenta: se marcan de una vez los integrantes cuyo nombre coincide (o todos, si el
 * grupo tiene justo esa cantidad), no se puede escoger un grupo con menos integrantes y no se guarda
 * si los participantes marcados no son tantos como las personas de la cuenta.
 *
 * Si llega EXTRA_EXPENSE_ID, la pantalla abre en modo edicion: el mismo formulario, lleno con el gasto.
 * Un gasto que ya existe no cambia de grupo, asi que en ese modo no aparece la tarjeta "Grupo".
 * En los dos modos, si el usuario intenta salir con cambios sin guardar, se le pregunta antes.
 */
public class AddExpenseActivity extends BaseActivity implements ParticipantAdapter.OnParticipantsChangedListener {

    /** Descripcion con la que llega el formulario ya lleno, por ejemplo desde la cuenta rapida. */
    public static final String EXTRA_DESCRIPTION = "extraDescription";

    /** Monto en centavos con el que llega el formulario ya lleno. */
    public static final String EXTRA_AMOUNT_CENTS = "extraAmountCents";

    /** Nombres de las personas de la cuenta rapida, en orden. */
    public static final String EXTRA_QUICK_NAMES = "extraQuickNames";

    /** Lo que le toco a cada persona de la cuenta rapida, en centavos y en el mismo orden. */
    public static final String EXTRA_QUICK_AMOUNTS = "extraQuickAmounts";

    /** Como se dividio la cuenta rapida (nombre de SplitType). */
    public static final String EXTRA_QUICK_SPLIT_TYPE = "extraQuickSplitType";

    /** Id del gasto a editar. Si no llega, la pantalla registra un gasto nuevo. */
    public static final String EXTRA_EXPENSE_ID = "extraEditExpenseId";

    private static final String KEY_INITIAL_SNAPSHOT = "initialSnapshot";
    private static final String KEY_DATE = "expenseDate";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    /** En partes iguales los participantes van en dos columnas; con valores, uno por fila. */
    private static final int GRID_COLUMNS = 2;

    private TextView tvFormTitle;
    private TextView tvFormSubtitle;
    private View rowGroup;
    private TextView tvExpenseGroup;
    private EditText etDescription;
    private ImageButton btnClearDescription;
    private EditText etAmount;
    private ImageButton btnScanReceipt;
    private View rowPayer;
    private View rowSplitType;
    private Spinner spPayer;
    private Spinner spSplitType;
    private TextView tvSelectAll;
    private TextView tvFromQuickSplit;
    private RecyclerView rvParticipants;
    private View rowDate;
    private TextView tvExpenseDate;
    private View rowCategory;
    private ImageView ivCategory;
    private Spinner spCategory;
    private TextView tvEstimate;
    private TextView tvParticipantCount;
    private Button btnSaveExpense;

    private ParticipantAdapter participantAdapter;
    private GridLayoutManager participantLayout;
    private ArrayAdapter<User> payerAdapter;
    private ExpenseRepository expenseRepository;
    private UserRepository userRepository;
    private GroupRepository groupRepository;
    private String groupId;
    private String editingExpenseId;
    private Date expenseDate;

    /** Personas de la cuenta rapida de la que viene el gasto; vacia si no viene de una. */
    private List<String> quickNames = new ArrayList<>();
    private long[] quickAmounts;
    private SplitType quickSplitType;

    /** Hay que marcar a las personas de la cuenta rapida cuando lleguen los integrantes. */
    private boolean applyQuickSelection;

    private Expense expense;
    private SplitRequest splitRequest;

    /** Estado del adaptador guardado antes de girar el celular, pendiente de restaurar. */
    private Bundle pendingParticipantState;

    /** Como estaba el formulario al abrirse; si al salir es distinto, hay cambios sin guardar. */
    private String initialSnapshot;

    /** Abre el escaner de facturas y recibe el total que el usuario confirmo. */
    private ActivityResultLauncher<Intent> scanLauncher;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_add_expense;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        //Se guarda para restaurarlo cuando termine de cargar la lista de integrantes
        this.pendingParticipantState = savedInstanceState;
        if (savedInstanceState != null) {
            this.initialSnapshot = savedInstanceState.getString(KEY_INITIAL_SNAPSHOT);
            setExpenseDate(new Date(savedInstanceState.getLong(KEY_DATE, System.currentTimeMillis())));
        } else if (!isEditing()) {
            prefillFromIntent();
            this.applyQuickSelection = isFromQuickSplit();
        }
    }

    private boolean isEditing() {
        return this.editingExpenseId != null;
    }

    private boolean isFromQuickSplit() {
        return !this.quickNames.isEmpty();
    }

    /** Ya se esta registrando un gasto: "Gasto" en el menu del + no abre otro formulario. */
    @Override
    protected void openAddExpense(View view) {
        this.etAmount.requestFocus();
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
        outState.putLong(KEY_DATE, this.expenseDate.getTime());
    }

    @Override
    protected void initListeners() {
        this.btnSaveExpense.setOnClickListener(this::saveExpenseDB);
        this.rowGroup.setOnClickListener(this::pickGroupDB);
        this.btnScanReceipt.setOnClickListener(this::scanReceipt);
        this.btnClearDescription.setOnClickListener(this::clearDescription);
        this.rowPayer.setOnClickListener(view -> this.spPayer.performClick());
        this.rowSplitType.setOnClickListener(view -> this.spSplitType.performClick());
        this.rowCategory.setOnClickListener(view -> this.spCategory.performClick());
        this.rowDate.setOnClickListener(this::pickDate);
        this.tvSelectAll.setOnClickListener(view -> this.participantAdapter.toggleAll());
        this.etAmount.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                showEstimate();
            }
        });
        this.spSplitType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Al cambiar el tipo de division aparecen o desaparecen los campos de valor
                SplitType splitType = SplitType.fromPosition(position);
                participantLayout.setSpanCount(splitType == SplitType.EQUAL ? GRID_COLUMNS : 1);
                participantAdapter.setSplitType(splitType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //no se usa
            }
        });
        this.spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ivCategory.setImageResource(Categories.getIcon(ExpenseCategory.fromPosition(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //no se usa
            }
        });

        //Tanto el boton atras del celular como la flecha del encabezado pasan por aqui
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmExitIfChanged();
            }
        });
    }

    private void clearDescription(View view) {
        this.etDescription.setText("");
        this.etDescription.requestFocus();
    }

    private void scanReceipt(View view) {
        this.scanLauncher.launch(new Intent(this, ScanReceiptActivity.class));
    }

    /**
     * El escaner solo llena el monto (y la descripcion si estaba vacia, con el nombre del comercio):
     * no toca el pagador, los participantes ni la division.
     */
    private void onReceiptScanned(ActivityResult result) {
        if (result.getResultCode() != RESULT_OK || result.getData() == null) {
            return;
        }
        long cents = result.getData().getLongExtra(ScanReceiptActivity.EXTRA_AMOUNT_CENTS, 0L);
        this.etAmount.setText(toPlainAmount(cents));
        String merchant = result.getData().getStringExtra(ScanReceiptActivity.EXTRA_MERCHANT);
        if (merchant != null && this.etDescription.getText().toString().trim().isEmpty()) {
            this.etDescription.setText(merchant);
        }
        showToast(R.string.msgAmountScanned);
    }

    /** Calendario de Material. Solo deja escoger hasta hoy: un gasto no se registra a futuro. */
    private void pickDate(View view) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.dlgPickDateTitle)
                .setSelection(toUtcDay(this.expenseDate))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointBackward.now())
                        .build())
                .build();
        picker.addOnPositiveButtonClickListener(this::onDatePicked);
        picker.show(getSupportFragmentManager(), "expenseDate");
    }

    /**
     * El calendario trabaja con la medianoche del dia en UTC; se pasa a ese dia en la hora del
     * celular, conservando la hora del gasto para que el orden de la lista siga teniendo sentido.
     */
    private void onDatePicked(Long utcMillis) {
        Calendar picked = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        picked.setTimeInMillis(utcMillis);
        Calendar date = Calendar.getInstance();
        date.setTime(this.expenseDate);
        date.set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH));
        setExpenseDate(date.getTime());
    }

    private static long toUtcDay(Date date) {
        Calendar local = Calendar.getInstance();
        local.setTime(date);
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        return utc.getTimeInMillis();
    }

    private void setExpenseDate(Date date) {
        this.expenseDate = date;
        this.tvExpenseDate.setText(DateText.longDay(this, date));
    }

    @Override
    protected void onResume() {
        super.onResume();
        listMembersDB();
    }

    /** Deja claro en que grupo queda el gasto: en la tarjeta "Grupo" o, al editar, "En Viaje a Cartagena". */
    private void loadGroupNameDB() {
        this.groupRepository.getCurrentGroup(new UiCallback<Group>() {
            @Override
            protected void onData(Group data) {
                tvExpenseGroup.setText(data.getName());
                tvFormSubtitle.setText(getString(R.string.tvAddExpenseSubtitle, data.getName()));
            }
        });
    }

    /** Lista los grupos para escoger en cual queda el gasto; el actual aparece marcado. */
    private void pickGroupDB(View view) {
        this.groupRepository.getGroups(new UiCallback<List<GroupListItem>>() {
            @Override
            protected void onData(List<GroupListItem> data) {
                showGroupPicker(data);
            }
        });
    }

    private void showGroupPicker(final List<GroupListItem> groups) {
        String[] names = new String[groups.size()];
        int checked = -1;
        for (int i = 0; i < groups.size(); i++) {
            names[i] = groups.get(i).getName();
            if (groups.get(i).getGroupId().equals(this.groupId)) {
                checked = i;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dlgPickGroupTitle)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    dialog.dismiss();
                    changeGroup(groups.get(which));
                })
                .setNegativeButton(R.string.btnCancel, null)
                .show();
    }

    /**
     * Pasa el gasto a otro grupo: se vuelve el grupo actual (para que al guardar se abra ese grupo) y
     * se cargan sus integrantes. Monto, descripcion, fecha y categoria no se tocan; el pagador y los
     * participantes si, porque son de otro grupo. Un grupo sin con quien repartir no se puede escoger.
     */
    private void changeGroup(GroupListItem group) {
        if (group.getGroupId().equals(this.groupId)) {
            return;
        }
        if (group.getMemberCount() < MIN_MEMBERS) {
            showToast(getString(R.string.msgGroupNeedsMembers, group.getName()));
            return;
        }
        if (group.getMemberCount() < this.quickNames.size()) {
            showToast(getString(R.string.msgGroupTooSmallForQuick, quickPeopleText(), group.getName(),
                    getResources().getQuantityString(R.plurals.tvGroupMembers, group.getMemberCount(),
                            group.getMemberCount())));
            return;
        }
        this.groupRepository.switchGroup(group.getGroupId());
        this.applyQuickSelection = isFromQuickSplit();
        this.groupId = group.getGroupId();
        this.tvExpenseGroup.setText(group.getName());
        this.tvFormSubtitle.setText(getString(R.string.tvAddExpenseSubtitle, group.getName()));
        listMembersDB();
    }

    private void listMembersDB() {
        showLoading();
        this.userRepository.getActiveUsers(new UiCallback<List<User>>() {
            @Override
            protected void onData(List<User> data) {
                if (data.size() < MIN_MEMBERS && !isEditing()) {
                    //No hay entre quienes repartir: se lleva al usuario a agregar integrantes. Se le pasa
                    //lo que traia este formulario (por ejemplo, el total de la cuenta rapida) para que,
                    //al volver con el boton "Continuar con el gasto", no tenga que escribirlo otra vez.
                    showToast(R.string.msgNeedTwoMembers);
                    Intent intent = GroupFormActivity.editIntent(AddExpenseActivity.this);
                    intent.putExtras(getIntent());
                    intent.putExtra(GroupFormActivity.EXTRA_CONTINUE_TO_EXPENSE, true);
                    startActivity(intent);
                    finish();
                    return;
                }
                payerAdapter.clear();
                payerAdapter.addAll(data);
                participantAdapter.setParticipants(data);
                if (applyQuickSelection) {
                    selectQuickSplitPeople(data);
                    applyQuickSelection = false;
                }

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

    /**
     * Marca a las personas de la cuenta rapida. Si el grupo tiene justo esa cantidad de integrantes,
     * son todos; si tiene mas, los que se llaman igual (sin importar mayusculas) y el resto lo marca
     * el usuario. Si la cuenta no se dividio en partes iguales, se pasa como montos exactos: cada
     * integrante reconocido queda con lo que le toco en la cuenta.
     */
    private void selectQuickSplitPeople(List<User> members) {
        Map<String, Integer> positionByName = new LinkedHashMap<>();
        for (int i = 0; i < this.quickNames.size(); i++) {
            positionByName.put(normalize(this.quickNames.get(i)), i);
        }
        boolean everybody = members.size() == this.quickNames.size();
        Set<String> ids = new LinkedHashSet<>();
        Map<String, String> values = new LinkedHashMap<>();
        for (User member : members) {
            Integer position = positionByName.remove(normalize(member.getNames()));
            if (position != null || everybody) {
                ids.add(member.getId());
            }
            if (position != null && this.quickAmounts != null && position < this.quickAmounts.length) {
                values.put(member.getId(), toPlainAmount(this.quickAmounts[position]));
            }
        }
        SplitType splitType = this.quickSplitType == SplitType.EQUAL ? SplitType.EQUAL : SplitType.EXACT;
        this.spSplitType.setSelection(splitType.getPosition());
        this.participantLayout.setSpanCount(splitType == SplitType.EQUAL ? GRID_COLUMNS : 1);
        this.participantAdapter.setSplitType(splitType);
        this.participantAdapter.preload(ids, splitType == SplitType.EQUAL ? new LinkedHashMap<String, String>() : values);
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    /** "4 personas". */
    private String quickPeopleText() {
        return getResources().getQuantityString(R.plurals.tvPersonCount, this.quickNames.size(),
                this.quickNames.size());
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
        this.participantLayout.setSpanCount(current.getSplitType() == SplitType.EQUAL ? GRID_COLUMNS : 1);
        this.participantAdapter.setSplitType(current.getSplitType());
        if (!current.isPayment()) {
            this.spCategory.setSelection(current.getCategory().getPosition());
        }
        setExpenseDate(current.getDate());

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

    @Override
    public void onParticipantsChanged() {
        showEstimate();
    }

    /**
     * "Division estimada": en partes iguales, cuanto paga cada uno (con la misma estrategia que se
     * usara al guardar); con montos o porcentajes, cuanto se lleva asignado frente al total.
     */
    private void showEstimate() {
        int selected = this.participantAdapter.getSelectedUserIds().size();
        this.tvParticipantCount.setText(getResources().getQuantityString(R.plurals.tvParticipantCount,
                selected, selected));
        Money amount;
        try {
            amount = Money.of(this.etAmount.getText().toString());
        } catch (IllegalArgumentException e) {
            amount = Money.ZERO;
        }
        SplitType splitType = this.participantAdapter.getSplitType();
        if (splitType == SplitType.PERCENTAGE) {
            this.tvEstimate.setText(getString(R.string.tvEstimatePercent,
                    this.participantAdapter.sumTypedValues().stripTrailingZeros().toPlainString()));
        } else if (!amount.isPositive()) {
            this.tvEstimate.setText(R.string.tvEstimateEmpty);
        } else if (selected == 0) {
            this.tvEstimate.setText(R.string.tvEstimateNobody);
        } else if (splitType == SplitType.EXACT) {
            String assigned;
            try {
                assigned = Money.of(this.participantAdapter.sumTypedValues()).format();
            } catch (IllegalArgumentException e) {
                //mas de dos decimales: todavia lo esta escribiendo
                assigned = this.participantAdapter.sumTypedValues().toPlainString();
            }
            this.tvEstimate.setText(getString(R.string.tvEstimateExact, assigned, amount.format()));
        } else {
            List<Share> shares = SplitStrategyFactory.create(SplitType.EQUAL)
                    .split(new SplitRequest(amount, this.participantAdapter.getSelectedUserIds()));
            //con centavos que no se dividen exacto, la primera parte es la mayor
            this.tvEstimate.setText(getString(R.string.tvEstimateEqual, shares.get(0).getAmount().format()));
        }
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
     * Despues de guardar se va al grupo, donde el gasto nuevo aparece de primero. CLEAR_TOP cierra lo
     * que haya encima del grupo: si el gasto venia de la cuenta rapida, esa pantalla tambien se cierra
     * y no queda la tentacion de guardarlo dos veces.
     */
    private void goToExpenseList() {
        Intent intent = new Intent(this, GroupDetailActivity.class);
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
        return this.groupId + '|' + this.etDescription.getText().toString() + '|' + this.etAmount.getText().toString() + '|'
                + this.spPayer.getSelectedItemPosition() + '|' + this.spSplitType.getSelectedItemPosition()
                + '|' + this.spCategory.getSelectedItemPosition() + '|' + DateText.daysBetween(this.expenseDate, new Date())
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
        //la cuenta rapida era entre cierta cantidad de personas: el gasto debe quedar entre las mismas
        if (isFromQuickSplit() && participantIds.size() != this.quickNames.size()) {
            throw new IllegalArgumentException(getString(R.string.msgQuickPeopleMismatch, quickPeopleText(),
                    getResources().getQuantityString(R.plurals.tvPersonCount, participantIds.size(),
                            participantIds.size())));
        }
        Map<String, BigDecimal> values = this.participantAdapter.getTypedValues();

        this.expense = new Expense(this.groupId, payer.getId(), description, amount, splitType);
        this.expense.setCategory(ExpenseCategory.fromPosition(this.spCategory.getSelectedItemPosition()));
        this.expense.setDate(this.expenseDate);
        if (isEditing()) {
            //mismo id: se actualiza el gasto existente en vez de crear otro
            this.expense.setId(this.editingExpenseId);
        }
        this.splitRequest = new SplitRequest(amount, participantIds, values);
    }

    @Override
    protected void initObjects() {
        this.tvFormTitle = findViewById(R.id.tvFormTitle);
        this.tvFormSubtitle = findViewById(R.id.tvFormSubtitle);
        this.rowGroup = findViewById(R.id.rowGroup);
        this.tvExpenseGroup = findViewById(R.id.tvExpenseGroup);
        this.etDescription = findViewById(R.id.etDescription);
        this.btnClearDescription = findViewById(R.id.btnClearDescription);
        this.etAmount = findViewById(R.id.etAmount);
        this.btnScanReceipt = findViewById(R.id.btnScanReceipt);
        this.rowPayer = findViewById(R.id.rowPayer);
        this.rowSplitType = findViewById(R.id.rowSplitType);
        this.spPayer = findViewById(R.id.spPayer);
        this.spSplitType = findViewById(R.id.spSplitType);
        this.tvSelectAll = findViewById(R.id.tvSelectAll);
        this.tvFromQuickSplit = findViewById(R.id.tvFromQuickSplit);
        this.rvParticipants = findViewById(R.id.rvParticipants);
        this.rowDate = findViewById(R.id.rowDate);
        this.tvExpenseDate = findViewById(R.id.tvExpenseDate);
        this.rowCategory = findViewById(R.id.rowCategory);
        this.ivCategory = findViewById(R.id.ivCategory);
        this.spCategory = findViewById(R.id.spCategory);
        this.tvEstimate = findViewById(R.id.tvEstimate);
        this.tvParticipantCount = findViewById(R.id.tvParticipantCount);
        this.btnSaveExpense = findViewById(R.id.btnSaveExpense);

        this.groupId = getServiceLocator().getSessionManager().getCurrentGroupId();
        this.editingExpenseId = getIntent().getStringExtra(EXTRA_EXPENSE_ID);
        ArrayList<String> names = getIntent().getStringArrayListExtra(EXTRA_QUICK_NAMES);
        if (names != null && !isEditing()) {
            this.quickNames = names;
            this.quickAmounts = getIntent().getLongArrayExtra(EXTRA_QUICK_AMOUNTS);
            String splitTypeName = getIntent().getStringExtra(EXTRA_QUICK_SPLIT_TYPE);
            this.quickSplitType = splitTypeName == null ? SplitType.EQUAL : SplitType.valueOf(splitTypeName);
            this.tvFromQuickSplit.setText(getString(R.string.tvFromQuickSplit, quickPeopleText()));
            this.tvFromQuickSplit.setVisibility(View.VISIBLE);
        }
        this.expenseRepository = getServiceLocator().getExpenseRepository();
        this.userRepository = getServiceLocator().getUserRepository();
        this.groupRepository = getServiceLocator().getGroupRepository();
        this.scanLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                this::onReceiptScanned);

        if (isEditing()) {
            this.tvFormTitle.setText(R.string.tvTitleEditExpense);
            this.btnSaveExpense.setText(R.string.btnSaveChanges);
            this.rowGroup.setVisibility(View.GONE);
        } else {
            //el grupo ya se ve en su tarjeta
            this.tvFormSubtitle.setVisibility(View.GONE);
        }

        //Spinner de pagadores: se apoya en el toString() de User, que devuelve el nombre
        this.payerAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_plain);
        this.payerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        this.spPayer.setAdapter(this.payerAdapter);

        //Spinner de tipos de division: el orden del arreglo coincide con el del enum SplitType
        ArrayAdapter<CharSequence> splitTypeAdapter = ArrayAdapter.createFromResource(
                this, R.array.splitTypes, R.layout.item_spinner_plain);
        splitTypeAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        this.spSplitType.setAdapter(splitTypeAdapter);

        //Spinner de categorias: el orden coincide con ExpenseCategory.selectable(); arranca en "Otro"
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(
                this, R.array.expenseCategories, R.layout.item_spinner_plain);
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        this.spCategory.setAdapter(categoryAdapter);
        this.spCategory.setSelection(ExpenseCategory.OTHER.getPosition());

        this.participantAdapter = new ParticipantAdapter();
        this.participantAdapter.setOnParticipantsChangedListener(this);
        this.participantLayout = new GridLayoutManager(this, GRID_COLUMNS);
        this.rvParticipants.setLayoutManager(this.participantLayout);
        this.rvParticipants.setAdapter(this.participantAdapter);

        setExpenseDate(new Date());
        loadGroupNameDB();
    }
}
