# DramaFlow Admin Console

Phase 7 keeps the existing admin console and adds test and release gates around it.

## Stack

- Next.js App Router
- TypeScript
- Tailwind CSS
- TanStack Query
- React Hook Form + Zod
- Vitest

## Commands

```bash
corepack pnpm install
corepack pnpm dev
corepack pnpm typecheck
corepack pnpm test
corepack pnpm test:pages
```

## Coverage focus

- login success / failure
- dramas list and drama detail
- feed-config draft / publish / rollback
- purchases / entitlements / RTDN dangerous actions
- playback / audit list rendering

## Required env

- `NEXT_PUBLIC_ADMIN_API_BASE_URL=http://localhost:8088`
