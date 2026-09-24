package ue.edu.co.splitbill.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Programa el SyncWorker con WorkManager.
 *
 * Es un trabajo unico (KEEP): si ya hay uno esperando la red, no se crea otro; cuando corra subira
 * todos los cambios pendientes, no solo el ultimo.
 */
public class SyncScheduler {

    private static final String WORK_NAME = "splitbill-sync";
    private static final long BACKOFF_SECONDS = 30;

    private final Context context;

    public SyncScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    public void schedule() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(this.context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request);
    }
}
