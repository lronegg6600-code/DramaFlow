INSERT INTO billing_catalog_products (product_id, display_name, benefit_snapshot, active)
VALUES
    ('premium_access', 'DramaFlow Premium', '["premium episodes","resume sync","early drops"]'::jsonb, TRUE)
ON CONFLICT (product_id) DO UPDATE
SET display_name = EXCLUDED.display_name,
    benefit_snapshot = EXCLUDED.benefit_snapshot,
    active = EXCLUDED.active,
    updated_at = NOW();

INSERT INTO billing_catalog_base_plans (product_id, base_plan_id, region_code, currency_code, billing_period, tags, active)
VALUES
    ('premium_access', 'monthly', 'US', 'USD', 'P1M', '["default","subscription"]'::jsonb, TRUE),
    ('premium_access', 'quarterly', 'US', 'USD', 'P3M', '["subscription"]'::jsonb, TRUE)
ON CONFLICT (product_id, base_plan_id, region_code) DO UPDATE
SET currency_code = EXCLUDED.currency_code,
    billing_period = EXCLUDED.billing_period,
    tags = EXCLUDED.tags,
    active = EXCLUDED.active,
    updated_at = NOW();

INSERT INTO billing_catalog_offers (product_id, base_plan_id, offer_id, offer_token, region_code, currency_code, billing_period, tags, active)
VALUES
    ('premium_access', 'monthly', 'trial_intro', 'monthly_trial_intro_token', 'US', 'USD', 'P1M', '["trial","intro"]'::jsonb, TRUE),
    ('premium_access', 'quarterly', 'save_23', 'quarterly_save_23_token', 'US', 'USD', 'P3M', '["discount"]'::jsonb, TRUE)
ON CONFLICT (product_id, base_plan_id, offer_id, region_code) DO UPDATE
SET offer_token = EXCLUDED.offer_token,
    currency_code = EXCLUDED.currency_code,
    billing_period = EXCLUDED.billing_period,
    tags = EXCLUDED.tags,
    active = EXCLUDED.active,
    updated_at = NOW();
