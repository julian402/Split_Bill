package ue.edu.co.splitbill.model;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import retrofit2.Response;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.Message;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.network.ApiClient;
import ue.edu.co.splitbill.network.ApiMapper;
import ue.edu.co.splitbill.network.ApiService;
import ue.edu.co.splitbill.network.dto.MessageDto;
import ue.edu.co.splitbill.session.SessionManager;
import ue.edu.co.splitbill.sync.SyncManager;

/**
 * Chat del grupo actual.
 *
 * Escribir funciona igual que todo lo demas: el mensaje se guarda en el celular (PENDING_CREATE) y el
 * SyncManager lo envia, con o sin conexion en ese momento. Leer lo que escribieron los demas es
 * distinto: la pantalla del chat, mientras esta abierta, le pide al servidor cada pocos segundos lo
 * que llego desde la ultima vez (refreshMessages). Asi el chat se siente casi en vivo sin cargar la
 * sincronizacion general con los mensajes de todos los grupos.
 */
public class ChatRepository extends BaseRepository {

    private static final String TAG = "ChatRepository";

    /** Llave aparte para la hora del ultimo pedido de mensajes de cada grupo. */
    private static final String LAST_PULL_PREFIX = "chat:";

    /** Mismo margen que en la sincronizacion: traer un mensaje dos veces no hace dano. */
    private static final long PULL_SAFETY_MARGIN_MS = 60_000L;

    private final ApiService api;
    private final SessionManager sessionManager;
    private final SyncManager syncManager;

    public ChatRepository(SplitBillDatabase database, AppExecutors executors, ApiService api,
                          SessionManager sessionManager, SyncManager syncManager) {
        super(database, executors);
        this.api = api;
        this.sessionManager = sessionManager;
        this.syncManager = syncManager;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    public String getCurrentUserId() {
        return this.sessionManager.getUserId();
    }

    /** Lo que hay en el celular: se muestra de inmediato, aun sin conexion. */
    public void getMessages(DataCallback<List<Message>> callback) {
        final String groupId = this.sessionManager.getCurrentGroupId();
        runAsync(new Callable<List<Message>>() {
            @Override
            public List<Message> call() {
                return database.messageDao().findByGroup(groupId);
            }
        }, callback);
    }

    /** El ultimo mensaje del grupo actual, o null si el chat esta vacio. */
    public void getLastMessage(DataCallback<Message> callback) {
        final String groupId = this.sessionManager.getCurrentGroupId();
        runAsync(new Callable<Message>() {
            @Override
            public Message call() {
                return database.messageDao().findLast(groupId);
            }
        }, callback);
    }

    /** Guarda el mensaje en el celular y pide enviarlo. */
    public void sendMessage(final String text, DataCallback<Message> callback) {
        final Message message = new Message(this.sessionManager.getCurrentGroupId(),
                this.sessionManager.getUserId(), this.sessionManager.getUserNames(), text);
        runAsync(new Callable<Message>() {
            @Override
            public Message call() {
                message.validar();
                database.messageDao().insert(message);
                syncManager.notifyLocalChange();
                return message;
            }
        }, callback);
    }

    /**
     * Trae del servidor lo que llego desde la ultima vez y devuelve el chat completo. La primera vez
     * trae todo. Un mensaje propio que todavia no se envio no se toca. Si el grupo aun no esta en el
     * servidor (se creo sin conexion), solo devuelve lo local.
     */
    public void refreshMessages(DataCallback<List<Message>> callback) {
        final String groupId = this.sessionManager.getCurrentGroupId();
        runNetwork(new Callable<List<Message>>() {
            @Override
            public List<Message> call() throws IOException {
                Group group = database.groupDao().findById(groupId);
                if (group != null && group.getSyncStatus() == SyncStatus.SYNCED) {
                    pull(groupId);
                }
                return database.messageDao().findByGroup(groupId);
            }
        }, callback);
    }

    private void pull(final String groupId) throws IOException {
        String key = LAST_PULL_PREFIX + groupId;
        long lastPull = this.sessionManager.getLastPull(key);
        String since = lastPull > 0 ? Instant.ofEpochMilli(lastPull).toString() : null;
        Response<List<MessageDto>> response = ApiClient.executeForResponse(this.api.getMessages(groupId, since));
        final List<MessageDto> messages = response.body() == null ? new ArrayList<MessageDto>() : response.body();
        this.database.runInTransaction(new Runnable() {
            @Override
            public void run() {
                for (MessageDto dto : messages) {
                    Message local = database.messageDao().findById(dto.getId());
                    if (local == null || !local.isPending()) {
                        database.messageDao().upsert(ApiMapper.toEntity(dto));
                    }
                }
            }
        });
        Date serverDate = response.headers().getDate("Date");
        if (serverDate != null) {
            this.sessionManager.setLastPull(key, serverDate.getTime() - PULL_SAFETY_MARGIN_MS);
        }
    }
}
