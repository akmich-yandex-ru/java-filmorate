package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;

import java.util.Collection;
import java.util.Optional;

@Component("mpaDbStorage")
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {

    private static final String FIND_MPA_BY_ID_QUERY = """
            SELECT id, rating, description
            FROM mpa
            WHERE id = ?
            """;
    private static final String FIND_MPA_BY_RATING_QUERY = """
            SELECT id, rating, description
            FROM mpa
            WHERE rating = ?
            """;
    private static final String FIND_ALL_MPA_QUERY = """
            SELECT id, rating, description
            FROM mpa
            ORDER BY id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final MpaRowMapper mpaRowMapper;

    @Override
    public Optional<MPA> findById(Long id) {
        return jdbcTemplate.query(FIND_MPA_BY_ID_QUERY, mpaRowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<MPA> findByRating(String rating) {
        return jdbcTemplate.query(FIND_MPA_BY_RATING_QUERY, mpaRowMapper, rating)
                .stream()
                .findFirst();
    }

    @Override
    public Collection<MPA> findAll() {
        return jdbcTemplate.query(FIND_ALL_MPA_QUERY, mpaRowMapper);
    }
}
