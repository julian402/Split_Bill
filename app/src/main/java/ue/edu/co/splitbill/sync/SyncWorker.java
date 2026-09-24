package ue.edu.co.splitbill.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import ue.edu.co.splitbill.SplitBillApplication;

/**
 * Sincronizacion que Android ejecuta por su cuenta, aunque la app este cerrada.
 *
 * Lo programa SyncScheduler cada vez que hay un cambio local, con la condicion "hay red". Si el cambio
 * se hizo en modo avion y el usuario cerro la app, Android lanza este trabajo apenas vuelve la conexion.
 * No tiene logica propia: usa el mismo SyncManager que la app abierta.
 */
public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SplitBillApplication application = (SplitBillApplication) getApplicationContext();
        SyncResult result = application.getServiceLocator().getSyncManager().syncNow();
        switch (result.getState()) {
            case SYNCED:
                return Result.success();
            case SESSION_EXPIRED:
                //sin sesion no hay nada que hacer hasta que la persona vuelva a entrar
                return Result.failure();
            default:
                //sin red o servidor caido: Android lo reintenta mas tarde, esperando cada vez un poco mas
                return Result.retry();
        }
    }
}
