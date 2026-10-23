package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("POST /internal/accounts/transaction — пополнение счёта")

    request {
        method POST()
        url "/internal/accounts/transaction"
        headers {
            header("Authorization", "Bearer service-jwt")
            contentType(applicationJson())
        }
        body(
                kind: "CASH_DEPOSIT",
                amount: 100.00,
                srcLogin: "login1"
        )
    }

    response {
        status OK()
    }
}