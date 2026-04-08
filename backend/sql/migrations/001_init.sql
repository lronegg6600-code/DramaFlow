CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    anonymous_device_id TEXT UNIQUE NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dramas (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    short_description TEXT NOT NULL,
    long_description TEXT NOT NULL,
    poster_url TEXT NOT NULL,
    cover_url TEXT NOT NULL,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    region TEXT NOT NULL,
    language TEXT NOT NULL,
    publish_status TEXT NOT NULL,
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS episodes (
    id TEXT PRIMARY KEY,
    drama_id TEXT NOT NULL REFERENCES dramas(id) ON DELETE CASCADE,
    episode_no INTEGER NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL,
    preview_seconds INTEGER NOT NULL DEFAULT 0,
    is_premium BOOLEAN NOT NULL DEFAULT FALSE,
    stream_key_placeholder TEXT NOT NULL,
    publish_status TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (drama_id, episode_no)
);

CREATE TABLE IF NOT EXISTS watch_progress (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    drama_id TEXT NOT NULL REFERENCES dramas(id) ON DELETE CASCADE,
    episode_id TEXT NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    position_seconds INTEGER NOT NULL,
    duration_seconds INTEGER NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, episode_id)
);

CREATE TABLE IF NOT EXISTS watch_history (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    drama_id TEXT NOT NULL REFERENCES dramas(id) ON DELETE CASCADE,
    episode_id TEXT NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    watched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, episode_id)
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_episodes_drama_id_sort_order ON episodes(drama_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_watch_progress_user_id_updated_at ON watch_progress(user_id, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_watch_history_user_id_watched_at ON watch_history(user_id, watched_at DESC);
