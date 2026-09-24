package ue.edu.co.splitbill.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
