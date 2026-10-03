package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate EARLIEST_RELEASE = LocalDate.of(1895, 12, 28);
    private static final DateTimeFormatter RUSSIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'года'", Locale.of("ru"));
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Film add(Film film) {
        log.info("Получен запрос на добавление фильма: {}", film);

        validateReleaseDate(film);
        Film savedFilm = filmStorage.add(film);

        log.info("Фильм успешно добавлен с id = {}: {}", savedFilm.getId(), savedFilm);
        return savedFilm;
    }

    public Film update(Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);

        if (film.getId() == null) {
            log.warn("Ошибка валидации при обновлении: не указан id фильма");
            throw new ValidationException("Id должен быть указан");
        }

        Film oldFilm = filmStorage.findById(film.getId())
                .orElseThrow(() -> {
                    log.warn("Ошибка обновления: фильм с id = {} не найден", film.getId());
                    return new NotFoundException("Фильм с id = " + film.getId() + " не найден");
                });

        if (film.getLikes() == null) {
            film.setLikes(oldFilm.getLikes() != null ? oldFilm.getLikes() : new HashSet<>());
        }

        validateReleaseDate(film);

        Film updatedFilm = filmStorage.update(film);
        log.info("Фильм с id = {} успешно обновлен: {}", updatedFilm.getId(), updatedFilm.getName());
        return updatedFilm;
    }

    public Collection<Film> findAll() {
        log.info("Получен запрос на получение списка всех фильмов.");
        Collection<Film> films = filmStorage.findAll();
        log.info("Успешно возвращено {} фильмов", films.size());
        return films;
    }

    public void delete(Long id) {
        log.info("Получен запрос на удаление фильма с id = {}", id);

        if (id == null) {
            log.warn("Ошибка валидации при удалении: id не может быть null");
            throw new ValidationException("Id для удаления должен быть указан");
        }

        if (!filmStorage.containsFilm(id)) {
            log.warn("Ошибка удаления: фильм с id = {} не найден", id);
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }

        filmStorage.delete(id);
        log.info("Фильм с id = {} успешно удален", id);
    }

    public void addLike(Long filmId, Long userId) {
        log.info("Получен запрос на добавление лайка: пользователь {} хочет поставить лайк фильму {}", userId, filmId);

        Film film = getFilmOrThrow(filmId);
        validateUser(userId);

        if (film.getLikes() == null) {
            film.setLikes(new HashSet<>());
        }

        film.getLikes().add(userId);

        log.info("Лайк добавлен. У фильма {} теперь {} лайков", filmId, film.getLikes().size());

        filmStorage.update(film);
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Получен запрос на удаление лайка: пользователь {} хочет удалить лайк фильму {}", userId, filmId);

        Film film = getFilmOrThrow(filmId);
        validateUser(userId);

        film.getLikes().remove(userId);

        log.info("Лайк удален. У фильма {} теперь {} лайков", filmId, film.getLikes().size());

        filmStorage.update(film);
    }

    public Collection<Film> getMostPopular(Integer count) {
        log.info("Получен запрос на получение {} самых популярных фильмов", count);

        return filmStorage.findAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes() == null ? 0 : film.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    public Film findById(Long id) {
        log.info("Получен запрос на получение фильма с id = {}", id);
        return getFilmOrThrow(id);
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE)) {
            log.warn("Ошибка валидации фильма '{}': некорректная дата релиза {}", film.getName(), film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше " + EARLIEST_RELEASE.format(RUSSIAN_DATE_FORMATTER));
        }
    }

    private Film getFilmOrThrow(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Фильм с id = {} не найден", id);
                    return new NotFoundException("Фильм с id = " + id + " не найден");
                });
    }

    private void validateUser(Long id) {
        if (userStorage.findById(id).isEmpty()) {
            log.warn("Пользователь с id = {} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");

        }
    }

}
