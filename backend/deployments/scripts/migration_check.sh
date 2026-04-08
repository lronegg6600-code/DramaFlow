#!/bin/sh
set -eu

MIGRATION_DIR="${MIGRATION_DIR:-sql/migrations}"

echo "[migration-check] checking rollback pairs in ${MIGRATION_DIR}"

for file in "${MIGRATION_DIR}"/*.sql; do
  name="$(basename "${file}")"
  case "${name}" in
    *_rollback.sql)
      continue
      ;;
    001_init.sql)
      continue
      ;;
  esac

  rollback="${name%.sql}_rollback.sql"
  if [ ! -f "${MIGRATION_DIR}/${rollback}" ]; then
    echo "[migration-check] missing rollback for ${name}"
    exit 1
  fi
done

echo "[migration-check] checking high risk markers"
for file in "${MIGRATION_DIR}"/*.sql; do
  name="$(basename "${file}")"
  case "${name}" in
    *_rollback.sql)
      continue
      ;;
  esac

  if grep -Eq "DROP COLUMN|DROP TABLE|ALTER COLUMN|DELETE FROM" "${file}"; then
    echo "[migration-check] ${name} contains potentially high-risk statements; require manual review"
  fi
done

echo "[migration-check] ok"
