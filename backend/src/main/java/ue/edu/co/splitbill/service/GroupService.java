package ue.edu.co.splitbill.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import ue.edu.co.splitbill.dto.GroupRequest;
import ue.edu.co.splitbill.dto.GroupResponse;
import ue.edu.co.splitbill.dto.MemberRequest;
import ue.edu.co.splitbill.dto.UserResponse;
import ue.edu.co.splitbill.entity.DatabaseContract;
import ue.edu.co.splitbill.entity.Expense;
import ue.edu.co.splitbill.entity.ExpenseShare;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.GroupMemberId;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.exception.ConflictException;
import ue.edu.co.splitbill.exception.ForbiddenException;
import ue.edu.co.splitbill.exception.NotFoundException;
import ue.edu.co.splitbill.repository.ExpenseRepository;
import ue.edu.co.splitbill.repository.GroupMemberRepository;
import ue.edu.co.splitbill.repository.GroupRepository;
import ue.edu.co.splitbill.repository.UserRepository;

/**
 * Grupos y sus integrantes.
 *
 * Regla de permisos: cualquier integrante puede ver el grupo y agregar personas; solo el dueno puede
 * editarlo, borrarlo o retirar integrantes. Quien no pertenece al grupo recibe 404, como si no
 * existiera.
 */
@Service
public class GroupService {

    private static final String GROUP_NOT_FOUND = "Grupo no encontrado";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    public GroupService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository, ExpenseRepository expenseRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listMine(UUID userId) {
        List<GroupResponse> result = new ArrayList<>();
        for (Group group : this.groupRepository.findActiveGroupsOfUser(userId)) {
            result.add(GroupResponse.from(group));
        }
        return result;
    }

    /**
     * Crea el grupo y deja a quien lo crea como dueno e integrante. Si la app manda un id que ya
     * existe y el usuario pertenece a ese grupo, devuelve el grupo en vez de duplicarlo (reintento).
     */
    @Transactional
    public GroupResponse create(UUID userId, GroupRequest request) {
        if (request.id() != null) {
            Optional<Group> existing = this.groupRepository.findById(request.id());
            if (existing.isPresent()) {
                if (existing.get().isActive() && this.groupMemberRepository.isActiveMember(request.id(), userId)) {
                    return GroupResponse.from(existing.get());
                }
                throw new ConflictException("Ya existe un grupo con ese id");
            }
        }
        Group group = new Group(request.name().trim(), request.currency(), userId);
        if (request.id() != null) {
            group.setId(request.id());
        }
        group.validar();
        group = this.groupRepository.save(group);
        this.groupMemberRepository.save(new GroupMember(group.getId(), userId));
        return GroupResponse.from(group);
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID userId, UUID groupId) {
        return GroupResponse.from(requireMembership(groupId, userId));
    }

    @Transactional
    public GroupResponse update(UUID userId, UUID groupId, GroupRequest request) {
        Group group = requireMembership(groupId, userId);
        requireOwner(group, userId);
        group.setName(request.name().trim());
        if (request.currency() != null) {
            group.setCurrency(request.currency());
        }
        group.validar();
        return GroupResponse.from(this.groupRepository.saveAndFlush(group));
    }

