INSERT INTO accounts.accounts (login, first_name, last_name, birth_date, balance)
VALUES ('login1', 'Федя', 'Сумкин', '1990-01-01', 1000.00),
       ('login2', 'Пётр', 'Петров', '1985-05-05', 500.00)
       ('login3', 'Дмитрий', 'Петров', '1985-05-05', 0.00)
    ON CONFLICT (login) DO NOTHING;