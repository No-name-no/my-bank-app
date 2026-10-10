package org.mnuykin.mapper;

import org.mnuykin.dto.AccountDto;
import org.mnuykin.dto.AccountShortDto;
import org.mnuykin.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
    public AccountDto toDto(Account a) {
        return new AccountDto(
                a.getLogin(),
                a.getFirstName(),
                a.getLastName(),
                a.getBirthDate(),
                a.getBalance()
        );
    }

    public AccountShortDto toShortDto(Account a) {
        return new AccountShortDto(
                a.getLogin(), a.getFirstName(), a.getLastName()
        );
    }
}
