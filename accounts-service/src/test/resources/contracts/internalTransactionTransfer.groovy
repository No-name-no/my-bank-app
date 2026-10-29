package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("POST /internal/accounts/transaction — перевод между счетами")

    request {
        method POST()
        url "/internal/accounts/transaction"
        headers {
            header("Authorization", "Bearer service-jwt")
            contentType(applicationJson())
        }
        body(
                kind: "TRANSFER",
                amount: 100.00,
                srcLogin: "login1",
                dstLogin: "login2",
                transactionId: "a8e99587-0e81-4b74-8d18-2ba6791d9d1b"
        )
    }

    response {
        status OK()
    }
}