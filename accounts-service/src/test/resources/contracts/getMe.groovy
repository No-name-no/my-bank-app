package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("GET /accounts/me возвращает аккаунт текущего пользователя")

    request {
        method GET()
        url "/accounts/me"
        headers {
            header("Authorization", "Bearer test-jwt")
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
        bodyMatchers {
            jsonPath('$.login', byRegex('[a-zA-Z0-9]+'))
            jsonPath('$.balance', byRegex('[0-9]+(\\.[0-9]+)?'))
        }
    }
}