package ue.edu.co.splitbill.model;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.network.ApiException;

/**
 * Base de todos los repositorios.
 *
 * Concentra el manejo de hilos y de errores para que ninguna subclase lo repita: mover el trabajo al
 * hilo de disco, devolver el resultado en el hilo principal y registrar en el Log lo que falle. Las
 * subclases solo escriben la consulta.
 *
 * Es el patron metodo plantilla: el padre fija el esqueleto de la operacion y el hijo llena el hueco,
 * que aqui es el Callable con el trabajo concreto.
 *
 * Desde la entrega 3 tiene tambien runNetwork(), el mismo esqueleto pero en el hilo de red, para
 * las operaciones que necesitan al servidor (iniciar sesion, registrarse). La sincronizacion de los
 * datos no pasa por aqui: la hace SyncManager por detras, sin que la pantalla espere.
 */
public abstract class BaseRepository {

    protected final SplitBillDatabase database;
    protected final AppExecutors executors;

    protected BaseRepository(SplitBillDatabase database, AppExecutors executors) {
        this.database = database;
        this.executors = executors;
    }

    /** Etiqueta con la que cada repositorio escribe en el Log. */
    protected abstract String getTag();

    /**
     * Ejecuta el trabajo en el hilo de disco y entrega el resultado en el hilo principal.
     *
     * @param work     consulta o escritura a realizar
     * @param callback quien recibe el resultado o el error
     */
    protected <T> void runAsync(final Callable<T> work, final DataCallback<T> callback) {
        run(this.executors.io(), work, callback);
    }

    /**
     * Igual que runAsync, pero en el hilo de red: para operaciones que esperan respuesta del servidor.
     */
    protected <T> void runNetwork(final Callable<T> work, final DataCallback<T> callback) {
        run(this.executors.network(), work, callback);
    }

    private <T> void run(Executor executor, final Callable<T> work, final DataCallback<T> callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final T result = work.call();
                    executors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess(result);
                        }
                    });
                } catch (final Exception e) {
                    Log.e(getTag(), "ERROR AL EJECUTAR LA OPERACION", e);
                    executors.mainThread().execute(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(describirError(e));
                        }
                    });
                }
            }
        });
    }

    /**
     * Convierte la excepcion en un mensaje que se le pueda mostrar al usuario.
     *
     * Las validaciones del dominio traen un mensaje escrito para el usuario y se muestran tal cual;
     * cualquier otra falla se reporta de forma generica y el detalle queda en el Log.
     */
    private String describirError(Exception e) {
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException
                || e instanceof ApiException) {
            //ApiException trae el mensaje que escribio el servidor, por ejemplo "Email o contrasena incorrectos"
            return e.getMessage();
        }
        if (e instanceof IOException) {
            return "No hay conexión con el servidor. Revisa tu internet e intenta de nuevo";
        }
        return "No fue posible completar la operación";
    }
}
