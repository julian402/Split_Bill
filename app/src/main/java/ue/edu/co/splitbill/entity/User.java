package ue.edu.co.splitbill.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

import ue.edu.co.splitbill.manager.DatabaseContract;

/**
 * Integrante que participa en los gastos del grupo.
 *
 * La llave primaria es un UUID generado en el dispositivo y no un entero autoincremental. Hoy no se
 * nota la diferencia, pero es lo que hara posible trabajar sin conexion cuando entre la API: una fila
 * creada en el celular ya nace con su identificador definitivo y el servidor la acepta tal cual, sin
 * tener que reconciliar identificadores locales contra remotos.
 */
@Entity(tableName = DatabaseContract.Users.TABLE_NAME,
        indices = {@Index(value = DatabaseContract.Users.COLUMN_NAMES)})
public class User {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = DatabaseContract.Users.COLUMN_ID)
    private String id;

    @ColumnInfo(name = DatabaseContract.Users.COLUMN_NAMES)
    private String names;

    @ColumnInfo(name = DatabaseContract.Users.COLUMN_EMAIL)
    private String email;

    @ColumnInfo(name = DatabaseContract.Users.COLUMN_PHONE)
    private String phone;

    @ColumnInfo(name = DatabaseContract.Users.COLUMN_STATUS)
    private int status;

    @ColumnInfo(name = DatabaseContract.Users.COLUMN_SYNC_STATUS)
    private SyncStatus syncStatus;

    /** Constructor vacio: es el que usa Room para reconstruir la fila. */
    public User() {
        this.id = UUID.randomUUID().toString();
        this.status = DatabaseContract.STATUS_ACTIVE;
        this.syncStatus = SyncStatus.PENDING_CREATE;
    }

    /**
     * Constructor de conveniencia para crear un integrante nuevo desde la pantalla.
     * Lleva @Ignore para que Room no lo confunda con el constructor que debe usar.
     */
    @Ignore
    public User(String names, String email, String phone) {
        this();
        this.names = names;
        this.email = email;
        this.phone = phone;
    }

    public void validar() {
        if (this.names == null || this.names.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del integrante es obligatorio");
        }
        if (this.names.trim().length() < 2) {
            throw new IllegalArgumentException("El nombre del integrante es demasiado corto");
        }
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getNames() {
        return this.names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public SyncStatus getSyncStatus() {
        return this.syncStatus;
    }

    public void setSyncStatus(SyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    /** Es el texto que se muestra en el Spinner de pagadores. */
    @Override
    public String toString() {
        return this.names;
    }
}
