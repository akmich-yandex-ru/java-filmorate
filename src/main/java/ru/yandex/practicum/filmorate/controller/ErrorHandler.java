package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.validation.ConstraintViolationException;
import ru.yandex.practicum.filmorate.exception.ErrorResponse;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    // 1. Обработка ошибки 400 (Ошибка валидации через ValidationException)
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(final ValidationException e) {
        log.warn("Ошибка валидации 400: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    // Обработка ошибки 400 (Стандартная валидация Spring для @Valid / @RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(final MethodArgumentNotValidException e) {
        String defaultMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        log.warn("Ошибка валидации параметров 400: {}", defaultMessage);
        return new ErrorResponse(defaultMessage);
    }

    // Обработка ошибки 400 (Валидация параметров @RequestParam вроде @Positive count)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(final ConstraintViolationException e) {
        log.warn("Ошибка валидации параметров запроса 400: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    // 2. Обработка ошибки 404 (Объект не найден)
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException e) {
        log.warn("Объект не найден 404: {}", e.getMessage());
        return new ErrorResponse(e.getMessage());
    }

    // 3. Обработка ошибки 500 (Все остальные непредвиденные исключения)
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleThrowable(final Throwable e) {
        log.error("Непредвиденная ошибка сервера 500: ", e);
        return new ErrorResponse("Произошла непредвиденная ошибка: " + e.getMessage());
    }
}

