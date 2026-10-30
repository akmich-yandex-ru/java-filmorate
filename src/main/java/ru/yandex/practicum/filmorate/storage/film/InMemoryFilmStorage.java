package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new ConcurrentHashMap<>();
    private final AtomicLong currentId = new AtomicLong(0);

    @Override
    public Film add(Film film) {
        film.setId(getNextId());
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public void delete(Long id) {
        films.remove(id);
    }

    @Override
    public boolean containsFilm(Long id) {
        return films.containsKey(id);
    }

    @Override
    public Collection<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Optional<Film> findById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        Film film = films.get(filmId);
        if (film != null) {
            if (film.getLikes() == null) {
                film.setLikes(new LinkedHashSet<>());
            }
            film.getLikes().add(userId);
        }
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        Film film = films.get(filmId);
        if (film != null && film.getLikes() != null) {
            film.getLikes().remove(userId);
        }
    }

    @Override
    public Collection<Film> findPopular(Integer count) {
        return films.values().stream()
                .sorted((f1, f2) -> {
                    int likes1 = f1.getLikes() == null ? 0 : f1.getLikes().size();
                    int likes2 = f2.getLikes() == null ? 0 : f2.getLikes().size();
                    return Integer.compare(likes2, likes1); // Сортировка по убыванию
                })
                .limit(count)
                .collect(Collectors.toList());
    }

    private long getNextId() {
        return currentId.incrementAndGet();
    }
}
