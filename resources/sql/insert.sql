--name: insert-user!
INSERT INTO "user" (email, password) VALUES (:email, md5(:password));
