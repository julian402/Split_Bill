package ue.edu.co.splitbill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.Group;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    /** Grupos activos en los que el usuario es integrante activo. */
    @Query("SELECT g FROM Group g, GroupMember m "
            + "WHERE m.id.groupId = g.id AND m.id.userId = :userId AND m.status = 1 AND g.status = 1 "
            + "ORDER BY g.createdAt DESC")
    List<Group> findActiveGroupsOfUser(@Param("userId") UUID userId);
}
