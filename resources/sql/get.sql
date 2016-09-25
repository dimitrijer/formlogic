-- name: find-user-by-email
SELECT * FROM public.user WHERE email = :email;

-- name: load-assignment-categories
SELECT category, count(*) AS cnt FROM static.assignment GROUP BY category;

-- name: load-assignments-by-category
SELECT * FROM static.assignment WHERE category = :category;

-- name: find-assignment-by-id
SELECT * FROM static.assignment WHERE id = :id;

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

-- name: get-number-of-tasks-for-assignment
SELECT count(*) FROM static.task WHERE assignment_id = :id;

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

-- name: get-number-of-questions-for-assignment
SELECT count(*)
FROM static.assignment a
INNER JOIN static.task t ON a.id = t.assignment_id
INNER JOIN static.question q ON t.id = q.task_id
WHERE a.id = :id;
