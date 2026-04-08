# Kubernetes Access Setup

## Required Inputs From Platform

- kubeconfig or cluster access method
- context names
- target namespaces
- RBAC role binding
- rollout/rollback allowed verbs

## Naming Convention

- staging context: `dramaflow-staging`
- production context: `dramaflow-production`
- staging namespace: `dramaflow-staging`
- production namespace: `dramaflow-production`

## Minimum RBAC

- get/list/watch pods
- get/list/watch deployments
- update/patch deployments
- rollout undo on target services
- read logs in target namespace

## Verification

- `kubectl config current-context`
- `kubectl auth can-i get deployments -n <namespace>`
- `kubectl auth can-i patch deployments -n <namespace>`
- `kubectl auth can-i get pods/log -n <namespace>`

## Current Gap

As of `2026-04-08`, `kubectl` exists locally but no current context is configured. This is a platform blocker, not a code blocker.
