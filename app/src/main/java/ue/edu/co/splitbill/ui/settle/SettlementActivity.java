package ue.edu.co.splitbill.ui.settle;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.model.SettlementRepository;
import ue.edu.co.splitbill.model.SettlementResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.BalanceAdapter;
import ue.edu.co.splitbill.ui.adapter.TransferAdapter;

/**
 * Pantalla de liquidacion: el saldo de cada integrante y las transferencias minimas para saldarlo.
 *
 * Es la respuesta al problema que plantea el Acta de Constitucion. El resumen que se muestra arriba
 * de la lista compara cuantas transferencias propone el algoritmo contra cuantas harian falta si
 * cada integrante le transfiriera por separado a todos los demas.
 */
public class SettlementActivity extends BaseActivity {

    private TextView tvTransferSummary;
    private TextView tvAllSettled;
    private RecyclerView rvBalances;
    private RecyclerView rvTransfers;

    private BalanceAdapter balanceAdapter;
    private TransferAdapter transferAdapter;
    private SettlementRepository settlementRepository;
    private String groupId;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_settlement;
    }

    @Override
    protected void initListeners() {
        //Esta pantalla solo muestra informacion, no tiene botones
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
    }

    private void showSettlement(SettlementResult settlement) {
        this.balanceAdapter.setSettlement(settlement);
        this.transferAdapter.setSettlement(settlement);

        boolean settled = settlement.isSettled();
        this.tvAllSettled.setVisibility(settled ? View.VISIBLE : View.GONE);
        this.tvTransferSummary.setVisibility(settled ? View.GONE : View.VISIBLE);

        if (!settled) {
            this.tvTransferSummary.setText(getString(
                    R.string.tvTransferSummary,
                    settlement.getTransfers().size(),
                    settlement.getDirectTransferCount()));
        }
    }

    @Override
    protected void initObjects() {
        this.tvTransferSummary = findViewById(R.id.tvTransferSummary);
        this.tvAllSettled = findViewById(R.id.tvAllSettled);
        this.rvBalances = findViewById(R.id.rvBalances);
        this.rvTransfers = findViewById(R.id.rvTransfers);

        this.groupId = DatabaseContract.DEFAULT_GROUP_ID;
        this.settlementRepository = getServiceLocator().getSettlementRepository();

        this.balanceAdapter = new BalanceAdapter();
        this.rvBalances.setLayoutManager(new LinearLayoutManager(this));
        this.rvBalances.setAdapter(this.balanceAdapter);

        this.transferAdapter = new TransferAdapter();
        this.rvTransfers.setLayoutManager(new LinearLayoutManager(this));
        this.rvTransfers.setAdapter(this.transferAdapter);
    }
}
