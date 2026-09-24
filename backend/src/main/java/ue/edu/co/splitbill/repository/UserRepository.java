package ue.edu.co.splitbill.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ue.edu.co.splitbill.entity.User;

/**
 * Acceso a la tabla users. Spring Data genera la implementacion a partir del nombre de cada metodo;
 * donde la consulta no se deduce del nombre, se escribe a mano con @Query.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Integrantes activos de un grupo, ordenados por nombre como en la app. */
    @Query("SELECT u FROM User u, GroupMember m "
            + "WHERE m.id.userId = u.id AND m.id.groupId = :groupId AND m.status = 1 AND u.status = 1 "
            + "ORDER BY u.names ASC")
    List<User> findActiveMembersOfGroup(@Param("groupId") UUID groupId);
}
