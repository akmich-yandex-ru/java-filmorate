package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;

@Component("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private static final String SQL_INSERT_FILM = """
            INSERT INTO films (name, description, release_date, duration, mpa_id)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SQL_UPDATE_FILM = """
            UPDATE films
            SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
            WHERE id = ?
            """;

    private static final String SQL_DELETE_FILM = """
            DELETE FROM films
            WHERE id = ?
            """;

    private static final String SQL_CHECK_FILM_EXISTS = """
            SELECT COUNT(*) FROM films
            WHERE id = ?
            """;

    private static final String SQL_FIND_ALL_FILMS = """
            SELECT f.*, m.rating AS mpa_rating, m.description AS mpa_description
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.id
            """;

    private static final String SQL_FIND_FILM_BY_ID = """
            SELECT f.*, m.rating AS mpa_rating, m.description AS mpa_description
            FROM films f
            LEFT JOIN mpa m ON f.mpa_id = m.id
            WHERE f.id = ?
            """;

    private static final String SQL_FIND_GENRES_BY_FILM_ID = """
            SELECT g.id, g.name
            FROM film_genres fg
            JOIN genres g ON fg.genre_id = g.id
            WHERE fg.film_id = ?
            ORDER BY g.id
            """;

    private static final String SQL_DELETE_FILM_GENRES = """
            DELETE FROM film_genres
            WHERE film_id = ?
            """;

    private static final String SQL_INSERT_FILM_GENRE = """
            INSERT INTO film_genres (film_id, genre_id)
            VALUES (?, ?)
            """;

    private static final String SQL_FIND_FILM_LIKES = """
            SELECT user_id
            FROM likes
            WHERE film_id = ?
            """;

    private static final String SQL_INSERT_LIKE = """
            INSERT INTO likes (film_id, user_id)
            VALUES (?, ?)
            """;

    private static final String SQL_DELETE_LIKE = """
            DELETE FROM likes
            WHERE film_id = ? AND user_id = ?
            """;

    private static final String SQL_FIND_POPULAR_FILMS = """
        SELECT f.*, m.rating AS mpa_rating, m.description AS mpa_description
        FROM films f
        LEFT JOIN mpa m ON f.mpa_id = m.id
        LEFT JOIN likes l ON f.id = l.film_id
        GROUP BY f.id, m.rating, m.description
        ORDER BY COUNT(l.user_id) DESC
        LIMIT ?
        """;

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    public FilmDbStorage(JdbcTemplate jdbcTemplate,
                         FilmRowMapper filmRowMapper,
                         @Qualifier("mpaDbStorage") MpaStorage mpaStorage,
                         @Qualifier("genreDbStorage") GenreStorage genreStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.filmRowMapper = filmRowMapper;
        this.mpaStorage = mpaStorage;
        this.genreStorage = genreStorage;
    }

    @Override
    public Film add(Film film) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MPA mpa = resolveMpa(film);

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(SQL_INSERT_FILM, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setObject(3, film.getReleaseDate());
            statement.setObject(4, film.getDuration());
            statement.setLong(5, mpa.getId());
            return statement;
        }, keyHolder);

        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        film.setMpa(mpa);
        film.setGenres(resolveGenres(film));
        saveFilmGenres(film);

        return findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм не найден сразу после создания"));
    }

    @Override
    public Film update(Film film) {
        MPA mpa = resolveMpa(film);

        int rowsUpdated = jdbcTemplate.update(SQL_UPDATE_FILM,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                mpa.getId(),
                film.getId());

        if (rowsUpdated == 0) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        film.setMpa(mpa);
        film.setGenres(resolveGenres(film));
        saveFilmGenres(film);

        return findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм не найден сразу после обновления"));
    }

    @Override
    public void delete(Long id) {
        jdbcTemplate.update(SQL_DELETE_FILM, id);
    }

    @Override
    public boolean containsFilm(Long id) {
        Integer count = jdbcTemplate.queryForObject(SQL_CHECK_FILM_EXISTS, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public Collection<Film> findAll() {
        Collection<Film> films = jdbcTemplate.query(SQL_FIND_ALL_FILMS, filmRowMapper);
        films.forEach(this::loadFilmDependencies);
        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        return jdbcTemplate.query(SQL_FIND_FILM_BY_ID, filmRowMapper, id)
                .stream()
                .findFirst()
                .map(this::loadFilmDependencies);
    }

    private Film loadFilmDependencies(Film film) {
        film.setGenres(findGenres(film.getId()));
        film.setLikes(findLikes(film.getId()));
        return film;
    }

    private MPA resolveMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("Рейтинг MPA должен быть указан");
        }
        return mpaStorage.findById(film.getMpa().getId())
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id = " + film.getMpa().getId() + " не найден"));
    }

    private Set<Genre> resolveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return new LinkedHashSet<>();
        }

        Set<Genre> sortedGenres = new TreeSet<>(Comparator.comparing(Genre::getId));

        for (Genre genre : film.getGenres()) {
            if (genre.getId() == null) {
                throw new ValidationException("Id жанра должен быть указан");
            }
            Genre resolvedGenre = genreStorage.findById(genre.getId())
                    .orElseThrow(() -> new NotFoundException("Жанр с id = " + genre.getId() + " не найден"));
            sortedGenres.add(resolvedGenre);
        }

        return new LinkedHashSet<>(sortedGenres);
    }

    private Set<Genre> findGenres(long filmId) {
        List<Genre> genres = jdbcTemplate.query(SQL_FIND_GENRES_BY_FILM_ID, (rs, rowNum) -> {
            Genre genre = new Genre();
            genre.setId(rs.getLong("id"));
            genre.setName(rs.getString("name"));
            return genre;
        }, filmId);
        return new LinkedHashSet<>(genres);
    }

    private Set<Long> findLikes(long filmId) {
        List<Long> likes = jdbcTemplate.query(SQL_FIND_FILM_LIKES,
                (rs, rowNum) -> rs.getLong("user_id"), filmId);
        return Set.copyOf(likes);
    }

    private void saveFilmGenres(Film film) {
        jdbcTemplate.update(SQL_DELETE_FILM_GENRES, film.getId());
        film.getGenres().forEach(genre ->
                jdbcTemplate.update(SQL_INSERT_FILM_GENRE, film.getId(), genre.getId())
        );
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbcTemplate.update(SQL_INSERT_LIKE, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        int rowsAffected = jdbcTemplate.update(SQL_DELETE_LIKE, filmId, userId);
        if (rowsAffected == 0) {
            throw new NotFoundException("Лайк от пользователя с id = " + userId + " не найден");
        }
    }
    @Override
    public Collection<Film> findPopular(Integer count) {
        Collection<Film> films = jdbcTemplate.query(SQL_FIND_POPULAR_FILMS, filmRowMapper, count);
        films.forEach(this::loadFilmDependencies);
        return films;
    }
}
