package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {
    User add(User user);
    User update(User user);
    boolean delete(Long id);
    boolean containsUser(Long id);
    Collection<User> findAll();
    Optional<User> findById(Long id);
    void addFriend(Long userId, Long friendId);
    void removeFriend(Long userId, Long friendId);
    Collection<User> findFriendsByUserId(Long userId);
    FriendshipStatus getFriendshipStatus(long userId, long friendId);
    void updateFriendship(long userId, long friendId, FriendshipStatus status);
    void addFriendConnection(long userId, long friendId, FriendshipStatus status);
}