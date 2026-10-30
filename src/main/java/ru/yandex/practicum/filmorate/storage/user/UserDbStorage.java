package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Component("userDbStorage")
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private static final String SQL_INSERT_USER = """
            INSERT INTO users (email, login, name, birthday)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SQL_UPDATE_USER = """
            UPDATE users
            SET email = ?, login = ?, name = ?, birthday = ?
            WHERE id = ?
            """;

    private static final String SQL_DELETE_USER = """
            DELETE FROM users
            WHERE id = ?
            """;

    private static final String SQL_CHECK_USER_EXISTS = """
            SELECT COUNT(*) FROM users
            WHERE id = ?
            """;

    private static final String SQL_FIND_ALL_USERS = """
            SELECT * FROM users
            """;

    private static final String SQL_FIND_USER_BY_ID = """
            SELECT * FROM users
            WHERE id = ?
            """;

    private static final String SQL_FIND_USER_FRIENDS = """
            SELECT friend_id, status
            FROM friendships
            WHERE user_id = ?
            """;

    private static final String SQL_DELETE_USER_FRIENDS = """
            DELETE FROM friendships
            WHERE user_id = ?
            """;

    private static final String SQL_INSERT_USER_FRIEND = """
            INSERT INTO friendships (user_id, friend_id, status)
            VALUES (?, ?, ?)
            """;

    private static final String SQL_ADD_FRIEND = """
            INSERT INTO friendships (user_id, friend_id, status)
            VALUES (?, ?, 'UNCONFIRMED')
            """;

    private static final String SQL_REMOVE_FRIEND = """
            DELETE FROM friendships
            WHERE user_id = ? AND friend_id = ?
            """;

    private static final String SQL_FIND_FRIENDS_BY_USER_ID = """
            SELECT u.* FROM users u
            JOIN friendships f ON u.id = f.friend_id
            WHERE f.user_id = ?
            """;

    private static final String SQL_GET_FRIENDSHIP_STATUS = """
            SELECT status FROM friendships
            WHERE user_id = ? AND friend_id = ?
            """;

    private static final String SQL_UPDATE_FRIENDSHIP_STATUS = """
            UPDATE friendships
            SET status = ?
            WHERE user_id = ? AND friend_id = ?
            """;

    private static final String SQL_INSERT_FRIENDSHIP = """
            INSERT INTO friendships (user_id, friend_id, status)
            VALUES (?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    @Override
    public User add(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(SQL_INSERT_USER, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);

        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        user.setId(id);

        if (user.getFriends() != null && !user.getFriends().isEmpty()) {
            saveFriends(user);
        }

        return user;
    }

    @Override
    public User update(User user) {
        jdbcTemplate.update(SQL_UPDATE_USER,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId());

        if (user.getFriends() != null) {
            saveFriends(user);
        }

        return user;
    }

    @Override
    public boolean delete(Long id) {
        int rowsAffected = jdbcTemplate.update(SQL_DELETE_USER, id);
        return rowsAffected > 0;
    }

    @Override
    public boolean containsUser(Long id) {
        Integer count = jdbcTemplate.queryForObject(SQL_CHECK_USER_EXISTS, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public Collection<User> findAll() {
        Collection<User> users = jdbcTemplate.query(SQL_FIND_ALL_USERS, userRowMapper);
        users.forEach(this::loadFriends);
        return users;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jdbcTemplate.query(SQL_FIND_USER_BY_ID, userRowMapper, id)
                .stream()
                .findFirst()
                .map(this::loadFriends);
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        jdbcTemplate.update(SQL_ADD_FRIEND, userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        jdbcTemplate.update(SQL_REMOVE_FRIEND, userId, friendId);
    }

    @Override
    public Collection<User> findFriendsByUserId(Long userId) {
        Collection<User> friends = jdbcTemplate.query(SQL_FIND_FRIENDS_BY_USER_ID, userRowMapper, userId);
        friends.forEach(this::loadFriends);
        return friends;
    }

    private User loadFriends(User user) {
        user.setFriends(findFriends(user.getId()));
        return user;
    }

    private Map<Long, FriendshipStatus> findFriends(long userId) {
        Map<Long, FriendshipStatus> friends = new HashMap<>();
        jdbcTemplate.query(SQL_FIND_USER_FRIENDS, rs -> {
            friends.put(
                    rs.getLong("friend_id"),
                    FriendshipStatus.valueOf(rs.getString("status"))
            );
        }, userId);
        return friends;
    }

    private void saveFriends(User user) {
        jdbcTemplate.update(SQL_DELETE_USER_FRIENDS, user.getId());

        user.getFriends().forEach((friendId, status) ->
                jdbcTemplate.update(
                        SQL_INSERT_USER_FRIEND,
                        user.getId(),
                        friendId,
                        status.name()
                )
        );
    }

    @Override
    public FriendshipStatus getFriendshipStatus(long userId, long friendId) {
        return jdbcTemplate.query(SQL_GET_FRIENDSHIP_STATUS, rs -> {
            if (rs.next()) {
                return FriendshipStatus.valueOf(rs.getString("status"));
            }
            return null; // Если связи еще нет, возвращаем null
        }, userId, friendId);
    }

    @Override
    public void updateFriendship(long userId, long friendId, FriendshipStatus status) {
        jdbcTemplate.update(SQL_UPDATE_FRIENDSHIP_STATUS, status.name(), userId, friendId);
    }

    @Override
    public void addFriendConnection(long userId, long friendId, FriendshipStatus status) {
        jdbcTemplate.update(SQL_INSERT_FRIENDSHIP, userId, friendId, status.name());
    }
}
