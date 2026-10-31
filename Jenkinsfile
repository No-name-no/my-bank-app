stage('Deploy Kafka (Strimzi)') {
    steps {
        sh '''
            # 1. CRD (Helm их не ставит)
            helm pull oci://quay.io/strimzi-helm/strimzi-kafka-operator \
              --version 1.3.0 --untar --untardir /tmp
            kubectl apply --server-side --filename /tmp/strimzi-kafka-operator/crds --recursive

            # 2. Оператор
            helm upgrade --install strimzi-operator oci://quay.io/strimzi-helm/strimzi-kafka-operator \
              --version 1.3.0 --namespace kafka --create-namespace \
              --wait --timeout 10m

            # 3. Kafka-кластер
            kubectl apply -f k8s/strimzi/
            kubectl wait kafka/bank-kafka -n kafka --for=condition=Ready --timeout=10m
        '''
    }
}