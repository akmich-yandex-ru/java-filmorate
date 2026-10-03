package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {
    User add(User user);

    User update(User user);

    void delete(Long id);

    boolean containsUser(Long id);

    Collection<User> findAll();

    Optional<User> findById(Long id);
}