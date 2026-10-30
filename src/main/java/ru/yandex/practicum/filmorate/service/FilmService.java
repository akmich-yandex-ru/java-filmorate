package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate EARLIEST_RELEASE = LocalDate.of(1895, 12, 28);
    private static final DateTimeFormatter RUSSIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'года'", Locale.of("ru"));

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

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

        if (!filmStorage.containsFilm(film.getId())) {
            log.warn("Ошибка обновления: фильм с id = {} не найден", film.getId());
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
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

        if (!filmStorage.containsFilm(filmId)) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }

        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        filmStorage.addLike(filmId, userId);
        log.info("Лайк успешно добавлен для фильма {} от пользователя {}", filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.info("Получен запрос на удаление лайка: пользователь {} хочет удалить лайк фильму {}", userId, filmId);

        if (!filmStorage.containsFilm(filmId)) {
            throw new NotFoundException("Фильм с id = " + filmId + " не найден");
        }
        if (!userStorage.containsUser(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }

        filmStorage.removeLike(filmId, userId);
        log.info("Лайк успешно удален для фильма {} от пользователя {}", filmId, userId);
    }

    public Collection<Film> getMostPopular(Integer count) {
        log.info("Получен запрос на получение {} самых популярных фильмов", count);
        return filmStorage.findPopular(count);
    }

    public Film findById(Long id) {
        log.info("Получен запрос на получение фильма с id = {}", id);
        return filmStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Фильм с id = {} не найден", id);
                    return new NotFoundException("Фильм с id = " + id + " не найден");
                });
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE)) {
            log.warn("Ошибка валидации фильма '{}': некорректная дата релиза {}", film.getName(), film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше " + EARLIEST_RELEASE.format(RUSSIAN_DATE_FORMATTER));
        }
    }
}
