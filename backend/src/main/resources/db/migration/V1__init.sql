CREATE TABLE users (
    id            BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      TEXT        NOT NULL UNIQUE,
    email         TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE logs (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name           TEXT        NOT NULL,
    file_path           TEXT        NOT NULL,           -- относительный путь к файлу на диске
    player              TEXT,
    loader              TEXT        NOT NULL CHECK (loader IN ('FABRIC', 'NEOFORGE')),
    minecraft_version   TEXT,
    mod_count           INTEGER     NOT NULL DEFAULT 0,
    resource_pack_count INTEGER     NOT NULL DEFAULT 0,
    integrity_status    TEXT        NOT NULL DEFAULT 'NOT_CHECKED'
        CHECK (integrity_status IN ('NOT_CHECKED', 'OK', 'SUSPICIOUS')),
    integrity_issues    JSONB       NOT NULL DEFAULT '[]'::jsonb,
    checker_version     INTEGER,                        -- версия правил проверки
    uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    checked_at          TIMESTAMPTZ
);

CREATE TABLE log_mods (
    log_id  UUID NOT NULL REFERENCES logs(id) ON DELETE CASCADE,
    mod_id  TEXT NOT NULL,
    name    TEXT,
    version TEXT NOT NULL,
    PRIMARY KEY (log_id, mod_id)
);

CREATE TABLE log_resource_packs (
    log_id UUID NOT NULL REFERENCES logs(id) ON DELETE CASCADE,
    name   TEXT NOT NULL,
    PRIMARY KEY (log_id, name)
);

CREATE INDEX idx_logs_user_uploaded ON logs (user_id, uploaded_at DESC);
CREATE INDEX idx_logs_player ON logs (player);
CREATE INDEX idx_log_mods_mod_id ON log_mods (mod_id);
CREATE INDEX idx_log_resource_packs_name ON log_resource_packs (name);
