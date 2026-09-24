package ue.edu.co.splitbill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Persona que participa en los gastos.
 *
 * Hay dos clases de usuario en la misma tabla:
 * - Cuenta registrada: tiene email y hash de contrasena, y puede iniciar sesion.
 * - Integrante sin cuenta: solo tiene nombre; lo agrega alguien del grupo (como en la app de la
 *   entrega 1, donde los integrantes son solo nombres).
 *
 * La contrasena nunca se guarda: se guarda su hash BCrypt. Y esta entidad nunca sale del servidor:
 * los controladores responden con UserResponse, que no tiene el campo del hash.
 */
@Entity
@Table(name = DatabaseContract.Users.TABLE_NAME)
public class User {

    @Id
    @Column(name = DatabaseContract.Users.COLUMN_ID)
    private UUID id;

    @Column(name = DatabaseContract.Users.COLUMN_NAMES, nullable = false)
    private String names;

    @Column(name = DatabaseContract.Users.COLUMN_EMAIL, unique = true)
    private String email;

    @Column(name = DatabaseContract.Users.COLUMN_PHONE)
    private String phone;

    @Column(name = DatabaseContract.Users.COLUMN_PASSWORD_HASH)
    private String passwordHash;

    @Column(name = DatabaseContract.Users.COLUMN_STATUS, nullable = false)
    private short status;

    @Column(name = DatabaseContract.Users.COLUMN_CREATED_AT, nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = DatabaseContract.Users.COLUMN_UPDATED_AT, nullable = false)
    private Instant updatedAt;

    /** Constructor vacio: es el que usa JPA para reconstruir la fila. */
    public User() {
        this.id = UUID.randomUUID();
        this.status = DatabaseContract.STATUS_ACTIVE;
    }

    /** Integrante sin cuenta: solo nombre. */
    public User(String names) {
        this();
        this.names = names;
    }

    /** Cuenta registrada. El hash ya debe venir calculado por el PasswordEncoder. */
    public User(String names, String email, String phone, String passwordHash) {
        this(names);
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    public void validar() {
        if (this.names == null || this.names.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (this.names.trim().length() < 2) {
            throw new IllegalArgumentException("El nombre es demasiado corto");
        }
        if ((this.email == null) != (this.passwordHash == null)) {
            throw new IllegalArgumentException("Una cuenta debe tener email y contrasena");
        }
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
    }

    public boolean hasAccount() {
        return this.email != null && this.passwordHash != null;
    }

    /** Las marcas de tiempo las pone el servidor, nunca el cliente. */
    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
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

    public String getPasswordHash() {
        return this.passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public short getStatus() {
        return this.status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public String toString() {
        //el hash de la contrasena no se imprime nunca, ni siquiera en los logs
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("id=").append(id);
        sb.append(", names=").append(names);
        sb.append(", email=").append(email);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
