package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.util.Collection;

@Slf4j
@Service
public class GenreService {
    private final GenreStorage genreStorage;

    public GenreService(@Qualifier("genreDbStorage") GenreStorage genreStorage) {
        this.genreStorage = genreStorage;
    }

    public Collection<Genre> findAll() {
        log.info("Получен запрос на получение списка всех жанров");
        Collection<Genre> genres = genreStorage.findAll();
        log.info("Успешно возвращено {} жанров", genres.size());
        return genres;
    }

    public Genre getById(Long id) {
        log.info("Получен запрос на получение жанра с id = {}", id);
        return genreStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Жанр с id = {} не найден", id);
                    return new NotFoundException("Жанр с id = " + id + " не найден");
                });

    }
}
