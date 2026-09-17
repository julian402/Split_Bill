package ue.edu.co.splitbill.model;

/**
 * Contrato unico entre los repositorios y las pantallas.
 *
 * Como las consultas corren en otro hilo, el repositorio no puede devolver el resultado con return:
 * lo entrega llamando a uno de estos dos metodos, siempre desde el hilo principal, de modo que la
 * Activity pueda actualizar las vistas sin preocuparse por hilos.
 *
 * Es generico para que sirva igual con un usuario, una lista de gastos o un numero de filas afectadas.
 *
 * @param <T> tipo de dato que devuelve la operacion
 */
public interface DataCallback<T> {

    void onSuccess(T data);

    void onError(String message);
}
