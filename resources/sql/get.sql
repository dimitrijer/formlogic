-- name: find-user-by-id
-- Finds user by PK ID.
SELECT * FROM public.user WHERE id = :id;

-- name: find-user-by-email
-- Finds user by email.
SELECT * FROM public.user WHERE email = :email;
