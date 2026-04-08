INSERT INTO admin_users (id, email, password_hash, display_name, role, status)
VALUES (
    'admin-bootstrap',
    'admin@dramaflow.local',
    '$2a$10$AaejowK7RjV7CjzM7M7S/ePUNsGrD/qjUKPTNroTsNMndvrYVF/3C',
    'DramaFlow Admin',
    'super_admin',
    'active'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO feed_home_configs (
    id,
    region,
    language,
    config_version,
    draft_payload,
    published_payload,
    published_at,
    published_by
)
VALUES (
    'feed-home-default-en-us',
    'US',
    'en',
    1,
    '{
      "featured": [],
      "trending": [],
      "recommended": [],
      "banners": [],
      "status": "draft"
    }'::jsonb,
    '{
      "featured": [],
      "trending": [],
      "recommended": [],
      "banners": [],
      "status": "published"
    }'::jsonb,
    NOW(),
    'admin-bootstrap'
)
ON CONFLICT (region, language) DO NOTHING;
