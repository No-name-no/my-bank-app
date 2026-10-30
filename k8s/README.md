# Kafka в локальном Kubernetes (kind + Strimzi)

## 1. Создать кластер
kind create cluster --config k8s/kind-config.yaml --name bank-kafka

## 2. Установить CRD (Helm их не ставит)
helm pull oci://quay.io/strimzi-helm/strimzi-kafka-operator --version 1.3.0 --untar
kubectl apply --server-side --filename ./strimzi-kafka-operator/crds --recursive

## 3. Установить оператор
helm upgrade --install strimzi-operator oci://quay.io/strimzi-helm/strimzi-kafka-operator `
--version 1.3.0 --namespace kafka --create-namespace --wait --timeout 10m

## 4. Развернуть Kafka-кластер
kubectl apply -f k8s/strimzi/
kubectl wait kafka/bank-kafka -n kafka --for=condition=Ready --timeout=10m

## 5. Проверить
kubectl get pods -n kafka
kubectl get pvc -n kafka
kubectl get svc -n kafka

## 6. Запустить docker-compose
docker compose up -d --build
# приложения создадут топик bank-notifications через NewTopic

## 7. Проверить топик
kubectl exec -n kafka bank-kafka-dual-role-0 -- `
bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

## 8. Проверить сохранность при перезапуске
kubectl delete pods -n kafka -l strimzi.io/cluster=bank-kafka
kubectl wait pods -n kafka -l strimzi.io/cluster=bank-kafka --for=condition=Ready --timeout=5m
kubectl exec -n kafka bank-kafka-dual-role-0 -- `
bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

## 9. Удалить кластер
kind delete cluster --name bank-kafka