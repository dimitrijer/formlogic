INSERT INTO static.assignment(id, name)
VALUES (1, 'KNF test');

INSERT INTO static.task(id, assignment_id)
VALUES (1, 1);

INSERT INTO static.question(task_id, type, body, choices, answers)
VALUES (1, 'single', 'Koje je boje nebo?', '{"zute", "crvene", "plave"}', '{2}'::int2[]);
