-- name: insert-user!
INSERT INTO "user" (email, password) VALUES (:email, md5(:password));

-- name: insert-progress<!
INSERT INTO "assignment_progress" (user_id, assignment_id)
VALUES (:user_id, :assignment_id);

-- name: insert-question-progress<!
INSERT INTO "question_progress" (assignment_progress_id, question_id, answers)
VALUES (:assignment_progress_id, :question_id, '{}'::text[]);
