package ue.edu.co.splitbill.ui.settle;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.domain.Balance;
import ue.edu.co.splitbill.domain.Money;
import ue.edu.co.splitbill.domain.Transfer;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.model.ExpenseRepository;
import ue.edu.co.splitbill.model.SettlementRepository;
import ue.edu.co.splitbill.model.SettlementResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.BalanceAdapter;
import ue.edu.co.splitbill.ui.adapter.TransferAdapter;

/**
 * Pantalla de liquidacion: el saldo de cada integrante y las transferencias minimas para saldarlo.
 *
 * Es la respuesta al problema que plantea el Acta de Constitucion. La tarjeta de arriba compara
 * cuantas transferencias propone el algoritmo contra cuantas harian falta si cada participante le
 * devolviera su parte a quien pago, gasto por gasto.
 *
 * Desde el rediseno cada transferencia se puede compartir (por WhatsApp, por ejemplo) y marcar como
 * pagada. Un pago se guarda como un gasto PAYMENT (ver SettlementRepository.markPaid), asi que al
 * recargar, quien pago y quien recibio quedan en cero.
 */
public class SettlementActivity extends BaseActivity implements TransferAdapter.OnTransferActionListener {

    private TextView tvTransferSummary;
    private TextView tvAllSettled;
    private TextView tvSettlementHint;
    private TextView tvTransferCount;
    private View sectionTransfers;
    private ImageView ivHeroDecoIcon;
    private RecyclerView rvBalances;
    private RecyclerView rvTransfers;
    private TextView tvSummaryTotal;
    private TextView tvSummarySettled;
    private TextView tvSummaryTransfers;
    private Button btnMarkAllPaid;

