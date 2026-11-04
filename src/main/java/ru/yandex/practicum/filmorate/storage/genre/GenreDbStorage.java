package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component("genreDbStorage")
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {

    private static final String FIND_GENRE_BY_ID_QUERY = """
            SELECT id, name
            FROM genres
            WHERE id = ?
            """;

    private static final String FIND_ALL_GENRES_QUERY = """
            SELECT id, name
            FROM genres
            ORDER BY id
            """;

    private static final String FIND_ALL_GENRES_BY_IDS_QUERY = """
            SELECT id, name
            FROM genres
            WHERE id IN (%s)
            """;

    private static final String FIND_GENRES_BY_FILM_ID_QUERY = """
            SELECT g.id, g.name
            FROM film_genres fg
            JOIN genres g ON fg.genre_id = g.id
            WHERE fg.film_id = ?
            ORDER BY g.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final GenreRowMapper genreRowMapper;

    @Override
    public Optional<Genre> findById(Long id) {
        return jdbcTemplate.query(FIND_GENRE_BY_ID_QUERY, genreRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public Collection<Genre> findAll() {
        return jdbcTemplate.query(FIND_ALL_GENRES_QUERY, genreRowMapper);
    }

    @Override
    public Collection<Genre> findAllByIds(List<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return List.of();
        }

        String inSql = String.join(",", Collections.nCopies(genreIds.size(), "?"));
        String sql = String.format(FIND_ALL_GENRES_BY_IDS_QUERY, inSql);

        Object[] args = genreIds.toArray();

        return jdbcTemplate.query(sql, genreRowMapper, args);
    }

    @Override
    public Collection<Genre> findGenresByFilmId(Long filmId) {
        return jdbcTemplate.query(FIND_GENRES_BY_FILM_ID_QUERY, genreRowMapper, filmId);
    }
}
