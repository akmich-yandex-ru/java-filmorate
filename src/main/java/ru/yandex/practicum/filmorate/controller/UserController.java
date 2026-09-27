package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Integer, User> users = new HashMap<>();
    private int currentId = 0;

    @GetMapping
    public Collection<User> findAll() {
        log.info("Получен запрос на получение списка всех пользователей. Текущее количество: {}", users.size());
        return users.values();

    }

    @PostMapping
    public User add(@Valid @RequestBody User newUser) {
        log.info("Получен запрос на добавление пользователя: {}", newUser);

        newUser.setId(getNextId());

        if (newUser.getName() == null || newUser.getName().isBlank()) {
            newUser.setName(newUser.getLogin());
            log.info("У пользователя не указано имя, установлено значение логина: {}", newUser.getLogin());
        }

        users.put(newUser.getId(), newUser);
        log.info("Пользователь успешно добавлен с id = {}: {}", newUser.getId(), newUser);
        return newUser;
    }

    private int getNextId() {
        return ++currentId;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);

        if (user.getId() == null) {
            log.warn("Ошибка валидации при обновлении: не указан id пользователя");
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(user.getId())) {
            log.warn("Ошибка обновления: пользователь с id = {} не найден", user.getId());
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("При обновлении установлено значение логина вместо пустого имени: {}", user.getLogin());
        }

        users.put(user.getId(), user);
        log.info("Пользователь с id = {} успешно обновлен: {}", user.getId(), user);
        return user;
    }
}