INSERT INTO static.assignment(id, name, category)
VALUES (1, 'Opšta kultura 1', 'Opšte znanje');

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
(2, 4, 'fill', 'Koja je najduža reka u Srbiji?', '{"Morava"}', '{0}'::int2[]),
(2, 5, 'single', 'Šta je gluten?', '{"protein", "lipid", "dekstroza"}', '{0}'::int2[]);

INSERT INTO static.assignment(id, name, category)
VALUES (2, 'Opšta kultura 2 - Muzika', 'Opšte znanje');

INSERT INTO static.task(assignment_id, ord)
VALUES (2, 1);

INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(3, 1, 'single', 'Kako se zove vrsta muzike koju svira bend Red Hot Chilly Peppers?', '{"bluegrass", "funk rock", "industrial metal", "nijedno od ponuđenih"}', '{1}'::int2[]),
(3, 2, 'multiple', 'Koji od sledećih bendova su iz Amerike?', '{"Prodigy", "Arctic Monkeys", "Opeth", "Black Sabbath", "Foster the People"}', '{4}'::int2[]),
(3, 3, 'fill', 'Navesti ime jedne pesme Beatles-a.', '{"Yellow Submarine"}', '{0}'::int2[]);

INSERT INTO static.assignment(id, name, category)
VALUES (3, 'Svođenje na KNF', 'Ekspertski sistemi');
