package ue.edu.co.splitbill.entity;

/**
 * Estado de sincronizacion de una fila frente al servidor.
 *
 * En esta entrega todo se guarda como PENDING_CREATE y nadie lo lee: la columna existe desde ahora
 * para que la entrega que agrega la API no obligue a migrar el esquema. Es el patron de bandeja de
 * salida: la base de datos local guarda su propia cola de cambios por enviar.
 */
public enum SyncStatus {

    /** La fila ya esta igual en el servidor. */
    SYNCED,

    /** La fila se creo en el dispositivo y todavia no se ha enviado. */
    PENDING_CREATE,

    /** La fila se modifico en el dispositivo y todavia no se ha enviado. */
    PENDING_UPDATE,

    /** La fila se elimino en el dispositivo y todavia no se ha enviado. */
    PENDING_DELETE
}
