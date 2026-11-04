package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;

@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User add(User user) {
        log.info("Получен запрос на добавление пользователя: {}", user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("У пользователя не указано имя, установлено значение логина: {}", user.getLogin());
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

        userStorage.findById(user.getId())
                .orElseThrow(() -> {
                    log.warn("Ошибка обновления: пользователь с id = {} не найден", user.getId());
                    return new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
                });

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.info("При обновлении установлено значение логина вместо пустого имени: {}", user.getLogin());
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

        boolean isDeleted = userStorage.delete(id);

        if (!isDeleted) {
            log.warn("Ошибка удаления: пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }

        log.info("Пользователь с id = {} успешно удален", id);
    }

    public void addFriend(Long userId, Long friendId) {
        log.info("Получен запрос на добавление в друзья: пользователь {} хочет добавить пользователя {}", userId, friendId);

        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        if (!userStorage.containsUser(friendId)) {
            throw new NotFoundException("Пользователь с id = " + friendId + " не найден");
        }

        FriendshipStatus statusFromFriend = userStorage.getFriendshipStatus(friendId, userId);

        if (statusFromFriend == FriendshipStatus.UNCONFIRMED) {
            userStorage.addFriendConnection(userId, friendId, FriendshipStatus.CONFIRMED);
            userStorage.updateFriendship(friendId, userId, FriendshipStatus.CONFIRMED);
            log.info("Дружба между {} и {} подтверждена (CONFIRMED)", userId, friendId);
        } else {
            userStorage.addFriendConnection(userId, friendId, FriendshipStatus.UNCONFIRMED);
            log.info("Пользователь {} отправил запрос дружбы пользователю {} (UNCONFIRMED)", userId, friendId);
        }
    }

    public void removeFriend(Long userId, Long friendId) {
        log.info("Получен запрос на удаление из друзей: пользователь {} хочет удалить пользователя {}", userId, friendId);

        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        if (!userStorage.containsUser(friendId)) {
            throw new NotFoundException("Пользователь с id = " + friendId + " не найден");
        }

        userStorage.removeFriend(userId, friendId);

        log.info("Пользователи {} и {} теперь не друзья", userId, friendId);
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

        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        return userStorage.findFriendsByUserId(userId);
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Получен запрос на получение общего списка друзей пользователей {} и {}", userId, otherId);

        if (!userStorage.containsUser(userId) || !userStorage.containsUser(otherId)) {
            throw new NotFoundException("Один из пользователей не найден");
        }

        return userStorage.findCommonFriends(userId, otherId);
    }
}
