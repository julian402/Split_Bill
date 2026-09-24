package ue.edu.co.splitbill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.GroupMemberId;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {

    /** Verificacion de permisos: el usuario pertenece (activo) al grupo. */
    @Query("SELECT COUNT(m) > 0 FROM GroupMember m "
            + "WHERE m.id.groupId = :groupId AND m.id.userId = :userId AND m.status = 1")
    boolean isActiveMember(@Param("groupId") UUID groupId, @Param("userId") UUID userId);

    /** Todas las pertenencias del grupo, activas y retiradas. */
    List<GroupMember> findByIdGroupId(UUID groupId);

    /** Cuantos de los usuarios dados son integrantes activos del grupo. */
    @Query("SELECT COUNT(m) FROM GroupMember m "
            + "WHERE m.id.groupId = :groupId AND m.id.userId IN :userIds AND m.status = 1")
    long countActiveMembers(@Param("groupId") UUID groupId, @Param("userIds") Collection<UUID> userIds);
}
