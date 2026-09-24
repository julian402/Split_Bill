package ue.edu.co.splitbill.ui;

import android.content.Context;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.sync.SyncResult;

/**
 * El texto del estado de la sincronizacion ("Sincronizado", "Sin conexion · 2 cambios pendientes"),
 * igual en el inicio, la actividad y el perfil.
 */
public final class SyncStatusText {

    private SyncStatusText() {
        //Clase de utilidades: no se instancia
    }

    public static String of(Context context, SyncResult result) {
        int pending = result.getPendingChanges();
        switch (result.getState()) {
            case SYNCED:
                return context.getString(R.string.tvSynced);
            case SERVER_ERROR:
                return context.getResources().getQuantityString(R.plurals.tvServerErrorPending, pending, pending);
            default:
                return pending == 0 ? context.getString(R.string.tvOffline)
                        : context.getResources().getQuantityString(R.plurals.tvOfflinePending, pending, pending);
        }
    }
}
