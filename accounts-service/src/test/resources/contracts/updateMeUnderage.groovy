package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("PUT /accounts/me возвращает 400, если возраст меньше 18")

    request {
        method PUT()
        url "/accounts/me"
        headers {
            header("Authorization", "Bearer test-jwt")
            contentType(applicationJson())
        }
        body(
                firstName: "Фёдор",
                lastName: "Сумкин",
                birthday: "2015-01-01"
        )
    }

    response {
        status BAD_REQUEST()
        headers {
            contentType(applicationJson())
        }
        body(
                error: "Возраст должен быть не менее 18 лет"
        )
    }
}