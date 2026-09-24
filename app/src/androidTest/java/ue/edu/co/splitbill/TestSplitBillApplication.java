package ue.edu.co.splitbill;

import android.util.Log;

import androidx.room.Room;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.idling.CountingIdlingResource;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.mockwebserver.MockWebServer;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.di.ServiceLocator;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.ui.FakeBackend;
import ue.edu.co.splitbill.ui.MemoryTokenStore;

/**
 * La aplicacion armada para las pruebas de interfaz:
 * - Base de datos en memoria: las pruebas no borran los datos reales del emulador.
 * - Token en memoria: no se usa el Android Keystore.
 * - Un servidor falso (FakeBackend) en vez del backend: las pruebas no dependen de que este prendido.
 * - Hilos que Espresso sabe esperar: antes de revisar la pantalla, espera a que terminen las
 *   consultas a la base de datos y las peticiones al servidor.
 */
public class TestSplitBillApplication extends SplitBillApplication {

    private static final String TAG = "TestSplitBillApp";

    /** Contador de tareas en curso en los hilos de la app; Espresso espera a que llegue a cero. */
    private final CountingIdlingResource busy = new CountingIdlingResource("SplitBillExecutors");

    @Override
    protected ServiceLocator createServiceLocator() {
        SplitBillDatabase database = Room.inMemoryDatabaseBuilder(this, SplitBillDatabase.class).build();
        AppExecutors executors = new AppExecutors(
                tracked(Executors.newFixedThreadPool(3)), tracked(Executors.newFixedThreadPool(2)));
        IdlingRegistry.getInstance().register(this.busy);
        return new ServiceLocator(this, database, new MemoryTokenStore(), executors, startFakeBackend());
    }

    /**
     * MockWebServer abre un socket: se arranca fuera del hilo principal, como pide Android. Se usa la
     * IP 127.0.0.1 y no "localhost" para no depender de resolver nombres.
     */
    private String startFakeBackend() {
        final MockWebServer server = new MockWebServer();
        server.setDispatcher(new FakeBackend());
        Thread thread = new Thread(() -> {
            try {
                server.start(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), 0);
            } catch (IOException e) {
                Log.e(TAG, "ERROR AL ARRANCAR EL SERVIDOR FALSO", e);
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "http://127.0.0.1:" + server.getPort() + "/";
    }

    /** Envuelve un grupo de hilos para contar cuantas tareas tiene pendientes. */
    private Executor tracked(final Executor delegate) {
        return command -> {
            busy.increment();
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    busy.decrement();
                }
            });
        };
    }
}
