package ue.edu.co.splitbill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Pertenencia de una persona a un grupo.
 *
 * Retirar a alguien del grupo lo deja inactivo en vez de borrar la fila: sus gastos anteriores siguen
 * existiendo y siguen apuntando a el, asi que los saldos del grupo no cambian.
 */
@Entity
@Table(name = DatabaseContract.GroupMembers.TABLE_NAME)
public class GroupMember {

    @EmbeddedId
    private GroupMemberId id;

    @Column(name = DatabaseContract.GroupMembers.COLUMN_STATUS, nullable = false)
    private short status;

    @Column(name = DatabaseContract.GroupMembers.COLUMN_JOINED_AT, nullable = false, updatable = false)
    private Instant joinedAt;

    /** Constructor vacio: lo exige JPA. */
    public GroupMember() {
        this.status = DatabaseContract.STATUS_ACTIVE;
    }

    public GroupMember(UUID groupId, UUID userId) {
        this();
        this.id = new GroupMemberId(groupId, userId);
    }

    public boolean isActive() {
        return this.status == DatabaseContract.STATUS_ACTIVE;
    }

    @PrePersist
    void onCreate() {
        this.joinedAt = Instant.now();
    }

    public GroupMemberId getId() {
        return this.id;
    }

    public UUID getGroupId() {
        return this.id.getGroupId();
    }

    public UUID getUserId() {
        return this.id.getUserId();
    }

    public short getStatus() {
        return this.status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return this.joinedAt;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GroupMember{");
        sb.append("id=").append(id);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
