package ue.edu.co.splitbill.manager;

/**
 * Contrato de la base de datos: nombres de tablas, nombres de columnas y consultas.
 *
 * Es la unica fuente de verdad del esquema. Las entidades lo usan en sus anotaciones @ColumnInfo y
 * los @Dao lo usan en sus @Query, de modo que un nombre de columna se escribe una sola vez en todo
 * el proyecto. Cada columna lleva un prefijo de tres letras para saber de que tabla viene apenas se
 * lee una consulta con varios JOIN.
 *
 * La clase es final y su constructor es privado porque solo agrupa constantes: no tiene sentido
 * crear objetos de ella.
 */
public final class DatabaseContract {

    public static final String DATABASE_NAME = "splitbill.db";
    /**
     * Version 2 (entrega 3): la tabla groups gana grp_sync_status y grp_owner_id.
     * Version 3 (entrega 4): tabla group_members, para que cada grupo tenga sus propios integrantes.
     * Version 4 (rediseno): expenses gana exp_category (comida, transporte... o PAYMENT).
     */
    public static final int DATABASE_VERSION = 4;

    /** Borrado logico: las filas no se eliminan, se marcan como inactivas. */
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_INACTIVE = 0;

    /** Valores de sync_status tal como los guarda Converters (el nombre del enum SyncStatus). */
    public static final String SYNCED = "'SYNCED'";
    public static final String PENDING_CREATE = "'PENDING_CREATE'";
    public static final String PENDING_UPDATE = "'PENDING_UPDATE'";
    public static final String PENDING_DELETE = "'PENDING_DELETE'";

    /** Categoria de los pagos entre integrantes: no son gastos y no cuentan en los totales. */
    public static final String PAYMENT = "'PAYMENT'";

    /**
     * Grupo por defecto que se siembra al crear la base de datos. Todas las instalaciones lo crean
     * con el mismo id, por eso al iniciar sesion recibe un UUID propio antes de subirse al servidor.
     */
    public static final String DEFAULT_GROUP_ID = "00000000-0000-0000-0000-000000000001";
    public static final String DEFAULT_GROUP_NAME = "Mi grupo";
    public static final String DEFAULT_GROUP_CURRENCY = "COP";

    private DatabaseContract() {
        //impide crear objetos de esta clase
    }

    /**
     * Personas que participan en los gastos: es el directorio de personas. Desde la version 3, en que
     * grupo esta cada una (y si sigue activa en el) se guarda en group_members; use_status y
     * use_sync_status quedan en la tabla para no reconstruirla, pero ya no se consultan.
     */
    public static final class Users {

        public static final String TABLE_NAME = "users";
        public static final String COLUMN_ID = "use_id";
        public static final String COLUMN_NAMES = "use_names";
        public static final String COLUMN_EMAIL = "use_email";
        public static final String COLUMN_PHONE = "use_phone";
        public static final String COLUMN_STATUS = "use_status";
        public static final String COLUMN_SYNC_STATUS = "use_sync_status";

        public static final String SELECT_BY_ID =
                "SELECT * FROM users WHERE use_id = :userId";

        public static final String COUNT_ALL =
                "SELECT COUNT(*) FROM users";

        private Users() {
            //impide crear objetos de esta clase
        }
    }

    /** Grupos de gastos. Desde la entrega 4 la persona puede tener varios y cambiar entre ellos. */
    public static final class Groups {

        public static final String TABLE_NAME = "groups";
        public static final String COLUMN_ID = "grp_id";
        public static final String COLUMN_NAME = "grp_name";
        public static final String COLUMN_CURRENCY = "grp_currency";
        public static final String COLUMN_CREATED_AT = "grp_created_at";
        public static final String COLUMN_STATUS = "grp_status";
        public static final String COLUMN_SYNC_STATUS = "grp_sync_status";
        public static final String COLUMN_OWNER_ID = "grp_owner_id";

        //groups va entre acentos graves porque GROUPS es palabra reservada de SQLite
        public static final String SELECT_BY_ID =
                "SELECT * FROM `groups` WHERE grp_id = :groupId";

        public static final String SELECT_PENDING =
                "SELECT * FROM `groups` WHERE grp_sync_status <> " + SYNCED;

