package ue.edu.co.splitbill.di;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Los hilos de la aplicacion, en un solo lugar.
 *
 * Android no permite tocar la base de datos desde el hilo principal: si se hace, la aplicacion se
 * congela y el sistema la mata. Tampoco permite tocar las vistas desde otro hilo. Por eso hay dos
 * ejecutores: uno para el trabajo de disco y otro que devuelve el resultado al hilo principal.
 *
 * Los repositorios reciben esta clase por constructor, asi que ninguna Activity crea hilos.
 * Desde la entrega 3 hay un tercer ejecutor, network(), para las peticiones HTTP al backend.
 */
public class AppExecutors {

    private static final int IO_THREADS = 3;

    private static final int NETWORK_THREADS = 2;

    private final ExecutorService io;
    private final ExecutorService network;
    private final Executor mainThread;

    public AppExecutors() {
        this.io = Executors.newFixedThreadPool(IO_THREADS);
        this.network = Executors.newFixedThreadPool(NETWORK_THREADS);
        this.mainThread = new MainThreadExecutor();
    }

    /** Hilos para leer y escribir en la base de datos. */
    public Executor io() {
        return this.io;
    }

    /**
     * Hilos para las peticiones HTTP. Van aparte de io() porque una peticion puede tardar segundos
     * esperando al servidor, y mientras tanto la lectura de la base de datos no debe quedar en cola.
     */
    public Executor network() {
        return this.network;
    }

    /** Hilo principal: el unico desde el que se pueden actualizar las vistas. */
    public Executor mainThread() {
        return this.mainThread;
    }

    /** Publica la tarea en la cola del hilo principal. */
    private static class MainThreadExecutor implements Executor {

        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            this.handler.post(command);
        }
    }
}
