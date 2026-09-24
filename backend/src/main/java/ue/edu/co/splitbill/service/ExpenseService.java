package ue.edu.co.splitbill.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ue.edu.co.splitbill.dto.ExpenseRequest;
import ue.edu.co.splitbill.dto.ExpenseResponse;
import ue.edu.co.splitbill.dto.ShareRequest;
import ue.edu.co.splitbill.entity.DatabaseContract;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.exception.ConflictException;
import ue.edu.co.splitbill.exception.NotFoundException;
import ue.edu.co.splitbill.repository.ExpenseRepository;
import ue.edu.co.splitbill.repository.GroupMemberRepository;

/**
 * Gastos de un grupo. Cualquier integrante puede registrar, editar o borrar gastos.
 *
 * Antes de guardar se comprueba:
 * 1. Las reglas del gasto (Expense.validar): monto positivo, partes que suman exactamente el monto.
 * 2. Que el pagador y todos los participantes sean integrantes activos del grupo.
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepository, GroupMemberRepository groupMemberRepository,
                          GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID userId, UUID groupId) {
        this.groupService.requireMembership(groupId, userId);
        List<ExpenseResponse> result = new ArrayList<>();
        for (Expense expense : this.expenseRepository.findByGroupIdAndStatusOrderByDateDesc(groupId,
                DatabaseContract.STATUS_ACTIVE)) {
            result.add(ExpenseResponse.from(expense));
        }
        return result;
    }

    /**
     * Registra un gasto. Si la app manda un id que ya existe en este grupo, devuelve el gasto
     * guardado en vez de crear otro: asi un envio que se corto y se reintenta no duplica el gasto.
     */
    @Transactional
    public ExpenseResponse create(UUID userId, UUID groupId, ExpenseRequest request) {
        this.groupService.requireMembership(groupId, userId);
        if (request.id() != null) {
            Optional<Expense> existing = this.expenseRepository.findById(request.id());
            if (existing.isPresent()) {
                if (existing.get().isActive() && existing.get().getGroupId().equals(groupId)) {
                    return ExpenseResponse.from(existing.get());
                }
                throw new ConflictException("Ya existe un gasto con ese id");
            }
        }
        Expense expense = new Expense(groupId, request.payerId(), request.description().trim(),
                request.amountCents(), request.splitType());
        if (request.id() != null) {
            expense.setId(request.id());
        }
        if (request.date() != null) {
            expense.setDate(request.date());
        }
        for (ShareRequest share : request.shares()) {
            expense.getShares().add(new ExpenseShare(share.userId(), share.amountCents()));
        }
        expense.validar();
        validateParticipants(groupId, expense);
        return ExpenseResponse.from(this.expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(UUID userId, UUID groupId, UUID expenseId) {
        this.groupService.requireMembership(groupId, userId);
        return ExpenseResponse.from(requireExpense(groupId, expenseId));
    }

    @Transactional
    public ExpenseResponse update(UUID userId, UUID groupId, UUID expenseId, ExpenseRequest request) {
        this.groupService.requireMembership(groupId, userId);
        Expense expense = requireExpense(groupId, expenseId);
        expense.setPayerId(request.payerId());
        expense.setDescription(request.description().trim());
        expense.setAmountCents(request.amountCents());
        expense.setSplitType(request.splitType());
        if (request.date() != null) {
            expense.setDate(request.date());
        }
        replaceShares(expense, request.shares());
        expense.validar();
        validateParticipants(groupId, expense);
        //saveAndFlush para que @PreUpdate actualice la fecha antes de armar la respuesta
        return ExpenseResponse.from(this.expenseRepository.saveAndFlush(expense));
    }

    /** Borrado logico: el gasto deja de contar en el listado y en los saldos. */
    @Transactional
    public void delete(UUID userId, UUID groupId, UUID expenseId) {
        this.groupService.requireMembership(groupId, userId);
        Expense expense = requireExpense(groupId, expenseId);
        expense.setStatus(DatabaseContract.STATUS_INACTIVE);
        this.expenseRepository.save(expense);
    }

    private Expense requireExpense(UUID groupId, UUID expenseId) {
        return this.expenseRepository.findById(expenseId)
                .filter(Expense::isActive)
                .filter(expense -> expense.getGroupId().equals(groupId))
                .orElseThrow(() -> new NotFoundException("Gasto no encontrado"));
    }

    private void validateParticipants(UUID groupId, Expense expense) {
        Set<UUID> people = new HashSet<>(expense.getParticipantIds());
        people.add(expense.getPayerId());
        if (this.groupMemberRepository.countActiveMembers(groupId, people) != people.size()) {
            throw new IllegalArgumentException(
                    "El pagador y todos los participantes deben ser integrantes del grupo");
        }
    }

    /**
     * Reemplaza las partes de un gasto existente. Las partes de quien sigue participando se
     * actualizan en su lugar en vez de borrarlas y crearlas de nuevo: la tabla no permite dos partes
     * de la misma persona en un gasto, y Hibernate inserta las filas nuevas antes de borrar las viejas.
     */
    private void replaceShares(Expense expense, List<ShareRequest> newShares) {
        Map<UUID, ExpenseShare> current = new HashMap<>();
        for (ExpenseShare share : expense.getShares()) {
            current.put(share.getUserId(), share);
        }
        Set<UUID> keep = new HashSet<>();
        for (ShareRequest request : newShares) {
            ExpenseShare share = current.get(request.userId());
            if (share != null && !keep.contains(request.userId())) {
                share.setAmountCents(request.amountCents());
            } else {
                //un participante nuevo, o uno repetido en la peticion (lo rechazara validar())
                expense.getShares().add(new ExpenseShare(request.userId(), request.amountCents()));
            }
            keep.add(request.userId());
        }
        expense.getShares().removeIf(share -> !keep.contains(share.getUserId()));
    }
}
