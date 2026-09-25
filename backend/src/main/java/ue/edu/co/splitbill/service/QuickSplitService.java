package ue.edu.co.splitbill.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ue.edu.co.splitbill.dto.QuickSplitRequest;
import ue.edu.co.splitbill.dto.QuickSplitResponse;
import ue.edu.co.splitbill.entity.DatabaseContract;
import ue.edu.co.splitbill.entity.QuickSplit;
import ue.edu.co.splitbill.entity.QuickSplitShare;
import ue.edu.co.splitbill.exception.ConflictException;
import ue.edu.co.splitbill.exception.NotFoundException;
import ue.edu.co.splitbill.repository.QuickSplitRepository;

/**
 * Cuentas rapidas guardadas. Cada persona solo ve y borra las suyas: la de otra persona responde
 * "no encontrada", sin revelar que existe.
 */
@Service
public class QuickSplitService {

    private final QuickSplitRepository quickSplitRepository;

    public QuickSplitService(QuickSplitRepository quickSplitRepository) {
        this.quickSplitRepository = quickSplitRepository;
    }

    /** Todas las activas de la persona. Son pocas, asi que la app siempre pide la lista completa. */
    @Transactional(readOnly = true)
    public List<QuickSplitResponse> list(UUID userId) {
        List<QuickSplitResponse> result = new ArrayList<>();
        for (QuickSplit quickSplit : this.quickSplitRepository.findByOwnerIdAndStatusOrderByDateDesc(
                userId, DatabaseContract.STATUS_ACTIVE)) {
            result.add(QuickSplitResponse.from(quickSplit));
        }
        return result;
    }

    /** Si la app reenvia una cuenta que ya se guardo (mismo id), se devuelve la guardada: no se duplica. */
    @Transactional
    public QuickSplitResponse create(UUID userId, QuickSplitRequest request) {
        if (request.id() != null) {
            Optional<QuickSplit> existing = this.quickSplitRepository.findById(request.id());
            if (existing.isPresent()) {
                if (existing.get().isActive() && existing.get().getOwnerId().equals(userId)) {
                    return QuickSplitResponse.from(existing.get());
                }
                throw new ConflictException("Ya existe una cuenta rápida con ese id");
            }
        }
        QuickSplit quickSplit = new QuickSplit(userId, request.description().trim(), request.subtotalCents(),
                request.tipPercent(), request.totalCents(), request.splitType());
        if (request.id() != null) {
            quickSplit.setId(request.id());
        }
        if (request.date() != null) {
            quickSplit.setDate(request.date());
        }
        short position = 0;
        for (QuickSplitRequest.Share share : request.shares()) {
            quickSplit.getShares().add(new QuickSplitShare(position, share.name().trim(), share.amountCents()));
            position++;
        }
        quickSplit.validar();
        return QuickSplitResponse.from(this.quickSplitRepository.save(quickSplit));
    }

    /** Borrado logico. */
    @Transactional
    public void delete(UUID userId, UUID quickSplitId) {
        QuickSplit quickSplit = this.quickSplitRepository.findById(quickSplitId)
                .filter(QuickSplit::isActive)
                .filter(found -> found.getOwnerId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Cuenta rápida no encontrada"));
        quickSplit.setStatus(DatabaseContract.STATUS_INACTIVE);
        this.quickSplitRepository.save(quickSplit);
    }
}
