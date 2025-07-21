#/bin/bash

eval $(minikube docker-env)
sbt 'docker:publishLocal'
kubectl apply -f simulator.yaml