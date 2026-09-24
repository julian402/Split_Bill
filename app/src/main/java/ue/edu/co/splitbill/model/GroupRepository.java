package ue.edu.co.splitbill.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import ue.edu.co.splitbill.dao.GroupListItem;
import ue.edu.co.splitbill.di.AppExecutors;
import ue.edu.co.splitbill.entity.Group;
import ue.edu.co.splitbill.entity.GroupMember;
import ue.edu.co.splitbill.entity.SyncStatus;
import ue.edu.co.splitbill.entity.User;
import ue.edu.co.splitbill.manager.DatabaseContract;
import ue.edu.co.splitbill.manager.SplitBillDatabase;
import ue.edu.co.splitbill.session.SessionManager;
import ue.edu.co.splitbill.sync.SyncManager;

/**
 * Los grupos de la persona: listarlos, crear uno nuevo, renombrarlo y cambiar de grupo.
 *
 * Como todo en la app, primero se escribe en el celular y el SyncManager lo sube despues: se puede
 * crear un grupo sin conexion y empezar a registrar gastos en el de una vez.
 */
public class GroupRepository extends BaseRepository {

    private static final String TAG = "GroupRepository";

    private final SessionManager sessionManager;
    private final SyncManager syncManager;

    public GroupRepository(SplitBillDatabase database, AppExecutors executors, SessionManager sessionManager,
                           SyncManager syncManager) {
        super(database, executors);
        this.sessionManager = sessionManager;
        this.syncManager = syncManager;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    public String getCurrentGroupId() {
        return this.sessionManager.getCurrentGroupId();
    }

    public String getCurrentUserId() {
        return this.sessionManager.getUserId();
    }

    /** Cuantos avatares pequenos se pintan en cada tarjeta de grupo. */
    private static final int AVATARS_PER_GROUP = 3;

    /** Grupos activos con su total, su gente y el saldo de la persona, por nombre. */
    public void getGroups(DataCallback<List<GroupListItem>> callback) {
        runAsync(new Callable<List<GroupListItem>>() {
            @Override
            public List<GroupListItem> call() {
                List<GroupListItem> groups = database.groupDao().findActiveWithTotals(getCurrentUserId());
                fillMemberNames(database, groups);
                return groups;
            }
        }, callback);
    }

    /** Los nombres de los primeros integrantes de cada grupo, para los avatares de su tarjeta. */
    static void fillMemberNames(SplitBillDatabase database, List<GroupListItem> groups) {
        for (GroupListItem group : groups) {
            List<User> users = database.groupMemberDao().findActiveUsers(group.getGroupId());
            List<String> names = new ArrayList<>();
            for (int i = 0; i < users.size() && i < AVATARS_PER_GROUP; i++) {
                names.add(users.get(i).getNames());
            }
            group.setMemberNames(names);
        }
    }

    /** El grupo actual, para mostrar su nombre en la pantalla principal. */
    public void getCurrentGroup(DataCallback<Group> callback) {
        runAsync(new Callable<Group>() {
            @Override
            public Group call() {
                Group group = database.groupDao().findById(sessionManager.getCurrentGroupId());
                if (group == null) {
                    throw new IllegalStateException("No se encontró el grupo");
                }
                return group;
            }
        }, callback);
    }

    /**
     * Crea un grupo con la persona como duena e integrante, mas los integrantes que haya escrito en
     * el formulario, y lo deja como grupo actual. Todo va en una sola transaccion: o queda el grupo
     * con su gente, o no queda nada.
     *
     * @param members personas nuevas (sin cuenta) que entran al grupo; puede ir vacia
     * @param callback recibe el grupo creado
     */
    public void createGroup(final String name, final List<User> members, DataCallback<Group> callback) {
        runAsync(new Callable<Group>() {
            @Override
            public Group call() {
                final Group group = new Group(name == null ? null : name.trim(), DatabaseContract.DEFAULT_GROUP_CURRENCY);
                group.validar();
                for (User member : members) {
                    member.validar();
                }
                final String userId = sessionManager.getUserId();
                group.setOwnerId(userId);
                database.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        database.groupDao().insert(group);
                        //el servidor agrega al creador como integrante: por eso ya queda sincronizado
                        database.groupMemberDao().upsert(new GroupMember(group.getId(), userId, SyncStatus.SYNCED));
                        for (User member : members) {
                            database.userDao().insert(member);
                            database.groupMemberDao().upsert(new GroupMember(group.getId(), member.getId(),
                                    SyncStatus.PENDING_CREATE));
                        }
                    }
                });
                sessionManager.setCurrentGroupId(group.getId());
                syncManager.notifyLocalChange();
                return group;
            }
        }, callback);
    }

    /** Cambia el nombre del grupo. En el servidor solo lo acepta si la persona es la duena. */
    public void renameGroup(final String groupId, final String name, DataCallback<Integer> callback) {
        runAsync(new Callable<Integer>() {
            @Override
            public Integer call() {
                Group probe = new Group(name == null ? null : name.trim(), DatabaseContract.DEFAULT_GROUP_CURRENCY);
                probe.validar();
                int rows = database.groupDao().updateName(groupId, probe.getName());
                if (rows == 0) {
                    throw new IllegalArgumentException("No se encontró el grupo");
                }
                syncManager.notifyLocalChange();
                return rows;
            }
        }, callback);
    }

    /** Desde ahora las pantallas muestran este grupo, y se sincroniza para traer lo mas reciente. */
    public void switchGroup(String groupId) {
        this.sessionManager.setCurrentGroupId(groupId);
        this.syncManager.requestSync();
    }
}
