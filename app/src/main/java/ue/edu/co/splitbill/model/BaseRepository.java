package ue.edu.co.splitbill.model;

import android.util.Log;

import java.util.concurrent.Callable;

import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.manager.SplitBillDatabase;

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
 * En la entrega que agrega la API, a esta misma clase se le suman los ganchos pushPendingChanges()
 * y pullRemoteChanges() con el metodo sync() final, sin tocar las subclases ya escritas.
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
        this.executors.io().execute(new Runnable() {
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
                    Log.e(getTag(), "ERROR AL EJECUTAR LA OPERACION EN LA BASE DE DATOS", e);
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
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return e.getMessage();
        }
        return "No fue posible completar la operacion";
    }
}
