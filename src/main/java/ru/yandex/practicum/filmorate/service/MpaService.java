package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MPA;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.Collection;

@Slf4j
@Service
public class MpaService {
    private final MpaStorage mpaStorage;

    public MpaService(@Qualifier("mpaDbStorage") MpaStorage mpaStorage) {
        this.mpaStorage = mpaStorage;
    }

    public Collection<MPA> findAll() {
        log.info("Получен запрос на получение списка всех MPA");
        Collection<MPA> mpas = mpaStorage.findAll();
        log.info("Успешно возвращено {} MPA", mpas.size());
        return mpas;
    }

    public MPA getById(long id) {
        log.info("Получен запрос на получение MPA с id = {}", id);
        return mpaStorage.findById(id)
                .orElseThrow(() -> {
                    log.warn("MPA с id = {} не найден", id);
                    return new NotFoundException("MPA с id = " + id + " не найден");
                });
    }
}