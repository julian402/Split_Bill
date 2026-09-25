package ue.edu.co.splitbill.ui.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.Message;
import ue.edu.co.splitbill.model.ChatRepository;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.adapter.MessageAdapter;

/**
 * Chat del grupo actual.
 *
 * Al abrirse muestra lo que hay en el celular y, mientras esta abierta, le pide al servidor cada
 * pocos segundos los mensajes nuevos. Lo que se escribe se guarda primero en el celular ("Enviando…")
 * y sale solo, aunque en ese momento no haya conexion. No hay notificaciones: los mensajes de los
 * demas se ven al entrar al chat.
 */
public class ChatActivity extends BaseActivity implements SyncListener {

    /** Cada cuanto se preguntan los mensajes nuevos mientras el chat esta abierto. */
    private static final long POLL_INTERVAL_MS = 5_000L;

    private TextView tvChatSubtitle;
    private TextView tvEmptyChat;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private View btnSend;

    private MessageAdapter messageAdapter;
    private ChatRepository chatRepository;
    private SyncManager syncManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = this::refreshMessagesAPI;
    private int shownCount;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_chat;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        //la barra de escribir sube con el teclado: se toma el alto del teclado ademas de las barras
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
    }

    @Override
    protected void initListeners() {
        this.btnSend.setOnClickListener(this::sendMessageDB);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroupNameDB();
        listMessagesDB();
        this.syncManager.addListener(this);
        this.handler.post(this.poll);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.handler.removeCallbacks(this.poll);
        this.syncManager.removeListener(this);
    }

    private void loadGroupNameDB() {
        getServiceLocator().getGroupRepository().getCurrentGroup(new UiCallback<Group>() {
            @Override
            protected void onData(Group data) {
                tvChatSubtitle.setText(data.getName());
            }
        });
    }

    private void listMessagesDB() {
        this.chatRepository.getMessages(new UiCallback<List<Message>>() {
            @Override
            protected void onData(List<Message> data) {
                showMessages(data);
            }
        });
    }

    /** Trae lo nuevo del servidor y vuelve a programarse. Sin conexion, no molesta: lo intenta luego. */
    private void refreshMessagesAPI() {
        this.chatRepository.refreshMessages(new UiCallback<List<Message>>() {
            @Override
            protected void onData(List<Message> data) {
                showMessages(data);
                scheduleNextPoll();
            }

            @Override
            public void onError(String message) {
                scheduleNextPoll();
            }
        });
    }

    private void scheduleNextPoll() {
        if (isAlive()) {
            this.handler.removeCallbacks(this.poll);
            this.handler.postDelayed(this.poll, POLL_INTERVAL_MS);
        }
    }

    /** Si llegaron mensajes nuevos, baja hasta el ultimo. */
    private void showMessages(List<Message> messages) {
        this.messageAdapter.setMessages(messages);
        this.tvEmptyChat.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
        if (messages.size() != this.shownCount && !messages.isEmpty()) {
            this.rvMessages.scrollToPosition(messages.size() - 1);
        }
        this.shownCount = messages.size();
    }

    //metodo para insertar el mensaje en la db; el SyncManager lo envia
    private void sendMessageDB(View view) {
        String text = this.etMessage.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        this.chatRepository.sendMessage(text, new UiCallback<Message>() {
            @Override
            protected void onData(Message data) {
                etMessage.setText("");
                listMessagesDB();
            }
        });
    }

    /** Al terminar de enviar, el mensaje deja de decir "Enviando…". */
    @Override
    public void onSyncStarted() {
        //no se muestra nada
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
        listMessagesDB();
    }

    @Override
    protected void initObjects() {
        this.tvChatSubtitle = findViewById(R.id.tvChatSubtitle);
        this.tvEmptyChat = findViewById(R.id.tvEmptyChat);
        this.rvMessages = findViewById(R.id.rvMessages);
        this.etMessage = findViewById(R.id.etMessage);
        this.btnSend = findViewById(R.id.btnSend);

        this.chatRepository = getServiceLocator().getChatRepository();
        this.syncManager = getServiceLocator().getSyncManager();

        this.messageAdapter = new MessageAdapter(this.chatRepository.getCurrentUserId());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        this.rvMessages.setLayoutManager(layoutManager);
        this.rvMessages.setAdapter(this.messageAdapter);
    }
}
