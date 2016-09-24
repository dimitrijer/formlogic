INSERT INTO static.assignment(id, name, category)
VALUES (1, 'Opšte znanje 1', 'Opšte znanje');

INSERT INTO static.task(assignment_id, ord)
VALUES (1, 1);

INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES 
(1, 1, 'single', 'Koje je boje nebo?', '{"zute", "crvene", "plave"}', '{2}'::int2[]),
(1, 2, 'multiple', 'Šta od navedenog NIJE satelit?', '{"Jupiter", "Demetra", "Io", "Kalisto", "Orfej"}', '{0, 1, 4}'::int2[]),
(1, 3, 'fill', 'Kako se zove glavni grad Estonije?', '{"Talin"}', '{0}'::int2[]);

INSERT INTO static.task(assignment_id, ord)
VALUES (1, 2);

INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(2, 1, 'fill', 'Koja je najduža reka u Srbiji?', '{"Morava"}', '{0}'::int2[]),
(2, 2, 'single', 'Šta je gluten?', '{"protein", "lipid", "dekstroza"}', '{0}'::int2[]);

INSERT INTO static.assignment(id, name, category)
VALUES (2, 'Svođenje na KNF', 'Ekspertski sistemi');
