package ue.edu.co.splitbill.ui.quick;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.dao.QuickSplitListItem;
import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.QuickSplitShare;
import ue.edu.co.splitbill.model.QuickSplitDetail;
import ue.edu.co.splitbill.model.QuickSplitRepository;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.DateText;
import ue.edu.co.splitbill.ui.adapter.SavedQuickSplitAdapter;

/**
 * Cuentas rapidas guardadas, sin grupo. Tocar una muestra lo que le toco a cada persona, y desde ahi
 * se puede compartir por WhatsApp (u otra app) o eliminar.
 *
 * Todo sale de la base de datos del celular; al abrir la pantalla se sincroniza, por si se guardaron
 * cuentas desde otro celular.
 */
public class SavedQuickSplitsActivity extends BaseActivity
        implements SavedQuickSplitAdapter.OnQuickSplitClickListener, SyncListener {

    private TextView tvEmptyQuickSplits;
    private RecyclerView rvQuickSplits;

    private SavedQuickSplitAdapter adapter;
    private QuickSplitRepository quickSplitRepository;
    private SyncManager syncManager;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_saved_quick_splits;
    }

    @Override
    protected void initListeners() {
        //solo el boton atras del encabezado, que maneja BaseActivity
    }

    @Override
    protected void onResume() {
        super.onResume();
        listQuickSplitsDB();
        this.syncManager.addListener(this);
        this.syncManager.requestSync();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.syncManager.removeListener(this);
    }

    @Override
    public void onSyncStarted() {
        //la lista ya se ve; no hace falta avisar
    }

    @Override
    public void onSyncFinished(SyncResult result) {
        if (!isAlive()) {
            return;
        }
        if (result.getState() == SyncResult.State.SESSION_EXPIRED) {
            goToLogin(true);
            return;
        }
        listQuickSplitsDB();
    }

    private void listQuickSplitsDB() {
        this.quickSplitRepository.getSavedQuickSplits(new UiCallback<List<QuickSplitListItem>>() {
            @Override
            protected void onData(List<QuickSplitListItem> data) {
                adapter.setQuickSplits(data);
                tvEmptyQuickSplits.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onQuickSplitClick(QuickSplitListItem quickSplit) {
        this.quickSplitRepository.getQuickSplitDetail(quickSplit.getQuickSplitId(), new UiCallback<QuickSplitDetail>() {
            @Override
            protected void onData(QuickSplitDetail data) {
                showDetail(data);
            }
        });
    }

    /** Lo que le toco a cada persona, con la cuenta y la propina arriba. */
    private void showDetail(final QuickSplitDetail detail) {
        QuickSplit quickSplit = detail.getQuickSplit();
        StringBuilder message = new StringBuilder();
        message.append(DateText.longDay(this, quickSplit.getDate())).append('\n');
        message.append(getString(R.string.tvQuickSplitTipLine, quickSplit.getSubtotal().format(),
                quickSplit.getTipPercent(), quickSplit.getTotal().format())).append("\n\n");
        message.append(shareLines(detail));
        new MaterialAlertDialogBuilder(this)
                .setTitle(quickSplit.getDescription())
                .setMessage(message.toString())
                .setPositiveButton(R.string.btnClose, null)
                .setNeutralButton(R.string.btnShare, (dialog, which) -> share(detail))
                .setNegativeButton(R.string.btnDelete, (dialog, which) -> confirmDelete(quickSplit))
                .show();
    }

    private String shareLines(QuickSplitDetail detail) {
        StringBuilder lines = new StringBuilder();
        for (QuickSplitShare share : detail.getShares()) {
            if (lines.length() > 0) {
                lines.append('\n');
            }
            lines.append(getString(R.string.tvQuickSplitShareLine, share.getName(), share.getAmount().format()));
        }
        return lines.toString();
    }

    /** El mismo texto del detalle, para mandarlo por WhatsApp, correo o la app que se escoja. */
    private void share(QuickSplitDetail detail) {
        QuickSplit quickSplit = detail.getQuickSplit();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, getString(R.string.shareQuickSplitText, quickSplit.getDescription(),
                quickSplit.getTotal().format(), shareLines(detail)));
        startActivity(Intent.createChooser(send, getString(R.string.shareChooserTitle)));
    }

    private void confirmDelete(final QuickSplit quickSplit) {
        confirm(getString(R.string.dlgDeleteQuickSplitTitle),
                getString(R.string.dlgDeleteQuickSplitMessage, quickSplit.getDescription()),
                R.string.btnDelete, () -> deleteQuickSplitDB(quickSplit.getId()));
    }

    //metodo para eliminar (borrado logico) en la db
    private void deleteQuickSplitDB(String quickSplitId) {
        this.quickSplitRepository.deleteQuickSplit(quickSplitId, new UiCallback<Integer>() {
            @Override
            protected void onData(Integer data) {
                showToast(R.string.msgQuickSplitDeleted);
                listQuickSplitsDB();
            }
        });
    }

    @Override
    protected void initObjects() {
        this.tvEmptyQuickSplits = findViewById(R.id.tvEmptyQuickSplits);
        this.rvQuickSplits = findViewById(R.id.rvQuickSplits);

        this.quickSplitRepository = getServiceLocator().getQuickSplitRepository();
        this.syncManager = getServiceLocator().getSyncManager();

        this.adapter = new SavedQuickSplitAdapter(this);
        this.rvQuickSplits.setAdapter(this.adapter);
    }
}
