package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong currentId = new AtomicLong(0);

    @Override
    public User add(User user) {
        user.setId(getNextId());
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public boolean delete(Long id) {
        User user = users.remove(id);
        return user != null;
    }

    @Override
    public boolean containsUser(Long id) {
        return users.containsKey(id);
    }

    @Override
    public Collection<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        User user = users.get(userId);
        User friend = users.get(friendId);

        if (user != null && friend != null) {
            if (user.getFriends() == null) {
                user.setFriends(new HashMap<>());
            }
            user.getFriends().put(friendId, FriendshipStatus.UNCONFIRMED);
        }
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        User user = users.get(userId);
        if (user != null && user.getFriends() != null) {
            user.getFriends().remove(friendId);
        }
    }

    @Override
    public Collection<User> findFriendsByUserId(Long userId) {
        User user = users.get(userId);
        if (user == null || user.getFriends() == null) {
            return List.of();
        }

        return user.getFriends().keySet().stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private long getNextId() {
        return currentId.incrementAndGet();
    }

    @Override
    public FriendshipStatus getFriendshipStatus(long userId, long friendId) {
        User user = users.get(userId);
        if (user != null && user.getFriends() != null) {
            return user.getFriends().get(friendId);
        }
        return null;
    }

    @Override
    public void updateFriendship(long userId, long friendId, FriendshipStatus status) {
        User user = users.get(userId);
        if (user != null && user.getFriends() != null) {
            user.getFriends().put(friendId, status);
        }
    }

    @Override
    public void addFriendConnection(long userId, long friendId, FriendshipStatus status) {
        User user = users.get(userId);
        if (user != null && user.getFriends() != null) {
            user.getFriends().put(friendId, status);
        }
    }

    @Override
    public Collection<User> findCommonFriends(Long userId, Long otherId) {
        User user = users.get(userId);
        User otherUser = users.get(otherId);

        if (user == null || user.getFriends() == null || otherUser == null || otherUser.getFriends() == null) {
            return List.of();
        }

        return user.getFriends().keySet().stream()
                .filter(otherUser.getFriends()::containsKey)
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