        public static final String COUNT_PENDING =
                "SELECT COUNT(*) FROM `groups` WHERE grp_sync_status <> " + SYNCED;

        public static final String MARK_SYNCED =
                "UPDATE `groups` SET grp_sync_status = " + SYNCED + " WHERE grp_id = :groupId";

        /**
         * Tres pasos para cambiarle el id al grupo sembrado sin romper las llaves foraneas: se crea
         * una copia con el id nuevo, se mueven los gastos a la copia y se borra el original.
         */
        public static final String COPY_WITH_NEW_ID =
                "INSERT INTO `groups` (grp_id, grp_name, grp_currency, grp_created_at, grp_status, "
                + "grp_sync_status, grp_owner_id) "
                + "SELECT :newId, grp_name, grp_currency, grp_created_at, grp_status, "
                + PENDING_CREATE + ", :ownerId FROM `groups` WHERE grp_id = :oldId";

        public static final String MOVE_EXPENSES =
                "UPDATE expenses SET exp_group_id = :newId WHERE exp_group_id = :oldId";

        public static final String MOVE_MEMBERS =
                "UPDATE group_members SET gmb_group_id = :newId WHERE gmb_group_id = :oldId";

        /**
         * Grupos activos, por nombre, con cuantos integrantes y gastos tienen, cuanto suman sus gastos
         * y el saldo de la persona (:userId) en cada uno. Subconsultas en vez de JOIN para que un grupo
         * sin gastos tambien salga.
         *
         * El saldo es lo que la persona pago menos lo que le tocaba, igual que en BalanceCalculator. Aqui
         * si entran los pagos (PAYMENT), porque son justamente los que lo dejan en cero.
         */
        public static final String SELECT_ACTIVE_WITH_TOTALS =
                "SELECT g.grp_id AS groupId, g.grp_name AS name, g.grp_owner_id AS ownerId, "
                + "(SELECT COUNT(*) FROM group_members m WHERE m.gmb_group_id = g.grp_id AND m.gmb_status = 1) "
                + "AS memberCount, "
                + "(SELECT COUNT(*) FROM expenses e WHERE e.exp_group_id = g.grp_id AND e.exp_status = 1 "
                + "AND e.exp_category <> " + PAYMENT + ") AS expenseCount, "
                + "(SELECT COALESCE(SUM(e.exp_amount_cents), 0) FROM expenses e "
                + "WHERE e.exp_group_id = g.grp_id AND e.exp_status = 1 AND e.exp_category <> " + PAYMENT
                + ") AS totalCents, "
                + "(SELECT COALESCE(SUM(e.exp_amount_cents), 0) FROM expenses e "
                + "WHERE e.exp_group_id = g.grp_id AND e.exp_status = 1 AND e.exp_payer_id = :userId) "
                + "- (SELECT COALESCE(SUM(s.shr_amount_cents), 0) FROM expense_shares s "
                + "INNER JOIN expenses e ON e.exp_id = s.shr_expense_id "
                + "WHERE e.exp_group_id = g.grp_id AND e.exp_status = 1 AND s.shr_user_id = :userId) "
                + "AS balanceCents "
                + "FROM `groups` g WHERE g.grp_status = 1 ORDER BY g.grp_name COLLATE NOCASE ASC";

        /** Si el grupo nunca se subio, sigue como PENDING_CREATE: al crearlo ya va el nombre nuevo. */
        public static final String UPDATE_NAME =
                "UPDATE `groups` SET grp_name = :name, grp_sync_status = CASE "
                + "WHEN grp_sync_status = " + PENDING_CREATE + " THEN " + PENDING_CREATE
                + " ELSE " + PENDING_UPDATE + " END WHERE grp_id = :groupId";

        public static final String SELECT_SYNCED_ACTIVE_IDS =
                "SELECT grp_id FROM `groups` WHERE grp_status = 1 AND grp_sync_status = " + SYNCED;

        /** El servidor ya no lo devuelve (lo borraron o sacaron a la persona): deja de mostrarse. */
        public static final String MARK_REMOVED_BY_SERVER =
                "UPDATE `groups` SET grp_status = 0 WHERE grp_id = :groupId AND grp_sync_status = " + SYNCED;

