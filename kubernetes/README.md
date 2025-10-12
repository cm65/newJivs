# JiVS Platform - Kubernetes Deployment Guide

This directory contains Kubernetes manifests for deploying the JiVS platform in a production environment with high availability.

## Architecture

- **Backend**: 3-10 replicas with HPA (Horizontal Pod Autoscaler)
- **Frontend**: 3-10 replicas with HPA
- **PostgreSQL**: 3-node StatefulSet with replication
- **Redis**: 3-node StatefulSet with Sentinel
- **Elasticsearch**: 3-node cluster
- **RabbitMQ**: 3-node cluster

## Prerequisites

1. Kubernetes cluster (v1.26+)
2. kubectl configured
3. Helm 3.x installed
4. Storage provisioner (for PersistentVolumeClaims)
5. Ingress controller (NGINX recommended)
6. cert-manager for TLS certificates

## Pre-Deployment Setup

### 1. Install Required Tools

```bash
# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

# Install cert-manager for TLS
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Install metrics-server for HPA
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### 2. Generate Secrets

**IMPORTANT**: Never use default passwords in production!

```bash
# Generate secure passwords
DB_PASSWORD=$(openssl rand -base64 32)
JWT_SECRET=$(openssl rand -base64 64)
ENCRYPTION_KEY=$(openssl rand -base64 32)
REDIS_PASSWORD=$(openssl rand -base64 32)
RABBITMQ_PASSWORD=$(openssl rand -base64 32)

# Create secrets
kubectl create namespace jivs-platform

kubectl create secret generic jivs-backend-secrets \
  --from-literal=DATABASE_PASSWORD="$DB_PASSWORD" \
  --from-literal=JWT_SECRET="$JWT_SECRET" \
  --from-literal=ENCRYPTION_KEY="$ENCRYPTION_KEY" \
  --from-literal=REDIS_PASSWORD="$REDIS_PASSWORD" \
  --from-literal=RABBITMQ_PASSWORD="$RABBITMQ_PASSWORD" \
  --from-literal=AWS_ACCESS_KEY="YOUR_AWS_ACCESS_KEY" \
  --from-literal=AWS_SECRET_KEY="YOUR_AWS_SECRET_KEY" \
  --namespace jivs-platform

# Create TLS certificate (or use cert-manager)
kubectl create secret tls tls-certificate \
  --cert=/path/to/tls.crt \
  --key=/path/to/tls.key \
  --namespace jivs-platform
```

## Deployment Steps

### 1. Create Namespace

```bash
kubectl apply -f namespace.yaml
```

### 2. Create ConfigMaps

```bash
kubectl apply -f configmap.yaml
```

### 3. Deploy Database Layer

```bash
# PostgreSQL
kubectl apply -f postgres-statefulset.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres --timeout=300s -n jivs-platform

# Redis
kubectl apply -f redis-statefulset.yaml

# Wait for Redis to be ready
kubectl wait --for=condition=ready pod -l app=redis --timeout=300s -n jivs-platform
```

### 4. Deploy Application Services

```bash
# Backend
kubectl apply -f backend-deployment.yaml

# Wait for backend to be ready
kubectl wait --for=condition=available deployment/jivs-backend --timeout=300s -n jivs-platform

# Frontend
kubectl apply -f frontend-deployment.yaml
```

### 5. Configure Ingress

```bash
kubectl apply -f ingress.yaml
```

### 6. Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n jivs-platform

# Check services
kubectl get svc -n jivs-platform

# Check ingress
kubectl get ingress -n jivs-platform

# View logs
kubectl logs -f deployment/jivs-backend -n jivs-platform
```

## Scaling

### Manual Scaling

```bash
# Scale backend
kubectl scale deployment jivs-backend --replicas=5 -n jivs-platform

# Scale frontend
kubectl scale deployment jivs-frontend --replicas=5 -n jivs-platform
```

### Auto-scaling (HPA)

HPA is automatically configured for backend and frontend:
- **Backend**: 3-10 replicas based on CPU (70%) and Memory (80%)
- **Frontend**: 3-10 replicas based on CPU (70%)

