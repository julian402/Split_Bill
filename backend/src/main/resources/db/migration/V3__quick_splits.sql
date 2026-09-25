-- Cuentas rapidas guardadas: una cuenta dividida al momento, sin grupo ni integrantes registrados.
-- Son de una sola persona (qsp_owner_id). Cada parte lleva el nombre que se escribio en la app
-- ("Ana", "Persona 2"), no un usuario.
CREATE TABLE quick_splits (
    qsp_id             UUID          PRIMARY KEY,
    qsp_owner_id       UUID          NOT NULL REFERENCES users (use_id),
    qsp_description    VARCHAR(150)  NOT NULL,
    qsp_subtotal_cents BIGINT        NOT NULL CHECK (qsp_subtotal_cents > 0),
    qsp_tip_percent    NUMERIC(5, 2) NOT NULL DEFAULT 0 CHECK (qsp_tip_percent >= 0),
    qsp_total_cents    BIGINT        NOT NULL CHECK (qsp_total_cents > 0),
    qsp_split_type     VARCHAR(20)   NOT NULL CHECK (qsp_split_type IN ('EQUAL', 'EXACT', 'PERCENTAGE')),
    qsp_date           TIMESTAMPTZ   NOT NULL,
    qsp_status         SMALLINT      NOT NULL DEFAULT 1 CHECK (qsp_status IN (0, 1)),
    qsp_created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    qsp_updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_quick_splits_owner ON quick_splits (qsp_owner_id);

CREATE TABLE quick_split_shares (
    qss_id             UUID        PRIMARY KEY,
    qss_quick_split_id UUID        NOT NULL REFERENCES quick_splits (qsp_id),
    qss_position       SMALLINT    NOT NULL CHECK (qss_position >= 0),
    qss_name           VARCHAR(80) NOT NULL,
    qss_amount_cents   BIGINT      NOT NULL CHECK (qss_amount_cents >= 0),
    -- cada persona de la cuenta ocupa un solo lugar
    CONSTRAINT uq_quick_split_shares_position UNIQUE (qss_quick_split_id, qss_position)
);
