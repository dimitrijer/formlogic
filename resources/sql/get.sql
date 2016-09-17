-- name: find-user-by-email
SELECT * FROM public.user WHERE email = :email;

-- name: load-questions-for-task
SELECT * FROM static.question WHERE task_id = :task-id;
