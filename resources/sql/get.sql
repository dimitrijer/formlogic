-- name: find-user-by-email
SELECT * FROM public.user WHERE email = :email;

-- name: find-user-by-id
SELECT * FROM public.user WHERE id = :id;

-- name: load-assignment-categories
SELECT category, count(*) AS cnt FROM static.assignment GROUP BY category;

-- name: load-assignments-by-category
SELECT * FROM static.assignment WHERE category = :category;

-- name: find-assignment-by-id
SELECT * FROM static.assignment WHERE id = :id;

-- name: find-progress-by-id
SELECT * FROM public.assignment_progress WHERE id = :id;

-- name: find-progress-by-assignment-id
SELECT *
FROM public.assignment_progress
WHERE user_id = :user_id AND assignment_id = :assignment_id;

-- name: find-question-progress-for-user
SELECT *
FROM public.question_progress
WHERE assignment_progress_id = :assignment_progress_id AND question_id = :question_id;

-- name: find-task-by-assignment-id
SELECT * FROM static.task WHERE assignment_id = :assignment_id AND ord = :ord;

-- name: find-questions-by-task-id
SELECT * FROM static.question WHERE task_id = :task_id ORDER BY ord;

-- name: find-question-progress-by-assignment-id-for-task
SELECT qp.*
FROM public.question_progress qp
INNER JOIN static.question q ON qp.question_id = q.id
WHERE assignment_progress_id = :assignment_progress_id
      AND question_id IN (SELECT id FROM static.question WHERE task_id = :task_id)
ORDER BY ord;

-- name: get-number-of-questions-completed-for-progress
SELECT count(*)
FROM assignment_progress ap
INNER JOIN question_progress qp on ap.id = qp.assignment_progress_id
WHERE qp.answers != '{""}' AND ap.id = :id;

-- name: get-number-of-correct-questions-for-progress
SELECT count(*)
FROM assignment_progress ap
INNER JOIN question_progress qp on ap.id = qp.assignment_progress_id
WHERE qp.correct AND ap.id = :id;

-- name: get-number-of-questions-for-assignment
SELECT count(*)
FROM static.assignment a
INNER JOIN static.task t ON a.id = t.assignment_id
INNER JOIN static.question q ON t.id = q.task_id
WHERE a.id = :id;

-- name: get-number-of-tasks-for-assignment
SELECT count(*) FROM static.task WHERE assignment_id = :id;

-- name: get-students-like
SELECT id, email FROM public.user WHERE NOT admin AND email ILIKE '%' || :email || '%';

-- name: get-assignments-like
SELECT id, name FROM static.assignment WHERE name ILIKE '%' || :name || '%';

-- name: find-completed-progresses-for-user
WITH
progress AS (SELECT * FROM assignment_progress ap
             WHERE ap.user_id = :id AND completed_at IS NOT NULL),
questions AS (SELECT p.id, correct, count(*) AS cnt FROM question_progress qp
              INNER JOIN progress p ON qp.assignment_progress_id = p.id
              GROUP BY p.id, qp.correct),
total_questions AS (SELECT id, sum(cnt) AS total FROM questions GROUP BY id)
SELECT p.id, p.started_at, p.completed_at, a.name, (100 * cnt::real / total::real)::int AS grade
FROM progress p
INNER JOIN static.assignment a ON p.assignment_id = a.id
INNER JOIN questions q ON p.id = q.id
INNER JOIN total_questions tq ON p.id = tq.id
WHERE q.correct
ORDER BY p.completed_at DESC;

-- name: find-completed-progresses-for-assignment
WITH
progress AS (SELECT * FROM assignment_progress ap
             WHERE ap.assignment_id = :id AND completed_at IS NOT NULL),
questions AS (SELECT p.id, correct, count(*) AS cnt FROM question_progress qp
              INNER JOIN progress p ON qp.assignment_progress_id = p.id
              GROUP BY p.id, qp.correct),
total_questions AS (SELECT id, sum(cnt) AS total FROM questions GROUP BY id)
SELECT p.id, p.started_at, p.completed_at, u.email, (100 * cnt::real / total::real)::int AS grade
FROM progress p
INNER JOIN public.user u ON p.user_id = u.id
INNER JOIN questions q ON p.id = q.id
INNER JOIN total_questions tq ON p.id = tq.id
WHERE q.correct
ORDER BY p.completed_at DESC;
