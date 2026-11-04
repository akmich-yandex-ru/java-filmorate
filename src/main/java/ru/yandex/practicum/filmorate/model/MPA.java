package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MPA {
    @NotNull(message = "ID рейтинга MPA должен быть указан")
    private Long id;
    private String name;
    private String description;
}
