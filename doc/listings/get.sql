WITH
-- First CTE fetches completed assignment progresses for
-- specified assignment ID.
progress AS (SELECT * FROM assignment_progress ap
             WHERE ap.assignment_id = :id
             AND completed_at IS NOT NULL),
-- Second CTE fetches two rows for each assignment progress -
-- number of correct and number of wrong answers.
questions AS (SELECT p.id, correct, count(*) AS cnt
              FROM question_progress qp
              INNER JOIN progress p ON qp.assignment_progress_id = p.id
              GROUP BY p.id, qp.correct),
-- Third CTE sums up both rows for each assignment progress,
-- yielding total number of questions.
total_questions AS (SELECT id, sum(cnt) AS total FROM questions GROUP BY id)
SELECT p.id, p.started_at, p.completed_at, u.email,
       (100 * COALESCE(cnt::real, 0::real) / total::real)::int AS grade
FROM progress p
INNER JOIN public.user u ON p.user_id = u.id
LEFT OUTER JOIN questions q ON p.id = q.id AND q.correct
INNER JOIN total_questions tq ON p.id = tq.id
ORDER BY p.completed_at DESC;