        public static final String DELETE_BY_ID =
                "DELETE FROM `groups` WHERE grp_id = :groupId";

        private Groups() {
            //impide crear objetos de esta clase
        }
    }

    /** Quien esta en cada grupo (version 3). */
    public static final class GroupMembers {

        public static final String TABLE_NAME = "group_members";
        public static final String COLUMN_GROUP_ID = "gmb_group_id";
        public static final String COLUMN_USER_ID = "gmb_user_id";
        public static final String COLUMN_STATUS = "gmb_status";
        public static final String COLUMN_SYNC_STATUS = "gmb_sync_status";

        public static final String SELECT_BY_ID =
                "SELECT * FROM group_members WHERE gmb_group_id = :groupId AND gmb_user_id = :userId";

        /** Los integrantes activos de un grupo, con sus datos de users, ordenados por nombre. */
        public static final String SELECT_ACTIVE_USERS =
                "SELECT u.* FROM users u "
                + "INNER JOIN group_members m ON m.gmb_user_id = u.use_id "
                + "WHERE m.gmb_group_id = :groupId AND m.gmb_status = 1 "
                + "ORDER BY u.use_names ASC";

        public static final String COUNT_ACTIVE =
                "SELECT COUNT(*) FROM group_members WHERE gmb_group_id = :groupId AND gmb_status = 1";

        /**
         * Borrado logico que ademas deja el cambio en la cola de sincronizacion. Si la fila nunca se
         * subio, sigue como PENDING_CREATE: el SyncManager la crea y luego la retira en el servidor.
         */
        public static final String SOFT_DELETE =
                "UPDATE group_members SET gmb_status = 0, gmb_sync_status = CASE "
                + "WHEN gmb_sync_status = " + PENDING_CREATE + " THEN " + PENDING_CREATE
                + " ELSE " + PENDING_DELETE + " END "
                + "WHERE gmb_group_id = :groupId AND gmb_user_id = :userId AND gmb_status = 1";

        public static final String SELECT_PENDING =
                "SELECT * FROM group_members WHERE gmb_sync_status <> " + SYNCED;

        public static final String COUNT_PENDING =
                "SELECT COUNT(*) FROM group_members WHERE gmb_sync_status <> " + SYNCED;

        /**
         * Se marca sincronizada solo si nadie la borro mientras se subia (gmb_status sigue igual);
         * si la borraron, queda pendiente y el siguiente ciclo sube el borrado.
         */
        public static final String MARK_SYNCED =
                "UPDATE group_members SET gmb_sync_status = " + SYNCED
                + " WHERE gmb_group_id = :groupId AND gmb_user_id = :userId AND gmb_status = :status";

        private GroupMembers() {
            //impide crear objetos de esta clase
        }
    }

    /** Gastos registrados en un grupo. El monto se guarda en centavos, nunca como decimal. */
    public static final class Expenses {

        public static final String TABLE_NAME = "expenses";
        public static final String COLUMN_ID = "exp_id";
        public static final String COLUMN_GROUP_ID = "exp_group_id";
        public static final String COLUMN_PAYER_ID = "exp_payer_id";
        public static final String COLUMN_DESCRIPTION = "exp_description";
        public static final String COLUMN_AMOUNT_CENTS = "exp_amount_cents";
        public static final String COLUMN_SPLIT_TYPE = "exp_split_type";
        public static final String COLUMN_DATE = "exp_date";
        public static final String COLUMN_CATEGORY = "exp_category";
        public static final String COLUMN_STATUS = "exp_status";
        public static final String COLUMN_SYNC_STATUS = "exp_sync_status";

        /** En un pago, el nombre de quien lo recibio (su unica parte); en un gasto, NULL. */
        public static final String PAYEE_NAMES =
                "(CASE WHEN e.exp_category = " + PAYMENT + " THEN (SELECT pu.use_names FROM expense_shares ps "
                + "INNER JOIN users pu ON pu.use_id = ps.shr_user_id WHERE ps.shr_expense_id = e.exp_id LIMIT 1) "
                + "ELSE NULL END)";

