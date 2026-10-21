package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("PUT /accounts/me обновляет имя, фамилию и дату рождения")

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
                birthday: "1990-01-01"
        )
    }

    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                login: "login1",
                firstName: "Фёдор",
                lastName: "Сумкин",
                birthDate: "1990-01-01",
                balance: 1000.00
        )
    }
}