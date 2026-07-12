#!/bin/bash

set -e

NAMESPACE="online-store"

echo "Building Docker image for minikube..."
minikube image build -t user-service:latest .

echo "Applying Kubernetes manifests to namespace $NAMESPACE..."
kubectl apply -f k8s/ -n $NAMESPACE

echo "Waiting for postgres-users to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres-users -n $NAMESPACE --timeout=120s

echo "Waiting for redis to be ready..."
kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=120s

echo "Checking if auth-service is available..."
if kubectl get deployment auth-service -n $NAMESPACE &> /dev/null; then
    echo "Waiting for auth-service to be ready..."
    kubectl wait --for=condition=available deployment/auth-service -n $NAMESPACE --timeout=120s
else
    echo "WARNING: auth-service deployment not found in namespace $NAMESPACE"
    echo "Make sure auth-service is deployed before user-service can function properly"
fi

echo "Applying user-service deployment and service..."
kubectl apply -f k8s/user-service-deployment.yaml -n $NAMESPACE
kubectl apply -f k8s/user-service-svc.yaml -n $NAMESPACE

echo "Waiting for user-service rollout to complete..."
kubectl rollout status deployment/user-service -n $NAMESPACE

echo "Deployment completed successfully!"
echo "User service is now running in namespace $NAMESPACE"
