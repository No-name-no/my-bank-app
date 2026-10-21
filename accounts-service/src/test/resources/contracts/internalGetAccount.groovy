package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("GET /internal/accounts/{login} для сервисных вызовов")

    request {
        method GET()
        url "/internal/accounts/login1"
        headers {
            header("Authorization", "Bearer service-jwt")
        }
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                login: "login1",
                firstName: "Федя",
                lastName: "Сумкин",
                birthDate: "1990-01-01",
                balance: 1000.00
        )
    }
}