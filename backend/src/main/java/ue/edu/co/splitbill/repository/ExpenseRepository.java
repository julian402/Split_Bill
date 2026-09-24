package ue.edu.co.splitbill.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    /**
     * Gastos activos del grupo, del mas reciente al mas antiguo. El @EntityGraph trae las partes en
     * la misma consulta, en vez de hacer una consulta extra por cada gasto.
     */
    @EntityGraph(attributePaths = "shares")
    List<Expense> findByGroupIdAndStatusOrderByDateDesc(UUID groupId, short status);

    /**
     * Gastos del grupo que cambiaron despues de la fecha dada, incluidos los borrados: la app los
     * necesita para saber que debe quitarlos.
     */
    @EntityGraph(attributePaths = "shares")
    List<Expense> findByGroupIdAndUpdatedAtAfterOrderByDateDesc(UUID groupId, Instant updatedAt);

    /** Todos los gastos del grupo, tambien los borrados, con sus partes. Lo usa "Soy yo". */
    @EntityGraph(attributePaths = "shares")
    List<Expense> findByGroupId(UUID groupId);
}
