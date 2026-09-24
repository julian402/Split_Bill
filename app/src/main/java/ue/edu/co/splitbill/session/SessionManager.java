package ue.edu.co.splitbill.session;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Quien inicio sesion y en que grupo esta trabajando.
 *
 * - El token va al TokenStore, cifrado.
 * - El resto (id, nombre, email, grupo actual) no es secreto y va en SharedPreferences normales.
 *
 * Una sesion "vencida" pierde el token pero conserva el id del usuario: asi, si la misma persona
 * vuelve a iniciar sesion, sus cambios pendientes de sincronizar siguen ahi; si entra otra persona,
 * SessionRepository borra los datos locales antes de continuar.
 *
 * Los metodos son synchronized porque se llaman desde el hilo principal y desde el hilo de red
 * (AuthInterceptor).
 */
public class SessionManager {

    private static final String PREFS_NAME = "splitbill_session";
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_USER_NAMES = "user_names";
    private static final String PREF_USER_EMAIL = "user_email";
    private static final String PREF_GROUP_ID = "group_id";

    private final SharedPreferences preferences;
    private final TokenStore tokenStore;

    /** Copia en memoria del token para no descifrarlo en cada peticion. */
    private String token;
    private boolean tokenLoaded;

    public SessionManager(Context context, TokenStore tokenStore) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.tokenStore = tokenStore;
    }

    public synchronized void startSession(String token, String userId, String names, String email) {
        this.tokenStore.save(token);
        this.token = token;
        this.tokenLoaded = true;
        this.preferences.edit()
                .putString(PREF_USER_ID, userId)
                .putString(PREF_USER_NAMES, names)
                .putString(PREF_USER_EMAIL, email)
                .apply();
    }

    public synchronized String getToken() {
        if (!this.tokenLoaded) {
            this.token = this.tokenStore.read();
            this.tokenLoaded = true;
        }
        return this.token;
    }

    /** Hay sesion si hay token y un grupo elegido para trabajar. */
    public synchronized boolean isLoggedIn() {
        return getToken() != null && getCurrentGroupId() != null;
    }

    /** El servidor rechazo el token: se descarta, pero se recuerda de quien era. */
    public synchronized void expireSession() {
        this.tokenStore.clear();
        this.token = null;
        this.tokenLoaded = true;
    }

    /** Cierre de sesion completo: no queda nada. */
    public synchronized void clear() {
        expireSession();
        this.preferences.edit().clear().apply();
    }

    public synchronized String getUserId() {
        return this.preferences.getString(PREF_USER_ID, null);
    }

    public synchronized String getUserNames() {
        return this.preferences.getString(PREF_USER_NAMES, null);
    }

    public synchronized String getUserEmail() {
        return this.preferences.getString(PREF_USER_EMAIL, null);
    }

    public synchronized String getCurrentGroupId() {
        return this.preferences.getString(PREF_GROUP_ID, null);
    }

    public synchronized void setCurrentGroupId(String groupId) {
        this.preferences.edit().putString(PREF_GROUP_ID, groupId).apply();
    }
}
