package ue.edu.co.splitbill.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

import ue.edu.co.splitbill.entity.QuickSplit;

public interface QuickSplitRepository extends JpaRepository<QuickSplit, UUID> {

    /** Cuentas rapidas activas de la persona, de la mas reciente a la mas antigua, con sus partes. */
    @EntityGraph(attributePaths = "shares")
    List<QuickSplit> findByOwnerIdAndStatusOrderByDateDesc(UUID ownerId, short status);
}
