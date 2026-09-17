package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Film.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    private Integer id;

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;

    //Привет Ирек!
    //В тз для description и releaseDate не указано, что не могут быть пустыми,
    //но логически я понимаю что надо заполнять.
    //Вопрос: почему ты предлагаешь @NotNull? Ведь он пропустит пустую строку или с пробелами.
    //Может лучше @NotBlank?
    //
    //Аналогичный вопрос и для User.name и User.birthday

    @Size(max = 200, message = "Максимальная длина описания — 200 символов")
    private String description;

    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private int duration;
}
