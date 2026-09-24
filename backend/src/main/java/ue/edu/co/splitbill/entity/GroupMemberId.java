package ue.edu.co.splitbill.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Llave compuesta de group_members: el par (grupo, usuario). Una persona aparece una sola vez en
 * cada grupo. JPA exige que la llave sea Serializable e implemente equals y hashCode.
 */
@Embeddable
public class GroupMemberId implements Serializable {

    @Column(name = DatabaseContract.GroupMembers.COLUMN_GROUP_ID)
    private UUID groupId;

    @Column(name = DatabaseContract.GroupMembers.COLUMN_USER_ID)
    private UUID userId;

    /** Constructor vacio: lo exige JPA. */
    public GroupMemberId() {
    }

    public GroupMemberId(UUID groupId, UUID userId) {
        this.groupId = groupId;
        this.userId = userId;
    }

    public UUID getGroupId() {
        return this.groupId;
    }

    public UUID getUserId() {
        return this.userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupMemberId other)) {
            return false;
        }
        return Objects.equals(this.groupId, other.groupId) && Objects.equals(this.userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.groupId, this.userId);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GroupMemberId{");
        sb.append("groupId=").append(groupId);
        sb.append(", userId=").append(userId);
        sb.append('}');
        return sb.toString();
    }
}