Check HPA status:
```bash
kubectl get hpa -n jivs-platform
```

## Monitoring

### Check Pod Status

```bash
kubectl get pods -n jivs-platform -o wide
```

### View Logs

```bash
# Backend logs
kubectl logs -f deployment/jivs-backend -n jivs-platform

# Frontend logs
kubectl logs -f deployment/jivs-frontend -n jivs-platform

# PostgreSQL logs
kubectl logs -f postgres-0 -n jivs-platform
```

### Metrics

```bash
# Pod resource usage
kubectl top pods -n jivs-platform

# Node resource usage
kubectl top nodes
```

## Backup and Restore

### PostgreSQL Backup

```bash
# Create backup
kubectl exec -it postgres-0 -n jivs-platform -- \
  pg_dump -U jivs jivs > backup-$(date +%Y%m%d-%H%M%S).sql

# Restore from backup
kubectl exec -i postgres-0 -n jivs-platform -- \
  psql -U jivs jivs < backup.sql
```

### Redis Backup

```bash
# Trigger RDB snapshot
kubectl exec -it redis-0 -n jivs-platform -- redis-cli BGSAVE

# Copy RDB file
kubectl cp jivs-platform/redis-0:/data/dump.rdb ./redis-backup.rdb
```

## Rolling Updates

### Update Backend

```bash
# Update image
kubectl set image deployment/jivs-backend backend=jivs-backend:v2.0.0 -n jivs-platform

# Check rollout status
kubectl rollout status deployment/jivs-backend -n jivs-platform

# Rollback if needed
kubectl rollout undo deployment/jivs-backend -n jivs-platform
```

### Update Frontend

```bash
kubectl set image deployment/jivs-frontend frontend=jivs-frontend:v2.0.0 -n jivs-platform
kubectl rollout status deployment/jivs-frontend -n jivs-platform
```

## Troubleshooting

### Pod Not Starting

```bash
# Describe pod to see events
kubectl describe pod <pod-name> -n jivs-platform

# Check logs
kubectl logs <pod-name> -n jivs-platform --previous
```

### Database Connection Issues

```bash
# Test database connection
kubectl exec -it postgres-0 -n jivs-platform -- psql -U jivs -d jivs -c "SELECT version();"

# Check service DNS
kubectl exec -it <backend-pod> -n jivs-platform -- nslookup postgres-service
```

### Ingress Not Working

```bash
# Check ingress controller logs
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller

# Check ingress configuration
kubectl describe ingress jivs-ingress -n jivs-platform
```

### High Memory/CPU Usage

```bash
# Check resource usage
kubectl top pods -n jivs-platform

# Increase resources if needed
kubectl edit deployment jivs-backend -n jivs-platform
```

## Maintenance

### Drain Node for Maintenance

```bash
# Cordon node (prevent new pods)
kubectl cordon <node-name>

# Drain node (evict pods gracefully)
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# Uncordon when done
kubectl uncordon <node-name>
```

### Delete Evicted Pods

```bash
kubectl get pods -n jivs-platform | grep Evicted | awk '{print $1}' | xargs kubectl delete pod -n jivs-platform
```

## Security Best Practices

1. **Secrets Management**: Use external secret management (AWS Secrets Manager, HashiCorp Vault)
2. **Network Policies**: Restrict pod-to-pod communication
3. **RBAC**: Implement least-privilege access
4. **Image Scanning**: Scan container images for vulnerabilities
5. **Pod Security Standards**: Enable PSS enforcement
6. **TLS Everywhere**: Use TLS for all communications
7. **Regular Updates**: Keep Kubernetes and applications updated

## Production Checklist

- [ ] Secrets generated and stored securely
- [ ] TLS certificates configured
- [ ] Ingress controller installed
- [ ] Metrics server installed
- [ ] Backup strategy implemented
- [ ] Monitoring and alerting configured
- [ ] Disaster recovery plan documented
- [ ] Resource limits configured
- [ ] HPA configured and tested
- [ ] PodDisruptionBudgets configured
- [ ] Network policies configured
- [ ] Health checks configured
- [ ] Logging aggregation setup

## Support

For issues or questions, contact the platform team at platform@example.com
