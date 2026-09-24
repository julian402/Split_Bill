package ue.edu.co.splitbill.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * Como termino un ciclo de sincronizacion, para mostrarselo al usuario.
 */
public class SyncResult {

    public enum State {
        /** Todo lo pendiente se subio y se trajo lo nuevo del servidor. */
        SYNCED,
        /** No hubo conexion con el servidor: los cambios siguen guardados y se suben despues. */
        OFFLINE,
        /** El servidor rechazo el token: hay que volver a iniciar sesion. */
        SESSION_EXPIRED,
        /** El servidor fallo (5xx): se reintenta en el siguiente ciclo. */
        SERVER_ERROR
    }

    private final State state;
    private final int pendingChanges;
    private final List<String> rejectedMessages;

    public SyncResult(State state, int pendingChanges, List<String> rejectedMessages) {
        this.state = state;
        this.pendingChanges = pendingChanges;
        this.rejectedMessages = rejectedMessages == null ? new ArrayList<>() : rejectedMessages;
    }

    public State getState() {
        return this.state;
    }

    /** Cambios hechos en el celular que todavia no estan en el servidor. */
    public int getPendingChanges() {
        return this.pendingChanges;
    }

    /** Mensajes del servidor para los cambios que rechazo y que por eso se descartaron. */
    public List<String> getRejectedMessages() {
        return this.rejectedMessages;
    }

    public boolean isSynced() {
        return this.state == State.SYNCED;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SyncResult{");
        sb.append("state=").append(state);
        sb.append(", pendingChanges=").append(pendingChanges);
        sb.append(", rejected=").append(rejectedMessages.size());
        sb.append('}');
        return sb.toString();
    }
}
