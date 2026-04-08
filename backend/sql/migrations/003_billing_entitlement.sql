CREATE TABLE IF NOT EXISTS billing_catalog_products (
    product_id TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    benefit_snapshot JSONB NOT NULL DEFAULT '[]'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS billing_catalog_base_plans (
    product_id TEXT NOT NULL REFERENCES billing_catalog_products(product_id) ON DELETE CASCADE,
    base_plan_id TEXT NOT NULL,
    region_code TEXT NOT NULL,
    currency_code TEXT NOT NULL,
    billing_period TEXT NOT NULL,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, base_plan_id, region_code)
);

CREATE TABLE IF NOT EXISTS billing_catalog_offers (
    product_id TEXT NOT NULL REFERENCES billing_catalog_products(product_id) ON DELETE CASCADE,
    base_plan_id TEXT NOT NULL,
    offer_id TEXT NOT NULL,
    offer_token TEXT,
    region_code TEXT NOT NULL,
    currency_code TEXT NOT NULL,
    billing_period TEXT NOT NULL,
    tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (product_id, base_plan_id, offer_id, region_code)
);

CREATE TABLE IF NOT EXISTS billing_purchase_records (
    purchase_token TEXT PRIMARY KEY,
    linked_purchase_token TEXT,
    order_id TEXT,
    user_id TEXT,
    package_name TEXT NOT NULL,
    product_id TEXT NOT NULL,
    base_plan_id TEXT,
    offer_id TEXT,
    purchase_state TEXT NOT NULL,
    acknowledgement_state TEXT NOT NULL,
    external_account_identifiers JSONB,
    obfuscated_account_id TEXT,
    obfuscated_profile_id TEXT,
    start_time TIMESTAMPTZ,
    expiry_time TIMESTAMPTZ,
    auto_renew_enabled BOOLEAN,
    cancel_reason TEXT,
    line_items_snapshot JSONB NOT NULL DEFAULT '[]'::jsonb,
    raw_payload_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    raw_api_version TEXT NOT NULL,
    source TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_billing_purchase_records_user_id ON billing_purchase_records(user_id);
CREATE INDEX IF NOT EXISTS idx_billing_purchase_records_product_id ON billing_purchase_records(product_id);
CREATE INDEX IF NOT EXISTS idx_billing_purchase_records_expiry_time ON billing_purchase_records(expiry_time);
CREATE INDEX IF NOT EXISTS idx_billing_purchase_records_linked_purchase_token ON billing_purchase_records(linked_purchase_token);

CREATE TABLE IF NOT EXISTS billing_orders (
    billing_order_id UUID PRIMARY KEY,
    user_id TEXT NOT NULL,
    purchase_token TEXT NOT NULL REFERENCES billing_purchase_records(purchase_token) ON DELETE CASCADE,
    product_id TEXT NOT NULL,
    amount_micros BIGINT,
    currency_code TEXT,
    country_code TEXT,
    platform TEXT NOT NULL,
    state TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (purchase_token, state)
);

CREATE INDEX IF NOT EXISTS idx_billing_orders_user_id ON billing_orders(user_id);

CREATE TABLE IF NOT EXISTS billing_rtdn_events (
    message_id TEXT PRIMARY KEY,
    package_name TEXT NOT NULL,
    event_type TEXT NOT NULL,
    purchase_token TEXT,
    subscription_notification_type INTEGER,
    one_time_product_notification_type INTEGER,
    event_time TIMESTAMPTZ NOT NULL,
    payload_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    processed_state TEXT NOT NULL DEFAULT 'pending',
    processed_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_billing_rtdn_events_purchase_token ON billing_rtdn_events(purchase_token);
CREATE INDEX IF NOT EXISTS idx_billing_rtdn_events_processed_state ON billing_rtdn_events(processed_state);

CREATE TABLE IF NOT EXISTS entitlements (
    entitlement_id UUID PRIMARY KEY,
    user_id TEXT NOT NULL,
    entitlement_type TEXT NOT NULL,
    product_id TEXT NOT NULL,
    scope_type TEXT NOT NULL,
    scope_ref TEXT,
    state TEXT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    source_purchase_token TEXT NOT NULL,
    last_synced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_entitlements_unique_source_scope
ON entitlements (source_purchase_token, scope_type, COALESCE(scope_ref, ''), entitlement_type);
CREATE INDEX IF NOT EXISTS idx_entitlements_user_id ON entitlements(user_id);
CREATE INDEX IF NOT EXISTS idx_entitlements_state ON entitlements(state);
CREATE INDEX IF NOT EXISTS idx_entitlements_product_id ON entitlements(product_id);

CREATE TABLE IF NOT EXISTS entitlement_audit_logs (
    audit_id UUID PRIMARY KEY,
    entitlement_id UUID REFERENCES entitlements(entitlement_id) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    action TEXT NOT NULL,
    source_purchase_token TEXT,
    state_before TEXT,
    state_after TEXT,
    reason TEXT NOT NULL,
    payload_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_entitlement_audit_logs_user_id ON entitlement_audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_entitlement_audit_logs_purchase_token ON entitlement_audit_logs(source_purchase_token);
