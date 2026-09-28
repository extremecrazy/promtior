CREATE TABLE rooms (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255) NOT NULL UNIQUE,
    max_capacity       INTEGER      NOT NULL,
    active             BOOLEAN      NOT NULL DEFAULT true,
    created_date       TIMESTAMP    NOT NULL DEFAULT now(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username           VARCHAR(255) NOT NULL,
    password           VARCHAR(255) NOT NULL,
    name               VARCHAR(255) NOT NULL,
    role               VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_date       TIMESTAMP    NOT NULL DEFAULT now(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE booking (
    booking_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255) NOT NULL,
    room_id            UUID         NOT NULL REFERENCES rooms (id),
    user_id            UUID         NOT NULL REFERENCES users (id),
    start_time         TIMESTAMP    NOT NULL,
    end_time           TIMESTAMP    NOT NULL,
    attendee_count     INTEGER      NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    created_date       TIMESTAMP    NOT NULL DEFAULT now(),
    last_modified_date TIMESTAMP    NOT NULL DEFAULT now()
);


-- Configuración global de reservas (fila única): duración de cada slot y
-- cantidad máxima de slots por reserva, seteables por un admin.
CREATE TABLE booking_settings (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_minutes       INTEGER   NOT NULL,
    max_slots          INTEGER   NOT NULL,
    created_date       TIMESTAMP NOT NULL DEFAULT now(),
    last_modified_date TIMESTAMP NOT NULL DEFAULT now()
);
