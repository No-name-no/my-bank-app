package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("POST /internal/accounts/transaction — недостаточно средств")

    request {
        method POST()
        url "/internal/accounts/transaction"
        headers {
            header("Authorization", "Bearer service-jwt")
            contentType(applicationJson())
        }
        body(
                kind: "CASH_WITHDRAWAL",
                amount: 99999.00,
                srcLogin: "login1",
                transactionId: "a8e99587-0e81-4b74-8d18-2ba6791d9d1b"
        )
    }

    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(
                "code":"InsufficientFunds","message":"Недостаточно средств на счёте login1"
        )
    }
}