-- Esquema inicial de SplitBill en el servidor.
--
-- Usa los mismos nombres de tabla y columna que la app Android (DatabaseContract), con el prefijo de
-- tres letras por tabla. Las llaves primarias son UUID que puede generar el cliente, y los montos se
-- guardan en centavos (BIGINT), nunca como decimal. Nada se borra: status 1 = activo, 0 = inactivo.

-- Personas. Un integrante agregado por nombre no tiene email ni contrasena; quien se registra en la
-- app si los tiene. Asi un grupo puede incluir a personas que no usan la aplicacion.
CREATE TABLE users (
    use_id            UUID         PRIMARY KEY,
    use_names         VARCHAR(100) NOT NULL,
    use_email         VARCHAR(150) UNIQUE,
    use_phone         VARCHAR(30),
    use_password_hash VARCHAR(100),
    use_status        SMALLINT     NOT NULL DEFAULT 1 CHECK (use_status IN (0, 1)),
    use_created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    use_updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- una cuenta registrada siempre tiene las dos cosas, o ninguna
    CONSTRAINT chk_users_account CHECK ((use_email IS NULL) = (use_password_hash IS NULL))
);

CREATE TABLE groups (
    grp_id         UUID         PRIMARY KEY,
    grp_name       VARCHAR(100) NOT NULL,
    grp_currency   VARCHAR(3)   NOT NULL DEFAULT 'COP',
    grp_owner_id   UUID         NOT NULL REFERENCES users (use_id),
    grp_status     SMALLINT     NOT NULL DEFAULT 1 CHECK (grp_status IN (0, 1)),
    grp_created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    grp_updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Quien pertenece a cada grupo. Retirar a alguien lo deja inactivo, no borra la fila, porque sus
-- gastos anteriores siguen apuntando a el.
CREATE TABLE group_members (
    gmb_group_id  UUID        NOT NULL REFERENCES groups (grp_id),
    gmb_user_id   UUID        NOT NULL REFERENCES users (use_id),
    gmb_status    SMALLINT    NOT NULL DEFAULT 1 CHECK (gmb_status IN (0, 1)),
    gmb_joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (gmb_group_id, gmb_user_id)
);

CREATE INDEX idx_group_members_user ON group_members (gmb_user_id);

CREATE TABLE expenses (
    exp_id           UUID         PRIMARY KEY,
    exp_group_id     UUID         NOT NULL REFERENCES groups (grp_id),
    exp_payer_id     UUID         NOT NULL REFERENCES users (use_id),
    exp_description  VARCHAR(150) NOT NULL,
    exp_amount_cents BIGINT       NOT NULL CHECK (exp_amount_cents > 0),
    exp_split_type   VARCHAR(20)  NOT NULL CHECK (exp_split_type IN ('EQUAL', 'EXACT', 'PERCENTAGE')),
    exp_date         TIMESTAMPTZ  NOT NULL,
    exp_status       SMALLINT     NOT NULL DEFAULT 1 CHECK (exp_status IN (0, 1)),
    exp_created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    exp_updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_expenses_group ON expenses (exp_group_id);
CREATE INDEX idx_expenses_payer ON expenses (exp_payer_id);

CREATE TABLE expense_shares (
    shr_id           UUID   PRIMARY KEY,
    shr_expense_id   UUID   NOT NULL REFERENCES expenses (exp_id),
    shr_user_id      UUID   NOT NULL REFERENCES users (use_id),
    shr_amount_cents BIGINT NOT NULL CHECK (shr_amount_cents >= 0),
    -- una persona tiene una sola parte en cada gasto
    CONSTRAINT uq_expense_shares_user UNIQUE (shr_expense_id, shr_user_id)
);

CREATE INDEX idx_expense_shares_user ON expense_shares (shr_user_id);
