package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FilmorateApplicationTests {
    private UserController userController;
    private FilmController filmController;
    private Validator validator;

    @BeforeEach
    void setUp() {
        userController = new UserController();
        filmController = new FilmController();

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

	@Test
	void contextLoads() {
	}

    // ===== ТЕСТЫ ВАЛИДАЦИИ ПОЛЬЗОВАТЕЛЕЙ (UserController) =====

    @Test
    void shouldAddUserWithValidData() {
        User user = new User();
        user.setEmail("mail@yandex.ru");
        user.setLogin("dolore");
        user.setName("Nick Name");
        user.setBirthday(LocalDate.of(1946, 8, 20));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Валидный пользователь не должен вызывать ошибок");

        User addedUser = userController.add(user);

        assertNotNull(addedUser.getId());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsEmpty() {
        User user = new User();
        user.setEmail(""); // Пустой email
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Должна быть ошибка валидации пустого email");
    }

    @Test
    void shouldThrowExceptionWhenEmailDoesNotContainAt() {
        User user = new User();
        user.setEmail("mail.yandex.ru"); // Нет знака @
        user.setLogin("login");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Должна быть ошибка валидации некорректного email");
    }

    @Test
    void shouldThrowExceptionWhenLoginIsEmpty() {
        User user = new User();
        user.setEmail("mail@yandex.ru");
        user.setLogin(""); // Пустой логин
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Должна быть ошибка валидации пустого логина");
    }

    @Test
    void shouldThrowExceptionWhenLoginContainsSpaces() {
        User user = new User();
        user.setEmail("mail@yandex.ru");
        user.setLogin("lo gin"); // Пробел в логине
        user.setBirthday(LocalDate.of(1990, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Должна быть ошибка валидации логина с пробелами");
    }

    @Test
    void shouldReplaceEmptyNameWithLogin() {
        User user = new User();
        user.setEmail("mail@yandex.ru");
        user.setLogin("dolore");
        user.setName(""); // Пустое имя
        user.setBirthday(LocalDate.of(1946, 8, 20));

        User addedUser = userController.add(user);

        assertEquals("dolore", addedUser.getName(), "Имя должно совпадать с логином, если оно пустое");
    }

    @Test
    void shouldThrowExceptionWhenBirthdayIsInFuture() {
        User user = new User();
        user.setEmail("mail@yandex.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.now().plusDays(1)); // В будущем

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Должна быть ошибка валидации даты в будущем");
    }

    @Test
    void shouldAddUserWhenBirthdayIsToday() {
        User user = new User();
        user.setEmail("mail@yandex.ru");
        user.setLogin("login");
        user.setBirthday(LocalDate.now()); // Сегодня — можно

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    // ===== ТЕСТЫ ВАЛИДАЦИИ ФИЛЬМОВ (FilmController) =====

    @Test
    void shouldAddFilmWithValidData() {
        Film film = new Film();
        film.setName("nisi et molestie");
        film.setDescription("adipisicing");
        film.setReleaseDate(LocalDate.of(1967, 3, 25));
        film.setDuration(100);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty());

        Film addedFilm = filmController.add(film);
        assertNotNull(addedFilm.getId());
    }

    @Test
    void shouldThrowExceptionWhenFilmNameIsEmpty() {
        Film film = new Film();
        film.setName("   "); // Пробелы
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(1967, 3, 25));
        film.setDuration(100);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Должна быть ошибка валидации пустого имени фильма");
    }

    @Test
    void shouldThrowExceptionWhenDescriptionIsTooLong() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("a".repeat(201)); // Ровно 201 символ
        film.setReleaseDate(LocalDate.of(1967, 3, 25));
        film.setDuration(100);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Максимальная длина описания — 200 символов");
    }

    @Test
    void shouldAddFilmWhenDescriptionIsExactly200Characters() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("a".repeat(200)); // Граничное условие: 200
        film.setReleaseDate(LocalDate.of(1967, 3, 25));
        film.setDuration(100);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenReleaseDateIsBeforeCinemaBirthday() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(1895, 12, 27)); // На день раньше кино-дня рождения
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> filmController.add(film));
    }

    @Test
    void shouldAddFilmWhenReleaseDateIsExactlyCinemaBirthday() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(1895, 12, 28)); // Граничное условие
        film.setDuration(100);

        assertDoesNotThrow(() -> filmController.add(film));
    }

    @Test
    void shouldThrowExceptionWhenDurationIsZeroOrNegative() {
        Film film = new Film();
        film.setName("Name");
        film.setDescription("description");
        film.setReleaseDate(LocalDate.of(1967, 3, 25));

        film.setDuration(0); // Ноль
        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Должна быть ошибка валидации при нулевой длительности");

        film.setDuration(-10); // Отрицательное
        violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Должна быть ошибка валидации при отрицательной длительности");
    }
}
