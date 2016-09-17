-- name: find-user-by-email
SELECT * FROM public.user WHERE email = :email;
