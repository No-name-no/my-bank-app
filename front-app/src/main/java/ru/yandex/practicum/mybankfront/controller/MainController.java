package ru.yandex.practicum.mybankfront.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountShortView;
import ru.yandex.practicum.mybankfront.controller.dto.AccountUpdateRq;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;
import ru.yandex.practicum.mybankfront.service.AccountsClient;
import ru.yandex.practicum.mybankfront.service.CashClient;
import ru.yandex.practicum.mybankfront.service.TransferClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Контроллер main.html.
 *
 * Используемая модель для main.html:
 *      model.addAttribute("name", name);
 *      model.addAttribute("birthdate", birthdate.format(DateTimeFormatter.ISO_DATE));
 *      model.addAttribute("sum", sum);
 *      model.addAttribute("accounts", accounts);
 *      model.addAttribute("errors", errors);
 *      model.addAttribute("info", info);
 *
 * Поля модели:
 *      name - Фамилия Имя текущего пользователя, String (обязательное)
 *      birthdate - дата рождения текущего пользователя, String в формате 'YYYY-MM-DD' (обязательное)
 *      sum - сумма на счету текущего пользователя, Integer (обязательное)
 *      accounts - список аккаунтов, которым можно перевести деньги, List<AccountDto> (обязательное)
 *      errors - список ошибок после выполнения действий, List<String> (не обязательное)
 *      info - строка успешности после выполнения действия, String (не обязательное)
 *
 * С примерами использования можно ознакомиться в тестовом классе заглушке AccountStub
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class MainController {

    private static final Pattern ERROR_PATTERN =
            Pattern.compile("\"error\"\\s*:\\s*\"([^\"]+)\"");

    private final AccountsClient accountsClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    /**
     * GET /.
     * Редирект на GET /account
     */
    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    /**
     * GET /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для получения данных аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     */
    @GetMapping("/account")
    public String getAccount(Model model) {
        loadModel(model);

        return "main";
    }

    /**
     * POST /account.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для изменения данных текущего пользователя по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Изменяемые данные:
     * 1. name - Фамилия Имя
     * 2. birthdate - дата рождения в формате YYYY-DD-MM
     */
    @PostMapping("/account")
    public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate
    ) {
        String[] firstLast = parseFirstLastName(name);
        AccountUpdateRq rq = new AccountUpdateRq(firstLast[0], firstLast[1], birthdate);

        try {
            accountsClient.updateMe(rq);
            model.addAttribute("info", "Данные сохранены");
        } catch (RestClientException e) {
            model.addAttribute("errors", List.of(extractError(e)));
        }

        loadModel(model);

        return "main";
    }

    /**
     * POST /cash.
     * Что нужно сделать:
     * 1. Сходить в сервис cash через Gateway API для снятия/пополнения счета текущего аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Параметры:
     * 1. value - сумма списания
     * 2. action - GET (снять), PUT (пополнить)
     */
    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action
            ) {
        BigDecimal amount = BigDecimal.valueOf(value);
        try {
            switch (action) {
                case PUT -> cashClient.deposit(amount);
                case GET -> cashClient.withdraw(amount);
            }
            model.addAttribute("info", switch (action) {
                case PUT -> "Счёт пополнен на " + value + " руб.";
                case GET -> "Со счёта снято " + value + " руб.";
            });
        } catch (RestClientException e) {
            model.addAttribute("errors", List.of(extractError(e)));
        }

        loadModel(model);
        return "main";
    }

    /**
     * POST /transfer.
     * Что нужно сделать:
     * 1. Сходить в сервис accounts через Gateway API для перевода со счета текущего аккаунта на счет другого аккаунта по REST
     * 2. Заполнить модель main.html полученными из ответа данными
     * 3. Текущего пользователя можно получить из контекста Security
     *
     * Параметры:
     * 1. value - сумма списания
     * 2. login - логин пользователя получателя
     */
    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("login") String login
    ) {
        BigDecimal amount = BigDecimal.valueOf(value);
        try {
            transferClient.transfer(login, amount);
            model.addAttribute("info",
                    "Перевод " + value + " руб. на счёт " + login + " выполнен");
        } catch (RestClientException e) {
            model.addAttribute("errors", List.of(extractError(e)));
        }

        loadModel(model);

        return "main";
    }

    private void loadModel(Model model) {
        AccountDto me = accountsClient.getMe();
        List<AccountShortView> others = accountsClient.getOthers().stream()
                .map(b -> new AccountShortView(
                        b.getLogin(),
                        b.getFirstName() + " " + b.getLastName()))
                .toList();

        model.addAttribute("name", me.getFirstName() + " " + me.getLastName());
        model.addAttribute("birthdate", me.getBirthDate() != null
                ? me.getBirthDate().toString() : "");
        model.addAttribute("sum", me.getBalance() != null
                ? me.getBalance().intValue() : 0);
        model.addAttribute("accounts", others);
    }

    private static String[] parseFirstLastName(String name) {
        String[] parts = name.trim().split("\\s+", 2);
        if (parts.length < 2) {
            return new String[]{parts[0], ""};
        }
        return new String[]{parts[1], parts[0]};
    }

    private static String extractError(RestClientException e) {
        if (e instanceof RestClientResponseException re) {
            String body = re.getResponseBodyAsString();
            var m = ERROR_PATTERN.matcher(body);
            if (m.find()) {
                return m.group(1);
            }
            return "HTTP " + re.getStatusCode().value() + ": " + re.getStatusText();
        }
        return "Сервис временно недоступен";
    }

}
