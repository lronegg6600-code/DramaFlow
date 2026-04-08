SHELL := /bin/sh

COMPOSE_FILE := deployments/docker-compose/docker-compose.yml
MIGRATION_DIR := sql/migrations
SEED_DIR := sql/seeds

ifneq (,$(wildcard .env))
include .env
export
endif

.PHONY: dev-up dev-down compose-config migrate-up migrate-down migrate-status migrate-check seed seed-reset pre-deploy post-deploy rollback rollback-verify staging-deploy canary-check release-watch test contract-test integration-test release-smoke lint

dev-up:
	docker compose -f $(COMPOSE_FILE) up --build

dev-down:
	docker compose -f $(COMPOSE_FILE) down -v

compose-config:
	docker compose -f $(COMPOSE_FILE) config

migrate-up:
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(MIGRATION_DIR)/001_init.sql
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(MIGRATION_DIR)/002_playback.sql
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(MIGRATION_DIR)/003_billing_entitlement.sql
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(MIGRATION_DIR)/004_admin_console.sql

migrate-down:
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(MIGRATION_DIR)/004_admin_console_rollback.sql
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(MIGRATION_DIR)/003_billing_entitlement_rollback.sql
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(MIGRATION_DIR)/002_playback_rollback.sql

migrate-status:
	@ls -1 $(MIGRATION_DIR)

migrate-check:
	sh deployments/scripts/migration_check.sh

seed:
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(SEED_DIR)/001_seed.sql
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(SEED_DIR)/002_playback_seed.sql
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(SEED_DIR)/003_billing_entitlement_seed.sql
	psql "$$DRAMAFLOW_POSTGRES_DSN" -f $(SEED_DIR)/004_admin_console_seed.sql

seed-reset:
	psql "$$DRAMAFLOW_POSTGRES_DSN" -c "TRUNCATE TABLE admin_sessions, admin_audit_logs, feed_home_configs, billing_rtdn_events, billing_orders, billing_purchase_records, entitlement_audit_logs, entitlements, playback_sessions, watch_progress RESTART IDENTITY CASCADE;"
	$(MAKE) seed

pre-deploy:
	sh deployments/scripts/pre_deploy_check.sh

post-deploy:
	sh deployments/scripts/post_deploy_verify.sh

rollback:
	sh deployments/scripts/rollback.sh

rollback-verify:
	sh deployments/scripts/rollback_verify.sh

staging-deploy:
	sh deployments/scripts/staging_deploy.sh

canary-check:
	sh deployments/scripts/canary_check.sh

release-watch:
	sh deployments/scripts/release_watch.sh

test:
	go test ./...

contract-test:
	node tests/contract/check_openapi.mjs

integration-test:
	node tests/integration/smoke.mjs

release-smoke:
	node tests/release/smoke_release.mjs

lint:
	go vet ./...