        /**
         * Lista de gastos del grupo con el nombre de quien pago.
         * El JOIN evita tener que consultar la tabla de usuarios una vez por cada fila.
         */
        public static final String SELECT_ACTIVE_WITH_PAYER =
                "SELECT e.exp_id AS expenseId, "
                + "e.exp_description AS description, "
                + "e.exp_amount_cents AS amountCents, "
                + "e.exp_split_type AS splitType, "
                + "e.exp_category AS category, "
                + "e.exp_date AS date, "
                + "u.use_names AS payerNames, "
                + PAYEE_NAMES + " AS payeeNames "
                + "FROM expenses e "
                + "INNER JOIN users u ON u.use_id = e.exp_payer_id "
                + "WHERE e.exp_group_id = :groupId AND e.exp_status = 1 "
                + "ORDER BY e.exp_date DESC";

        public static final String SELECT_BY_ID =
                "SELECT * FROM expenses WHERE exp_id = :expenseId";

        public static final String SELECT_TOTAL_CENTS =
                "SELECT COALESCE(SUM(exp_amount_cents), 0) FROM expenses "
                + "WHERE exp_group_id = :groupId AND exp_status = 1 AND exp_category <> " + PAYMENT;

        /**
         * Actividad: los gastos y pagos de TODOS los grupos activos, los mas recientes primero, con el
         * nombre del grupo, de quien pago y (en los pagos) de quien recibio.
         */
        public static final String SELECT_RECENT_ALL_GROUPS =
                "SELECT e.exp_id AS expenseId, "
                + "e.exp_description AS description, "
                + "e.exp_amount_cents AS amountCents, "
                + "e.exp_split_type AS splitType, "
                + "e.exp_category AS category, "
                + "e.exp_date AS date, "
                + "u.use_names AS payerNames, "
                + PAYEE_NAMES + " AS payeeNames, "
                + "g.grp_name AS groupName "
                + "FROM expenses e "
                + "INNER JOIN users u ON u.use_id = e.exp_payer_id "
                + "INNER JOIN `groups` g ON g.grp_id = e.exp_group_id "
                + "WHERE e.exp_status = 1 AND g.grp_status = 1 "
                + "ORDER BY e.exp_date DESC LIMIT :limit";

        /** Lo gastado en todos los grupos activos desde :fromMillis (0 = desde siempre), sin los pagos. */
        public static final String SUM_ALL_GROUPS_SINCE =
                "SELECT COALESCE(SUM(e.exp_amount_cents), 0) FROM expenses e "
                + "INNER JOIN `groups` g ON g.grp_id = e.exp_group_id "
                + "WHERE e.exp_status = 1 AND g.grp_status = 1 AND e.exp_category <> " + PAYMENT + " "
                + "AND e.exp_date >= :fromMillis";

        /** "Tu parte": la suma de lo que le toco a la persona en los gastos (no pagos) de sus grupos. */
        public static final String SUM_USER_SHARES_ALL_GROUPS =
                "SELECT COALESCE(SUM(s.shr_amount_cents), 0) FROM expense_shares s "
                + "INNER JOIN expenses e ON e.exp_id = s.shr_expense_id "
                + "INNER JOIN `groups` g ON g.grp_id = e.exp_group_id "
                + "WHERE e.exp_status = 1 AND g.grp_status = 1 AND e.exp_category <> " + PAYMENT + " "
                + "AND s.shr_user_id = :userId";

        /** Igual que en users: el borrado queda en la cola de sincronizacion. */
        public static final String SOFT_DELETE =
                "UPDATE expenses SET exp_status = 0, exp_sync_status = CASE "
                + "WHEN exp_sync_status = " + PENDING_CREATE + " THEN " + PENDING_CREATE
                + " ELSE " + PENDING_DELETE + " END WHERE exp_id = :expenseId";

        public static final String SELECT_PENDING =
                "SELECT * FROM expenses WHERE exp_sync_status <> " + SYNCED;

        public static final String COUNT_PENDING =
                "SELECT COUNT(*) FROM expenses WHERE exp_sync_status <> " + SYNCED;

        public static final String COUNT_ALL =
                "SELECT COUNT(*) FROM expenses";

