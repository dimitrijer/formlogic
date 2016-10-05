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
VALUES (3, 'Opšta kultura 3 - Nauka', 'Opšte znanje');

INSERT INTO static.task(assignment_id, ord)
VALUES (3, 1);

INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(4, 1, 'single', 'Koje godine je čovek prvi put sleteo na Mesec?', '{"1989", "1975", "1969", "1953"}', '{2}'::int2[]);

INSERT INTO static.task(assignment_id, ord, contents)
VALUES (3, 2, '[:img {:style "max-width: 100%;" :src "https://ned.ipac.caltech.edu/level5/Sept05/Gawiser2/Figures/figure3.jpg"}]');

INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(5, 2, 'single', 'Koji fizičar je otkrio zakon gravitacije?', '{"Isak Njutn", "Johanes Kepler", "Tiho Brahe"}', '{0}'::int2[]),
(5, 3, 'multiple', 'Za koja od ponuđenih naučnih dostignuća je dodeljena Nobelova nagrada?', '{"Radijacija", "Mikrokosmičko pozadinsko zračenje", "Antibiotici", "Heliocentični sistem", "Struktura atoma"}', '{0, 1, 2, 4}'::int2[]),
(5, 4, 'fill', 'Brzina svetlosti u kilometrima po sekundi iznosi:', '{"300000"}', '{0}'::int2[]);


INSERT INTO static.task(assignment_id, ord)
VALUES (3, 3);

INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(6, 5, 'fill', 'Koliko elemenata iz periodnog sistema se mogu naći u prirodi?', '{"94"}', '{0}'::int2[]);

INSERT INTO static.assignment(id, name, category)
VALUES (4, 'Formalna logika', 'Ekspertski sistemi');

INSERT INTO static.assignment(id, name, category)
VALUES (5, 'Svođenje na KNF', 'Ekspertski sistemi');
INSERT INTO static.task(assignment_id, ord, contents)
VALUES (5, 1, '(formlogic.views/cnf-page)');

INSERT INTO static.question(task_id, ord, type, body, choices, answers)
VALUES
(6, 1, 'single', 'Koji fizičar je otkrio zakon gravitacije?', '{"Isak Njutn", "Johanes Kepler", "Tiho Brahe"}', '{0}'::int2[]);

INSERT INTO static.assignment(id, name, category)
VALUES (6, 'Zaključivanje rezolucijom', 'Ekspertski sistemi');
