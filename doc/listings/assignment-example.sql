-- Ubacuje se red za novi test.
INSERT INTO static.assignment(id, name, category)
VALUES (1, '@Opšta@ kultura 3 - Nauka', '@Opšte@ znanje');

-- Ubacuje se prva stranica.
INSERT INTO static.task(assignment_id, ord)
VALUES (1, 1);

-- Ubacuju se tri pitanja na prvoj stranici.
WITH task AS (SELECT max(id) AS id FROM static.task)
INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(task.id, 2, 'single', 'Koji @fizičar@ je otkrio zakon gravitacije?', '{"Isak Njutn", "Johanes Kepler", "Tiho Brahe"}', '{0}'::int2[]),
(task.id, 3, 'multiple', 'Za koja od @ponuđenih@ @naučnih@ @dostignuća@ je dodeljena Nobelova nagrada?', '{"Radijacija", "@Mikrokosmičko@ pozadinsko @zračenje@", "Antibiotici", "@Heliocentični@ sistem", "Struktura atoma"}', '{0, 1, 2, 4}'::int2[]),
(task.id, 4, 'fill', 'Brzina svetlosti u kilometrima po sekundi iznosi:', '{"300000"}', '{0}'::int2[]);