    /** Borrado logico: el grupo deja de aparecer, pero sus gastos se conservan. */
    @Transactional
    public void delete(UUID userId, UUID groupId) {
        Group group = requireMembership(groupId, userId);
        requireOwner(group, userId);
        group.setStatus(DatabaseContract.STATUS_INACTIVE);
        this.groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listMembers(UUID userId, UUID groupId) {
        return listMembers(userId, groupId, false);
    }

    /**
     * Integrantes del grupo. Con includeRemoved tambien vienen los retirados, marcados active=false:
     * la app los necesita porque los gastos viejos todavia los referencian.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> listMembers(UUID userId, UUID groupId, boolean includeRemoved) {
        requireMembership(groupId, userId);
        List<UserResponse> result = new ArrayList<>();
        if (!includeRemoved) {
            for (User member : this.userRepository.findActiveMembersOfGroup(groupId)) {
                result.add(UserResponse.from(member));
            }
            return result;
        }
        Map<UUID, Boolean> activeByUser = new HashMap<>();
        for (GroupMember membership : this.groupMemberRepository.findByIdGroupId(groupId)) {
            activeByUser.put(membership.getUserId(), membership.isActive());
        }
        for (User member : this.userRepository.findAllById(activeByUser.keySet())) {
            result.add(UserResponse.from(member, member.isActive() && activeByUser.get(member.getId())));
        }
        result.sort(Comparator.comparing(UserResponse::names));
        return result;
    }

    /**
     * Agrega un integrante por email (persona con cuenta) o por nombre (integrante sin cuenta).
     * Si la persona ya estaba en el grupo, no se duplica: se devuelve (y se reactiva si la habian retirado).
     */
    @Transactional
    public UserResponse addMember(UUID userId, UUID groupId, MemberRequest request) {
        requireMembership(groupId, userId);
        User member;
        if (request.email() != null && !request.email().isBlank()) {
            member = this.userRepository.findByEmail(AuthService.normalizeEmail(request.email()))
                    .filter(User::isActive)
                    .orElseThrow(() -> new NotFoundException("No existe una cuenta con ese email"));
        } else if (request.names() != null && !request.names().isBlank()) {
            member = findOrCreateMemberWithoutAccount(groupId, request);
        } else {
            throw new IllegalArgumentException("Indique el nombre o el email del integrante");
        }

        GroupMemberId membershipId = new GroupMemberId(groupId, member.getId());
        Optional<GroupMember> membership = this.groupMemberRepository.findById(membershipId);
        if (membership.isEmpty()) {
            this.groupMemberRepository.save(new GroupMember(groupId, member.getId()));
        } else if (!membership.get().isActive()) {
            membership.get().setStatus(DatabaseContract.STATUS_ACTIVE);
        }
        return UserResponse.from(member);
    }

    /** Solo el dueno retira integrantes, y el dueno no puede retirarse de su propio grupo. */
    @Transactional
    public void removeMember(UUID userId, UUID groupId, UUID memberId) {
        Group group = requireMembership(groupId, userId);
        requireOwner(group, userId);
        if (group.isOwnedBy(memberId)) {
            throw new IllegalArgumentException("El dueño no puede retirarse de su propio grupo");
        }
        GroupMember membership = this.groupMemberRepository.findById(new GroupMemberId(groupId, memberId))
                .filter(GroupMember::isActive)
                .orElseThrow(() -> new NotFoundException("La persona no es integrante del grupo"));
        membership.setStatus(DatabaseContract.STATUS_INACTIVE);
    }

    /**
     * "Soy yo": la persona que inicio sesion dice que un integrante agregado por nombre es ella misma.
     *
     * Pasa cuando alguien uso la app antes de tener cuenta y se agrego como integrante: queda dos veces
     * en el grupo (el integrante sin cuenta y su cuenta). Aqui se juntan: los gastos que pago el
     * integrante pasan a la cuenta, sus partes tambien (si los dos tenian parte en el mismo gasto, se
     * suman) y el integrante queda retirado del grupo. Los totales y los saldos no cambian, solo de
     * quien son.
     */
    @Transactional
    public UserResponse claimMember(UUID userId, UUID groupId, UUID memberId) {
        requireMembership(groupId, userId);
        if (userId.equals(memberId)) {
            throw new IllegalArgumentException("Ya eres tú");
        }
        GroupMember membership = this.groupMemberRepository.findById(new GroupMemberId(groupId, memberId))
                .filter(GroupMember::isActive)
                .orElseThrow(() -> new NotFoundException("La persona no es integrante del grupo"));
        User member = this.userRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("La persona no es integrante del grupo"));
        if (member.hasAccount()) {
            throw new ConflictException("Esa persona ya tiene su propia cuenta");
        }

        for (Expense expense : this.expenseRepository.findByGroupId(groupId)) {
            if (expense.getPayerId().equals(memberId)) {
                expense.setPayerId(userId);
            }
            ExpenseShare memberShare = null;
            ExpenseShare userShare = null;
            for (ExpenseShare share : expense.getShares()) {
                if (share.getUserId().equals(memberId)) {
                    memberShare = share;
                } else if (share.getUserId().equals(userId)) {
                    userShare = share;
                }
            }
            if (memberShare != null || expense.getPayerId().equals(userId)) {
                //cambio de quien es el gasto o sus partes: la app lo debe traer en su proxima sincronizacion
                expense.touch();
            }
            if (memberShare != null && userShare != null) {
                //los dos tenian parte: se suman en la de la cuenta y la del integrante desaparece
                userShare.setAmountCents(userShare.getAmountCents() + memberShare.getAmountCents());
                expense.getShares().remove(memberShare);
            } else if (memberShare != null) {
                memberShare.setUserId(userId);
            }
        }
        membership.setStatus(DatabaseContract.STATUS_INACTIVE);
        return UserResponse.from(this.userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado")));
    }

    /**
     * Verificacion de permisos que usan todos los servicios: el grupo existe, esta activo y el
     * usuario es integrante. Si algo falla, 404.
     */
    @Transactional(readOnly = true)
    public Group requireMembership(UUID groupId, UUID userId) {
        Group group = this.groupRepository.findById(groupId)
                .filter(Group::isActive)
                .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND));
        if (!this.groupMemberRepository.isActiveMember(groupId, userId)) {
            throw new NotFoundException(GROUP_NOT_FOUND);
        }
        return group;
    }

    private void requireOwner(Group group, UUID userId) {
        if (!group.isOwnedBy(userId)) {
            throw new ForbiddenException("Solo el dueño del grupo puede hacer esta acción");
        }
    }

    /**
     * La app puede mandar el UUID que el integrante ya tenia en el celular. Ese id solo se reutiliza
     * si la persona ya pertenecia a este grupo (un reintento); si es de otra persona ajena al grupo
     * se rechaza, para que nadie pueda meter en su grupo a usuarios de otros grupos adivinando ids.
     */
    private User findOrCreateMemberWithoutAccount(UUID groupId, MemberRequest request) {
        if (request.id() != null) {
            Optional<User> existing = this.userRepository.findById(request.id());
            if (existing.isPresent()) {
                if (this.groupMemberRepository.existsById(new GroupMemberId(groupId, request.id()))) {
                    return existing.get();
                }
                throw new ConflictException("Ya existe una persona con ese id");
            }
        }
        User member = new User(request.names().trim());
        if (request.id() != null) {
            member.setId(request.id());
        }
        member.validar();
        return this.userRepository.save(member);
    }
}
