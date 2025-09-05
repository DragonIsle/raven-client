#/bin/bash

eval $(minikube docker-env)
sbt 'docker:publishLocal'
#kubectl create configmap simulator-config --from-file=application.conf=in-cluster.conf
kubectl apply -f simulator.yaml