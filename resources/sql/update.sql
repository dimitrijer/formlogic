-- name: update-question-progress! 
UPDATE public.question_progress
SET answers = ARRAY[ :answers ] WHERE id = :id;

-- name: update-completed-progress!
UPDATE public.assignment_progress
SET completed_at = (now() AT TIME ZONE 'UTC') WHERE id = :id;