        /** Igual que en users: solo si el gasto no se borro mientras se subia. */
        public static final String MARK_SYNCED =
                "UPDATE expenses SET exp_sync_status = " + SYNCED + " WHERE exp_id = :expenseId AND exp_status = :status";

        /** Gastos del grupo que ya estaban sincronizados: el pull compara esta lista con la del servidor. */
        public static final String SELECT_SYNCED_ACTIVE_IDS =
                "SELECT exp_id FROM expenses WHERE exp_group_id = :groupId AND exp_status = 1 "
                + "AND exp_sync_status = " + SYNCED;

        /** Otro integrante borro el gasto en el servidor: aqui tambien deja de contar. */
        public static final String MARK_DELETED_BY_SERVER =
                "UPDATE expenses SET exp_status = 0 WHERE exp_id = :expenseId AND exp_sync_status = " + SYNCED;

        /**
         * Cuanto puso cada integrante: se agrupa por quien pago y se suman los montos.
         * Es el primero de los dos insumos de BalanceCalculator.
         */
        public static final String SELECT_TOTAL_PAID_BY_USER =
                "SELECT exp_payer_id AS userId, SUM(exp_amount_cents) AS totalCents "
                + "FROM expenses "
                + "WHERE exp_group_id = :groupId AND exp_status = 1 "
                + "GROUP BY exp_payer_id";

        private Expenses() {
            //impide crear objetos de esta clase
        }
    }

    /** Parte de un gasto que le corresponde a cada participante. */
    public static final class ExpenseShares {

        public static final String TABLE_NAME = "expense_shares";
        public static final String COLUMN_ID = "shr_id";
        public static final String COLUMN_EXPENSE_ID = "shr_expense_id";
        public static final String COLUMN_USER_ID = "shr_user_id";
        public static final String COLUMN_AMOUNT_CENTS = "shr_amount_cents";

        public static final String SELECT_BY_EXPENSE =
                "SELECT * FROM expense_shares WHERE shr_expense_id = :expenseId";

        public static final String DELETE_BY_EXPENSE =
                "DELETE FROM expense_shares WHERE shr_expense_id = :expenseId";

        /**
         * Partes de un gasto con el nombre de cada participante, para la pantalla de detalle.
         * Primero las partes mas grandes; a igual monto, por nombre.
         */
        public static final String SELECT_BY_EXPENSE_WITH_NAMES =
                "SELECT s.shr_user_id AS userId, u.use_names AS names, s.shr_amount_cents AS amountCents "
                + "FROM expense_shares s "
                + "INNER JOIN users u ON u.use_id = s.shr_user_id "
                + "WHERE s.shr_expense_id = :expenseId "
                + "ORDER BY s.shr_amount_cents DESC, u.use_names ASC";

        /**
         * Cuanto le correspondia pagar a cada integrante: se recorren las partes de los gastos
         * activos del grupo y se agrupan por participante.
         * Es el segundo insumo de BalanceCalculator.
         */
        public static final String SELECT_TOTAL_OWED_BY_USER =
                "SELECT s.shr_user_id AS userId, SUM(s.shr_amount_cents) AS totalCents "
                + "FROM expense_shares s "
                + "INNER JOIN expenses e ON e.exp_id = s.shr_expense_id "
                + "WHERE e.exp_group_id = :groupId AND e.exp_status = 1 "
                + "GROUP BY s.shr_user_id";

        /**
         * Cuantas transferencias harian falta sin simplificar: cada participante le devuelve su
         * parte a quien pago, gasto por gasto. Es la cifra contra la que se compara el resultado
         * del algoritmo de liquidacion. No se cuentan las partes de quien pago ni las de valor cero.
         */
        public static final String COUNT_DIRECT_TRANSFERS =
                "SELECT COUNT(*) FROM expense_shares s "
                + "INNER JOIN expenses e ON e.exp_id = s.shr_expense_id "
                + "WHERE e.exp_group_id = :groupId AND e.exp_status = 1 AND e.exp_category <> " + PAYMENT + " "
                + "AND s.shr_user_id <> e.exp_payer_id AND s.shr_amount_cents > 0";

        private ExpenseShares() {
            //impide crear objetos de esta clase
        }
    }
}
