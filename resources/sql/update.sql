-- name: update-question-progress! 
UPDATE public.question_progress
SET answers = ARRAY[ :answers ] WHERE id = :id;
