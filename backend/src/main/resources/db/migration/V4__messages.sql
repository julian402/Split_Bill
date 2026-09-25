-- Chat de cada grupo: mensajes de texto entre los integrantes que tienen cuenta.
-- No se editan ni se borran. msg_sent_at es la hora del celular al escribirlo (el orden que ve la
-- persona); msg_created_at es la hora en que llego al servidor (la que usa la app para pedir lo nuevo).
CREATE TABLE messages (
    msg_id         UUID          PRIMARY KEY,
    msg_group_id   UUID          NOT NULL REFERENCES groups (grp_id),
    msg_sender_id  UUID          NOT NULL REFERENCES users (use_id),
    msg_text       VARCHAR(1000) NOT NULL CHECK (length(trim(msg_text)) > 0),
    msg_sent_at    TIMESTAMPTZ   NOT NULL,
    msg_created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_messages_group_created ON messages (msg_group_id, msg_created_at);
