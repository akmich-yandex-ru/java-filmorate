-- Наполнение справочника MPA рейтингов
MERGE INTO mpa KEY (id) VALUES (1, 'G', 'у фильма нет возрастных ограничений');
MERGE INTO mpa KEY (id) VALUES (2, 'PG', 'детям рекомендуется смотреть фильм с родителями');
MERGE INTO mpa KEY (id) VALUES (3, 'PG-13', 'детям до 13 лет просмотр не желателен');
MERGE INTO mpa KEY (id) VALUES (4, 'R', 'лицам до 17 лет просматривать фильм можно только в присутствии взрослого');
MERGE INTO mpa KEY (id) VALUES (5, 'NC-17', 'лицам до 18 лет просмотр запрещён');

-- Наполнение справочника жанров
MERGE INTO genres KEY (id) VALUES (1, 'Комедия');
MERGE INTO genres KEY (id) VALUES (2, 'Драма');
MERGE INTO genres KEY (id) VALUES (3, 'Мультфильм');
MERGE INTO genres KEY (id) VALUES (4, 'Триллер');
MERGE INTO genres KEY (id) VALUES (5, 'Документальный');
MERGE INTO genres KEY (id) VALUES (6, 'Боевик');
