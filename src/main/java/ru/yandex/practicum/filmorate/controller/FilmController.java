package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private static final LocalDate EARLIEST_RELEASE = LocalDate.of(1895, 12, 28);
    private static final DateTimeFormatter RUSSIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'года'", Locale.of("ru"));

    private final Map<Integer, Film> films = new HashMap<>();
    private int currentId = 0;

    @GetMapping
    public Collection<Film> findAll() {
        log.info("Получен запрос на получение списка всех фильмов. Текущее количество: {}", films.size());
        return films.values();

    }

    @PostMapping
    public Film add(@Valid @RequestBody Film newFilm) {
        log.info("Получен запрос на добавление фильма: {}", newFilm);
        validateReleaseDate(newFilm);

        newFilm.setId(getNextId());
        films.put(newFilm.getId(), newFilm);
        log.info("Фильм успешно добавлен с id = {}: {}", newFilm.getId(), newFilm);
        return newFilm;
    }

    private int getNextId() {
        return ++currentId;
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);

        if (film.getId() == null) {
            log.warn("Ошибка валидации при обновлении: не указан id фильма");
            throw new ValidationException("Id должен быть указан");
        }

        if (!films.containsKey(film.getId())) {
            log.warn("Ошибка обновления: фильм с id = {} не найден", film.getId());
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        validateReleaseDate(film);
        films.put(film.getId(), film);
        log.info("Фильм с id = {} успешно обновлен: {}", film.getId(), film.getName());
        return film;
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE)) {
            log.warn("Ошибка валидации фильма '{}': некорректная дата релиза {}", film.getName(), film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше " + EARLIEST_RELEASE.format(RUSSIAN_DATE_FORMATTER));
        }
    }
}
