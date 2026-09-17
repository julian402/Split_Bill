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
 * En la entrega que agrega la API se sumara aqui un ejecutor network() para las peticiones HTTP.
 */
public class AppExecutors {

    private static final int IO_THREADS = 3;

    private final ExecutorService io;
    private final Executor mainThread;

    public AppExecutors() {
        this.io = Executors.newFixedThreadPool(IO_THREADS);
        this.mainThread = new MainThreadExecutor();
    }

    /** Hilos para leer y escribir en la base de datos. */
    public Executor io() {
        return this.io;
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
