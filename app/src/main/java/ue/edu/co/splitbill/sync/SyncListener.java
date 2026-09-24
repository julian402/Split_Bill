package ue.edu.co.splitbill.sync;

/**
 * Lo implementa la pantalla que quiere enterarse cuando termina una sincronizacion (por ejemplo,
 * para recargar la lista de gastos). Siempre se llama en el hilo principal.
 */
public interface SyncListener {

    void onSyncStarted();

    void onSyncFinished(SyncResult result);
}
