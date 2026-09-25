package ue.edu.co.splitbill.entity;

/**
 * Contrato de la base de datos del servidor: nombres de tablas y columnas.
 *
 * Es el mismo patron que DatabaseContract de la app Android y usa los mismos nombres, de modo que un
 * gasto se llama igual en el celular y en el servidor. Las tablas las crea Flyway con el script
 * db/migration/V1__init_schema.sql; las entidades usan estas constantes en sus @Column. V2 agrega la categoria del gasto y V3 las
 * cuentas rapidas guardadas.
 *
 * La clase es final y su constructor es privado porque solo agrupa constantes.
 */
public final class DatabaseContract {

    /** Borrado logico: las filas no se eliminan, se marcan como inactivas. */
    public static final short STATUS_ACTIVE = 1;
    public static final short STATUS_INACTIVE = 0;

    public static final String DEFAULT_CURRENCY = "COP";

    private DatabaseContract() {
        //impide crear objetos de esta clase
    }

    public static final class Users {

        public static final String TABLE_NAME = "users";
        public static final String COLUMN_ID = "use_id";
        public static final String COLUMN_NAMES = "use_names";
        public static final String COLUMN_EMAIL = "use_email";
        public static final String COLUMN_PHONE = "use_phone";
        public static final String COLUMN_PASSWORD_HASH = "use_password_hash";
        public static final String COLUMN_STATUS = "use_status";
        public static final String COLUMN_CREATED_AT = "use_created_at";
        public static final String COLUMN_UPDATED_AT = "use_updated_at";

        private Users() {
            //impide crear objetos de esta clase
        }
    }

    public static final class Groups {

        public static final String TABLE_NAME = "groups";
        public static final String COLUMN_ID = "grp_id";
        public static final String COLUMN_NAME = "grp_name";
        public static final String COLUMN_CURRENCY = "grp_currency";
        public static final String COLUMN_OWNER_ID = "grp_owner_id";
        public static final String COLUMN_STATUS = "grp_status";
        public static final String COLUMN_CREATED_AT = "grp_created_at";
        public static final String COLUMN_UPDATED_AT = "grp_updated_at";

        private Groups() {
            //impide crear objetos de esta clase
        }
    }

    public static final class GroupMembers {

        public static final String TABLE_NAME = "group_members";
        public static final String COLUMN_GROUP_ID = "gmb_group_id";
        public static final String COLUMN_USER_ID = "gmb_user_id";
        public static final String COLUMN_STATUS = "gmb_status";
        public static final String COLUMN_JOINED_AT = "gmb_joined_at";

        private GroupMembers() {
            //impide crear objetos de esta clase
        }
    }

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
        public static final String COLUMN_CREATED_AT = "exp_created_at";
        public static final String COLUMN_UPDATED_AT = "exp_updated_at";

        private Expenses() {
            //impide crear objetos de esta clase
        }
    }

    public static final class ExpenseShares {

        public static final String TABLE_NAME = "expense_shares";
        public static final String COLUMN_ID = "shr_id";
        public static final String COLUMN_EXPENSE_ID = "shr_expense_id";
        public static final String COLUMN_USER_ID = "shr_user_id";
        public static final String COLUMN_AMOUNT_CENTS = "shr_amount_cents";

        private ExpenseShares() {
            //impide crear objetos de esta clase
        }
    }

    /** Cuentas rapidas guardadas (V3): de una sola persona, sin grupo. */
    public static final class QuickSplits {

        public static final String TABLE_NAME = "quick_splits";
        public static final String COLUMN_ID = "qsp_id";
        public static final String COLUMN_OWNER_ID = "qsp_owner_id";
        public static final String COLUMN_DESCRIPTION = "qsp_description";
        public static final String COLUMN_SUBTOTAL_CENTS = "qsp_subtotal_cents";
        public static final String COLUMN_TIP_PERCENT = "qsp_tip_percent";
        public static final String COLUMN_TOTAL_CENTS = "qsp_total_cents";
        public static final String COLUMN_SPLIT_TYPE = "qsp_split_type";
        public static final String COLUMN_DATE = "qsp_date";
        public static final String COLUMN_STATUS = "qsp_status";
        public static final String COLUMN_CREATED_AT = "qsp_created_at";
        public static final String COLUMN_UPDATED_AT = "qsp_updated_at";

        private QuickSplits() {
            //impide crear objetos de esta clase
        }
    }

    /** Lo que le toca a cada persona de una cuenta rapida, identificada por su nombre y su lugar. */
    public static final class QuickSplitShares {

        public static final String TABLE_NAME = "quick_split_shares";
        public static final String COLUMN_ID = "qss_id";
        public static final String COLUMN_QUICK_SPLIT_ID = "qss_quick_split_id";
        public static final String COLUMN_POSITION = "qss_position";
        public static final String COLUMN_NAME = "qss_name";
        public static final String COLUMN_AMOUNT_CENTS = "qss_amount_cents";

        private QuickSplitShares() {
            //impide crear objetos de esta clase
        }
    }
}
