package ue.edu.co.splitbill.ui.profile;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import ue.edu.co.splitbill.BuildConfig;
import ue.edu.co.splitbill.R;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.model.SessionRepository;
import ue.edu.co.splitbill.model.UserRepository;
import ue.edu.co.splitbill.session.SessionManager;
import ue.edu.co.splitbill.sync.SyncListener;
import ue.edu.co.splitbill.sync.SyncManager;
import ue.edu.co.splitbill.sync.SyncResult;
import ue.edu.co.splitbill.ui.Avatar;
import ue.edu.co.splitbill.ui.BaseActivity;
import ue.edu.co.splitbill.ui.SyncStatusText;

/**
 * Pestana Perfil: nombre y telefono propios (se guardan en el servidor con PUT /api/users/me), el
 * estado de la sincronizacion y el cierre de sesion.
 */
public class ProfileActivity extends BaseActivity implements SyncListener {

    private TextView tvProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private EditText etProfileNames;
    private EditText etProfilePhone;
    private Button btnSaveProfile;
    private TextView tvSyncStatus;
    private Button btnSync;
    private Button btnLogout;
    private TextView tvAppVersion;

    private UserRepository userRepository;
    private SessionRepository sessionRepository;
    private SessionManager sessionManager;
    private SyncManager syncManager;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_profile;
    }

    @Override
    protected int getNavItem() {
        return R.id.navProfile;
    }

    @Override
    protected void initListeners() {
        this.btnSaveProfile.setOnClickListener(this::saveProfileAPI);
        this.btnSync.setOnClickListener(this::syncAPI);
        this.btnLogout.setOnClickListener(this::confirmLogout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.syncManager.addListener(this);
        this.syncManager.requestSync();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.syncManager.removeListener(this);
    }

    private void loadProfileDB() {
        showHeader(this.sessionManager.getUserNames());
        this.etProfileNames.setText(this.sessionManager.getUserNames());
        this.userRepository.getCurrentUser(new UiCallback<User>() {
            @Override
            protected void onData(User data) {
                if (data != null && data.getPhone() != null) {
                    etProfilePhone.setText(data.getPhone());
                }
            }
        });
    }

    private void showHeader(String names) {
        Avatar.bind(this.tvProfileAvatar, names);
        this.tvProfileAvatar.setText(Avatar.getInitials(names).substring(0, 1));
        this.tvProfileName.setText(names);
        this.tvProfileEmail.setText(this.sessionManager.getUserEmail());
    }

    private void saveProfileAPI(View view) {
        showLoading();
        this.btnSaveProfile.setEnabled(false);
        this.userRepository.updateProfile(this.etProfileNames.getText().toString(),
                this.etProfilePhone.getText().toString(), new UiCallback<User>() {
                    @Override
                    protected void onData(User data) {
                        btnSaveProfile.setEnabled(true);
                        showHeader(data.getNames());
                        showToast(R.string.msgProfileSaved);
                    }

                    @Override
                    public void onError(String message) {
                        super.onError(message);
                        btnSaveProfile.setEnabled(true);
                    }
                });
    }

    private void syncAPI(View view) {
        this.syncManager.requestSync();
    }

    @Override
    public void onSyncStarted() {
        this.tvSyncStatus.setText(R.string.tvSyncing);
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
        this.tvSyncStatus.setText(SyncStatusText.of(this, result));
    }

    /**
     * Antes de cerrar sesion se avisa cuantos cambios no alcanzaron a subirse, porque al cerrar se
     * borran los datos del celular.
     */
    private void confirmLogout(View view) {
        this.sessionRepository.countPendingChanges(new UiCallback<Integer>() {
            @Override
            protected void onData(Integer pending) {
                String message = getString(R.string.dlgLogoutMessage);
                if (pending > 0) {
                    message += "\n\n" + getResources().getQuantityString(R.plurals.dlgLogoutPending, pending, pending);
                }
                confirm(getString(R.string.dlgLogoutTitle), message, R.string.btnLogout, ProfileActivity.this::logout);
            }
        });
    }

    private void logout() {
        showLoading();
        this.sessionRepository.logout(new UiCallback<Boolean>() {
            @Override
            protected void onData(Boolean data) {
                goToLogin(false);
            }
        });
    }

    @Override
    protected void initObjects() {
        this.tvProfileAvatar = findViewById(R.id.tvProfileAvatar);
        this.tvProfileName = findViewById(R.id.tvProfileName);
        this.tvProfileEmail = findViewById(R.id.tvProfileEmail);
        this.etProfileNames = findViewById(R.id.etProfileNames);
        this.etProfilePhone = findViewById(R.id.etProfilePhone);
        this.btnSaveProfile = findViewById(R.id.btnSaveProfile);
        this.tvSyncStatus = findViewById(R.id.tvSyncStatus);
        this.btnSync = findViewById(R.id.btnSync);
        this.btnLogout = findViewById(R.id.btnLogout);
        this.tvAppVersion = findViewById(R.id.tvAppVersion);

        this.userRepository = getServiceLocator().getUserRepository();
        this.sessionRepository = getServiceLocator().getSessionRepository();
        this.sessionManager = getServiceLocator().getSessionManager();
        this.syncManager = getServiceLocator().getSyncManager();

        this.tvAppVersion.setText(getString(R.string.tvAppVersion, BuildConfig.VERSION_NAME));
        this.tvSyncStatus.setText(getServiceLocator().getNetworkMonitor().isOnline()
                ? R.string.tvSyncing : R.string.tvOffline);
        loadProfileDB();
    }
}
