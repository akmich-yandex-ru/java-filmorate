package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate EARLIEST_RELEASE = LocalDate.of(1895, 12, 28);
    private static final DateTimeFormatter RUSSIAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy 'года'", Locale.of("ru"));

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("genreDbStorage") GenreStorage genreStorage,
                       @Qualifier("mpaDbStorage") MpaStorage mpaStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    public Film add(Film film) {
        log.info("Получен запрос на добавление фильма: {}", film);

        validateReleaseDate(film);

        MPA validatedMpa = resolveMpa(film);
        film.setMpa(validatedMpa);

        film.setGenres(resolveGenres(film));

        Film savedFilm = filmStorage.add(film);

        log.info("Фильм успешно добавлен с id = {}: {}", savedFilm.getId(), savedFilm);
        return savedFilm;
    }

    public Film update(Film film) {
        log.info("Получен запрос на обновление фильма: {}", film);

        if (!filmStorage.containsFilm(film.getId())) {
            log.warn("Ошибка обновления: фильм с id = {} не найден", film.getId());
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        validateReleaseDate(film);

        MPA validatedMpa = resolveMpa(film);
        film.setMpa(validatedMpa);

        film.setGenres(resolveGenres(film));

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

        Film film = filmStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("Фильм с id = {} не найден", id);
                    return new NotFoundException("Фильм с id = " + id + " не найден");
                });

        Collection<Genre> genres = genreStorage.findGenresByFilmId(List.of(id));
        film.setGenres(new LinkedHashSet<>(genres));

        return film;
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(EARLIEST_RELEASE)) {
            log.warn("Ошибка валидации фильма '{}': некорректная дата релиза {}", film.getName(), film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше " + EARLIEST_RELEASE.format(RUSSIAN_DATE_FORMATTER));
        }
    }

    private MPA resolveMpa(Film film) {
        return mpaStorage.findById(film.getMpa().getId())
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с id = " + film.getMpa().getId() + " не найден"));
    }

    private Set<Genre> resolveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Long> genreIds = film.getGenres().stream()
                .map(genre -> {
                    if (genre.getId() == null) {
                        throw new ValidationException("Id жанра должен быть указан");
                    }
                    return genre.getId();
                })
                .distinct()
                .toList();

        Collection<Genre> resolvedGenres = genreStorage.findAllByIds(genreIds);

        if (resolvedGenres.size() != genreIds.size()) {
            throw new NotFoundException("Один или несколько жанров не найдены в базе данных");
        }

        // 4. Сортируем жанры по ID через TreeSet и возвращаем требуемый LinkedHashSet
        Set<Genre> sortedGenres = new TreeSet<>(Comparator.comparing(Genre::getId));
        sortedGenres.addAll(resolvedGenres);

        return new LinkedHashSet<>(sortedGenres);
    }
}
