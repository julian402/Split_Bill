package ue.edu.co.splitbill.permission;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ue.edu.co.splitbill.R;

/**
 * Pide los permisos peligrosos de la aplicacion (camara y contactos) desde cualquier pantalla.
 *
 * Es el mismo flujo del proyecto App_Permisos, sacado de la Activity a una clase reutilizable:
 * 1. Lista de permisos que necesita la funcion (CAMERA, CONTACTS).
 * 2. Revisar cuales siguen denegados (getDeniedPermissions, antes checkHasPermissions).
 * 3. Pedir solo esos (antes ActivityCompat.requestPermissions con un REQUEST_CODE).
 * 4. Revisar la respuesta del usuario (antes onRequestPermissionsResult).
 *
 * El paso 3 y 4 usan registerForActivityResult, que es la forma actual de Android: en vez de un
 * codigo numerico y un metodo que recibe todas las respuestas, cada pantalla registra un "launcher"
 * y recibe la respuesta en el metodo que ella misma indica.
 *
 * Ademas cubre dos casos que el ejemplo no tenia:
 * - Si el usuario ya nego el permiso una vez, Android recomienda explicar por que se necesita
 *   antes de volver a pedirlo.
 * - Si lo nego para siempre ("no volver a preguntar"), Android ya no muestra el dialogo: lo unico
 *   que queda es llevarlo a los ajustes de la aplicacion.
 *
 * Se debe crear en initObjects(), porque Android solo permite registrar el launcher mientras la
 * pantalla se esta creando.
 */
public class PermissionManager {

    private static final String TAG = "PermissionManager";

    /** Para escanear facturas. */
    public static final String[] CAMERA = {Manifest.permission.CAMERA};

    /** Para agregar integrantes desde la agenda del celular. */
    public static final String[] CONTACTS = {Manifest.permission.READ_CONTACTS};

    private final ComponentActivity activity;
    private final ActivityResultLauncher<String[]> launcher;

    /** Lo que se hace si el usuario concede el permiso que se esta pidiendo. */
    private Runnable onGranted;
    /** Explicacion de para que se usa el permiso que se esta pidiendo. */
    private int rationaleMessage;

    public PermissionManager(ComponentActivity activity) {
        this.activity = activity;
        this.launcher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionsResult);
    }

    /** true si todos los permisos estan concedidos. */
    public boolean hasPermissions(String... permissions) {
        return getDeniedPermissions(permissions).isEmpty();
    }

    /** Los permisos de la lista que el usuario todavia no ha concedido. */
    public List<String> getDeniedPermissions(String... permissions) {
        List<String> denied = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this.activity, permission) != PackageManager.PERMISSION_GRANTED) {
                denied.add(permission);
            }
        }
        return denied;
    }

    /**
     * Ejecuta onGranted si los permisos ya estan concedidos; si no, los pide y lo ejecuta cuando el
     * usuario los conceda.
     *
     * @param permissions      permisos que necesita la funcion, por ejemplo PermissionManager.CAMERA
     * @param rationaleMessage texto que explica para que se usan, por si hay que explicarlo
     * @param onGranted        lo que se hace con el permiso concedido
     */
    public void request(String[] permissions, int rationaleMessage, Runnable onGranted) {
        List<String> denied = getDeniedPermissions(permissions);
        if (denied.isEmpty()) {
            onGranted.run();
            return;
        }
        this.onGranted = onGranted;
        this.rationaleMessage = rationaleMessage;
        final String[] toRequest = denied.toArray(new String[0]);

        //ya lo nego una vez: primero se explica y solo si acepta se vuelve a pedir
        if (shouldExplain(toRequest)) {
            new MaterialAlertDialogBuilder(this.activity)
                    .setTitle(R.string.dlgPermissionTitle)
                    .setMessage(rationaleMessage)
                    .setNegativeButton(R.string.btnNotNow, null)
                    .setPositiveButton(R.string.btnContinue, (dialog, which) -> launch(toRequest))
                    .show();
            return;
        }
        launch(toRequest);
    }

    private void launch(String[] permissions) {
        Log.i(TAG, "Permisos solicitados: " + java.util.Arrays.toString(permissions));
        this.launcher.launch(permissions);
    }

    /** Respuesta del usuario al dialogo de Android. */
    private void onPermissionsResult(Map<String, Boolean> result) {
        List<String> denied = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            if (!entry.getValue()) {
                denied.add(entry.getKey());
            }
        }
        if (denied.isEmpty() && this.onGranted != null) {
            Runnable action = this.onGranted;
            this.onGranted = null;
            action.run();
            return;
        }
        //si despues de negarlo Android ya no recomienda explicarlo, es porque marco "no volver a preguntar"
        if (!denied.isEmpty() && !shouldExplain(denied.toArray(new String[0]))) {
            offerSettings();
        }
    }

    /**
     * El permiso quedo negado para siempre: el unico camino es que el usuario lo active a mano en
     * los ajustes de la aplicacion.
     */
    private void offerSettings() {
        new MaterialAlertDialogBuilder(this.activity)
                .setTitle(R.string.dlgPermissionDeniedTitle)
                .setMessage(this.activity.getString(this.rationaleMessage) + "\n\n"
                        + this.activity.getString(R.string.dlgPermissionDeniedMessage))
                .setNegativeButton(R.string.btnNotNow, null)
                .setPositiveButton(R.string.btnOpenSettings, (dialog, which) -> openAppSettings())
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", this.activity.getPackageName(), null));
        this.activity.startActivity(intent);
    }

    private boolean shouldExplain(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this.activity, permission)) {
                return true;
            }
        }
        return false;
    }
}
