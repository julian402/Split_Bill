package ue.edu.co.splitbill.sync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

/**
 * Vigila la conexion del celular.
 *
 * Android avisa por medio de un callback cada vez que aparece o se pierde la red. Cuando vuelve la
 * conexion, se ejecuta la accion recibida en start(), que es pedir una sincronizacion: asi lo que se
 * registro en modo avion se sube solo, sin que el usuario tenga que hacer nada.
 */
public class NetworkMonitor {

    private final ConnectivityManager connectivityManager;
    private volatile boolean online;

    public NetworkMonitor(Context context) {
        this.connectivityManager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /** @param onNetworkAvailable accion a ejecutar cada vez que el celular recupera la conexion */
    public void start(final Runnable onNetworkAvailable) {
        this.online = checkNow();
        this.connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                online = true;
                onNetworkAvailable.run();
            }

            @Override
            public void onLost(@NonNull Network network) {
                online = false;
            }
        });
    }

    public boolean isOnline() {
        return this.online;
    }

    private boolean checkNow() {
        Network network = this.connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = this.connectivityManager.getNetworkCapabilities(network);
        //se pide solo INTERNET y no VALIDATED: el backend de desarrollo esta en el mismo PC
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