    private BalanceAdapter balanceAdapter;
    private TransferAdapter transferAdapter;
    private SettlementRepository settlementRepository;
    private ExpenseRepository expenseRepository;
    private SettlementResult settlement;
    private String groupId;
    private String groupName;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_settlement;
    }

    @Override
    protected void initListeners() {
        this.btnMarkAllPaid.setOnClickListener(this::confirmMarkAllPaid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettlementDB();
    }

    private void loadSettlementDB() {
        showLoading();
        this.settlementRepository.getSettlement(this.groupId, new UiCallback<SettlementResult>() {
            @Override
            protected void onData(SettlementResult data) {
                showSettlement(data);
            }
        });
        this.expenseRepository.getTotalExpenses(this.groupId, new UiCallback<Money>() {
            @Override
            protected void onData(Money data) {
                tvSummaryTotal.setText(data.format());
            }
        });
    }

    private void showSettlement(SettlementResult settlement) {
        this.settlement = settlement;
        this.balanceAdapter.setSettlement(settlement);
        this.transferAdapter.setSettlement(settlement);

        int transfers = settlement.getTransfers().size();
        boolean settled = settlement.isSettled();
        this.tvAllSettled.setVisibility(settled ? View.VISIBLE : View.GONE);
        this.tvTransferSummary.setVisibility(settled ? View.GONE : View.VISIBLE);
        this.sectionTransfers.setVisibility(settled ? View.GONE : View.VISIBLE);
        this.btnMarkAllPaid.setVisibility(settled ? View.GONE : View.VISIBLE);
        this.tvSettlementHint.setText(settled ? R.string.tvSettledHint : R.string.tvSettlementHint);
        if (!settled) {
            this.tvTransferSummary.setText(getResources().getQuantityString(R.plurals.tvTransferSummary,
                    transfers, transfers, settlement.getDirectTransferCount()));
            this.tvTransferCount.setText(getResources().getQuantityString(R.plurals.tvTransferCount,
                    transfers, transfers));
        }

        int settledPeople = 0;
        for (Balance balance : settlement.getBalances()) {
            if (balance.isSettled()) {
                settledPeople++;
            }
        }
        this.tvSummarySettled.setText(getString(R.string.tvSettledOf, settledPeople, settlement.getBalances().size()));
        this.tvSummaryTransfers.setText(getResources().getQuantityString(R.plurals.tvTransfersTotal,
                transfers, transfers));
    }

    /** El texto se arma aqui y cada quien escoge por donde mandarlo (WhatsApp, correo, SMS). */
    @Override
    public void onShareTransfer(Transfer transfer) {
        String text = getString(R.string.shareTransferText,
                this.settlement.getUserName(transfer.getFromUserId()),
                this.groupName == null ? "" : this.groupName,
                transfer.getAmount().format(),
                this.settlement.getUserName(transfer.getToUserId()));
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(send, getString(R.string.shareChooserTitle)));
    }

    /** Un pago cambia los saldos de todo el grupo: primero se confirma. */
    @Override
    public void onMarkPaid(final Transfer transfer) {
        confirm(getString(R.string.dlgMarkPaidTitle),
                getString(R.string.dlgMarkPaidMessage,
                        this.settlement.getUserName(transfer.getFromUserId()),
                        transfer.getAmount().format(),
                        this.settlement.getUserName(transfer.getToUserId())),
                R.string.btnRegisterPayment,
                () -> markPaidDB(Collections.singletonList(transfer)));
    }

    private void confirmMarkAllPaid(View view) {
        final List<Transfer> transfers = this.settlement.getTransfers();
        confirm(getString(R.string.dlgMarkAllPaidTitle),
                getResources().getQuantityString(R.plurals.dlgMarkAllPaidMessage, transfers.size(), transfers.size()),
                R.string.btnRegisterPayment,
                () -> markPaidDB(transfers));
    }

    private void markPaidDB(List<Transfer> transfers) {
        showLoading();
        this.btnMarkAllPaid.setEnabled(false);
        this.settlementRepository.markPaid(this.groupId, transfers, new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                btnMarkAllPaid.setEnabled(true);
                showToast(getResources().getQuantityString(R.plurals.msgPaymentsRegistered, data, data));
                loadSettlementDB();
            }

            @Override
            public void onError(String message) {
                super.onError(message);
                btnMarkAllPaid.setEnabled(true);
            }
        });
    }

    private void loadGroupNameDB() {
        getServiceLocator().getGroupRepository().getCurrentGroup(new UiCallback<Group>() {
            @Override
            protected void onData(Group data) {
                groupName = data.getName();
            }
        });
    }

    @Override
    protected void initObjects() {
        this.tvTransferSummary = findViewById(R.id.tvTransferSummary);
        this.tvAllSettled = findViewById(R.id.tvAllSettled);
        this.tvSettlementHint = findViewById(R.id.tvSettlementHint);
        this.tvTransferCount = findViewById(R.id.tvTransferCount);
        this.sectionTransfers = findViewById(R.id.sectionTransfers);
        this.ivHeroDecoIcon = findViewById(R.id.ivHeroDecoIcon);
        this.rvBalances = findViewById(R.id.rvBalances);
        this.rvTransfers = findViewById(R.id.rvTransfers);
        this.tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        this.tvSummarySettled = findViewById(R.id.tvSummarySettled);
        this.tvSummaryTransfers = findViewById(R.id.tvSummaryTransfers);
        this.btnMarkAllPaid = findViewById(R.id.btnMarkAllPaid);

        this.groupId = getServiceLocator().getSessionManager().getCurrentGroupId();
        this.settlementRepository = getServiceLocator().getSettlementRepository();
        this.expenseRepository = getServiceLocator().getExpenseRepository();

        this.balanceAdapter = new BalanceAdapter();
        this.rvBalances.setAdapter(this.balanceAdapter);
        this.transferAdapter = new TransferAdapter(this);
        this.rvTransfers.setAdapter(this.transferAdapter);

        this.ivHeroDecoIcon.setImageResource(R.drawable.ic_swap);
        loadGroupNameDB();
    }
}
