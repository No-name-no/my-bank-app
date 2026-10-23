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
                srcLogin: "login1"
        )
    }

    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(
                error: "Недостаточно средств на счёте login1"
        )
    }
}