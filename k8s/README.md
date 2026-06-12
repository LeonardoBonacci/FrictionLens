# FrictionLens — Kubernetes Deployment

Runs the full stack in Kubernetes using [CloudNativePG](https://cloudnative-pg.io/) as the PostgreSQL operator.

## Architecture

```
Internet
   │
[Ingress / frictionlens.local]
   │
[frictionlens-frontend :80]   ← nginx serves React SPA
   │  /api/* proxied to ──────►[frictionlens-backend :8080]  ← Spring Boot
                                        │
                          ┌─────────────┴─────────────┐
                          ▼                           ▼
              [frictionlens-postgres-rw :5432]  [frictionlens-ollama :11434]
              (CloudNativePG Cluster)           (Ollama + llama3.1)
```

## Prerequisites

| Tool | Purpose |
|------|---------|
| `kubectl` | Cluster access |
| `docker` | Build images |
| A container registry | Push images |
| An Ingress controller | Expose the frontend |

## Step 1 — Build and push Docker images

```bash
REGISTRY=leonardobonacci

# PostgreSQL + pgvector (CNPG-compatible)
docker build -t $REGISTRY/frictionlens-postgres:17 -f docker/Dockerfile.postgres .
docker push $REGISTRY/frictionlens-postgres:17

# Spring Boot backend
docker build -t $REGISTRY/frictionlens-backend:latest backend/
docker push $REGISTRY/frictionlens-backend:latest

# React frontend (nginx)
docker build -t $REGISTRY/frictionlens-frontend:latest frontend/
docker push $REGISTRY/frictionlens-frontend:latest
```

## Step 2 — Update image references

All image references are already set to `leonardobonacci/frictionlens-*`. No further substitution needed.

## Step 3 — Install the CloudNativePG operator

```bash
bash k8s/install-operator.sh
```

## Step 4 — Deploy the stack

```bash
# Namespace first
kubectl apply -f k8s/namespace.yaml

# Postgres credentials Secret (edit the password before applying!)
kubectl apply -f k8s/postgres/credentials-secret.yaml

# CNPG Cluster (postgres + pgvector)
kubectl apply -f k8s/postgres/cluster.yaml

# Wait for the cluster to become ready
kubectl wait cluster/frictionlens-postgres \
  -n frictionlens \
  --for=condition=Ready \
  --timeout=180s

# Backend
kubectl apply -f k8s/backend/configmap.yaml
kubectl apply -f k8s/backend/deployment.yaml
kubectl apply -f k8s/backend/service.yaml

# Ollama
kubectl apply -f k8s/ollama/pvc.yaml
kubectl apply -f k8s/ollama/deployment.yaml
kubectl apply -f k8s/ollama/service.yaml

# Frontend
kubectl apply -f k8s/frontend/configmap.yaml
kubectl apply -f k8s/frontend/deployment.yaml
kubectl apply -f k8s/frontend/service.yaml

# Ingress (edit the host: field first)
kubectl apply -f k8s/ingress.yaml
```

Or apply everything at once (order is handled by init containers / readiness probes):

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -R -f k8s/
```

## Step 5 — Access the app

```bash
# Get the Ingress IP
kubectl get ingress frictionlens -n frictionlens

# Add to /etc/hosts (adjust IP)
echo "1.2.3.4  frictionlens.local" | sudo tee -a /etc/hosts
```

Open http://frictionlens.local in your browser.

For a quick local test without DNS:
```bash
kubectl port-forward -n frictionlens svc/frictionlens-frontend 8000:80
# Open http://localhost:8000
```

## Notes

### First startup — Ollama model pull
On the first deployment Ollama will pull `llama3.1:latest` (~4.7 GB). The
backend has retry logic configured, but AI queries will fail until the pull
completes. Monitor progress with:

```bash
kubectl logs -n frictionlens -l app=frictionlens-ollama -f
```

### GPU support
To enable GPU inference, uncomment the `nvidia.com/gpu` resource limit in
[k8s/ollama/deployment.yaml](ollama/deployment.yaml) and ensure the
[NVIDIA device plugin](https://github.com/NVIDIA/k8s-device-plugin) is
installed.

### Secrets management
The credentials in [k8s/postgres/credentials-secret.yaml](postgres/credentials-secret.yaml)
use plaintext `stringData`. For production, replace this with
[Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) or
[External Secrets Operator](https://external-secrets.io/).

### Scaling
The CNPG cluster is set to `instances: 1`. To add read replicas, increase
this value and the operator will set up streaming replication automatically.

## File structure

```
k8s/
├── install-operator.sh       # Installs CloudNativePG operator
├── namespace.yaml
├── ingress.yaml
├── postgres/
│   ├── credentials-secret.yaml
│   └── cluster.yaml          # CloudNativePG Cluster resource
├── backend/
│   ├── configmap.yaml
│   ├── deployment.yaml
│   └── service.yaml
├── frontend/
│   ├── configmap.yaml        # nginx override (optional)
│   ├── deployment.yaml
│   └── service.yaml
└── ollama/
    ├── pvc.yaml
    ├── deployment.yaml
    └── service.yaml

docker/
└── Dockerfile.postgres       # CNPG base image + pgvector extension

backend/
└── Dockerfile                # Spring Boot multi-stage build (Java 25)

frontend/
├── Dockerfile                # Vite build + nginx
└── nginx.conf                # Reverse-proxies /api to backend service
```
