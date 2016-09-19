--name: insert-user!
INSERT INTO "user" (email, password) VALUES (:email, md5(:password));

--name: insert-progress!
INSERT INTO "assignment_progress" (user_id, assignment_id)
VALUES (:user_id, :assignment_id);
