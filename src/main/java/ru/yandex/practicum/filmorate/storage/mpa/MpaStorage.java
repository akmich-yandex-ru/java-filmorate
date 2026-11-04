package ru.yandex.practicum.filmorate.storage.mpa;

import ru.yandex.practicum.filmorate.model.MPA;

import java.util.Collection;
import java.util.Optional;

public interface MpaStorage {
    Optional<MPA> findById(Long id);

    Optional<MPA> findByRating(String rating);

    Collection<MPA> findAll();
}
