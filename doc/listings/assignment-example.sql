INSERT INTO static.assignment(id, name, category)
VALUES (1, '@Opšta@ kultura 3 - Nauka', '@Opšte@ znanje');

-- Prva stranica.
INSERT INTO static.task(assignment_id, ord)
VALUES (1, 1);

-- Jedno pitanje na stranici.
WITH task AS (SELECT max(id) AS id FROM static.task)
INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(task.id, 1, 'single', 'Koje godine je @čovek@ prvi put sleteo na Mesec?', '{"1989", "1975", "1969", "1953"}', '{2}'::int2[]);

-- Druga stranica sa demonstracijom.
INSERT INTO static.task(assignment_id, ord, contents)
VALUES (3, 2, '[:img {:style "max-width: 100%;" :src "http://goo.gl/QHHPpW"}]');

-- Tri pitanja na stranici.
WITH task AS (SELECT max(id) AS id FROM static.task)
INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(task.id, 2, 'single', 'Koji @fizičar@ je otkrio zakon gravitacije?', '{"Isak Njutn", "Johanes Kepler", "Tiho Brahe"}', '{0}'::int2[]),
(task.id, 3, 'multiple', 'Za koja od @ponuđenih@ @naučnih@ @dostignuća@ je dodeljena Nobelova nagrada?', '{"Radijacija", "@Mikrokosmičko@ pozadinsko @zračenje@", "Antibiotici", "@Heliocentični@ sistem", "Struktura atoma"}', '{0, 1, 2, 4}'::int2[]),
(task.id, 4, 'fill', 'Brzina svetlosti u kilometrima po sekundi iznosi:', '{"300000"}', '{0}'::int2[]);

-- Poslednja stranica.
INSERT INTO static.task(assignment_id, ord)
VALUES (1, 3);

-- Jedno pitanje.
WITH task AS (SELECT max(id) AS id FROM static.task)
INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(task.id, 5, 'fill', 'Koliko elemenata iz periodnog sistema se mogu @naći@ u prirodi?', '{"94"}', '{0}'::int2[]);
