package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public User add(User user) {
        log.info("Получен запрос на добавление пользователя: {}", user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("У пользователя не указано имя, установлено значение логина: {}", user.getLogin());
        }

        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }

        User savedUser = userStorage.add(user);

        log.info("Пользователь успешно добавлен с id = {}: {}", savedUser.getId(), savedUser);
        return savedUser;
    }

    public User update(User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);

        if (user.getId() == null) {
            log.warn("Ошибка валидации при обновлении: не указан id пользователя");
            throw new ValidationException("Id должен быть указан");
        }

        User oldUser = userStorage.findById(user.getId())
                .orElseThrow(() -> {
                    log.warn("Ошибка обновления: пользователь с id = {} не найден", user.getId());
                    return new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
                });

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("При обновлении установлено значение логина вместо пустого имени: {}", user.getLogin());
        }

        if (user.getFriends() == null) {
            user.setFriends(oldUser.getFriends() != null ? oldUser.getFriends() : new HashSet<>());
        }

        User updatedUser = userStorage.update(user);

        log.info("Пользователь с id = {} успешно обновлен: {}", updatedUser.getId(), updatedUser);
        return updatedUser;
    }

    public Collection<User> findAll() {
        log.info("Получен запрос на получение списка всех пользователей");
        Collection<User> users = userStorage.findAll();
        log.info("Успешно возвращено {} пользователей", users.size());
        return users;
    }

    public void delete(Long id) {
        log.info("Получен запрос на удаление пользователя с id = {}", id);

        if (id == null) {
            log.warn("Ошибка валидации при удалении: id не может быть null");
            throw new ValidationException("Id для удаления должен быть указан");
        }

        if (!userStorage.containsUser(id)) {
            log.warn("Ошибка удаления: пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }

        userStorage.delete(id);
        log.info("Пользователь с id = {} успешно удален", id);
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Получен запрос на добавление в друзья: пользователь {} хочет добавить пользователя {}", userId, friendId);

        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }

        if (friend.getFriends() == null) {
            friend.setFriends(new HashSet<>());
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        log.info("Пользователи {} и {} теперь друзья", userId, friendId);

        userStorage.update(user);
        userStorage.update(friend);
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Получен запрос на удаление из друзей: пользователь {} хочет удалить пользователя {}", userId, friendId);

        User user = getUserOrThrow(userId);
        User friend = getUserOrThrow(friendId);

        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }

        if (friend.getFriends() == null) {
            friend.setFriends(new HashSet<>());
        }

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        log.info("Пользователи {} и {} теперь не друзья", userId, friendId);
        userStorage.update(user);
        userStorage.update(friend);
    }

    public User findById(Long id) {
        log.info("Получен запрос на получение пользователя с id = {}", id);
        return getUserOrThrow(id);
    }

    private User getUserOrThrow(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id = {} не найден", id);
                    return new NotFoundException("Пользователь с id = " + id + " не найден");
                });
    }

    public Collection<User> getFriends(Long userId) {
        log.info("Получен запрос на получение списка друзей пользователя {}", userId);
        User user = getUserOrThrow(userId);

        return user.getFriends().stream()
                .map(this::getUserOrThrow)
                .collect(Collectors.toList());
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Получен запрос на получение общего списка друзей пользователей {} и {}", userId, otherId);
        User user = getUserOrThrow(userId);
        User anotherUser = getUserOrThrow(otherId);

        return user.getFriends().stream()
                .filter(anotherUser.getFriends()::contains)
                .map(this::getUserOrThrow)
                .collect(Collectors.toList());
    }
}
