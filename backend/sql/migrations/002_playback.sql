CREATE TABLE IF NOT EXISTS playback_sessions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    drama_id TEXT NOT NULL REFERENCES dramas(id) ON DELETE CASCADE,
    episode_id TEXT NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    playback_mode TEXT NOT NULL,
    media_path TEXT NOT NULL,
    session_status TEXT NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NULL,
    client_app TEXT NOT NULL,
    client_platform TEXT NOT NULL,
    client_version TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dev_entitlements (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entitlement_type TEXT NOT NULL,
    drama_id TEXT NULL REFERENCES dramas(id) ON DELETE CASCADE,
    episode_id TEXT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    source TEXT NOT NULL,
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_playback_sessions_user_id_created_at ON playback_sessions(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_playback_sessions_episode_id_created_at ON playback_sessions(episode_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dev_entitlements_user_id_active ON dev_entitlements(user_id, active);
