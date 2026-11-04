package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GenreStorage {
    Optional<Genre> findById(Long id);

    Collection<Genre> findAll();

    Collection<Genre> findAllByIds(List<Long> genreIds);

    Collection<Genre> findGenresByFilmId(Long filmId);
}
