package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;

import java.util.Collection;
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
}
