-- User table.
CREATE TABLE public.user (
	id SERIAL NOT NULL,
	email text NOT NULL,
	password text NOT NULL,
	admin boolean NOT NULL DEFAULT false,
	PRIMARY KEY("id")
);

CREATE UNIQUE INDEX user_email_idx ON "user" ("email");

-- Score for user and assignment.
CREATE TABLE public.user_score (
	id SERIAL NOT NULL,
	user_id int4 NOT NULL,
	assignment_id int4 NOT NULL,
	started_at timestamptz NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
	completed_at timestamptz,
	score int4 NOT NULL DEFAULT 0,
	PRIMARY KEY("id")
);

ALTER TABLE "user_score" ADD CONSTRAINT "Ref_user_score_to_user"
	FOREIGN KEY ("user_id") REFERENCES "user"("id")
	MATCH SIMPLE
	ON DELETE CASCADE
	ON UPDATE NO ACTION
	NOT DEFERRABLE;

-- ###########
-- STATIC DATA
-- ###########
CREATE SCHEMA static;

-- Assignments are top-level entities.
CREATE TABLE static.assignment (
	id int4 NOT NULL,
	name text NOT NULL,
	PRIMARY KEY("id")
);

-- Tasks specific to assignments.
CREATE TABLE static.task (
	id int4 NOT NULL,
	assignment_id int4 NOT NULL,
	contents text,
	PRIMARY KEY("id")
);

ALTER TABLE "static"."task" ADD CONSTRAINT "Ref_task_to_assignment"
	FOREIGN KEY ("assignment_id") REFERENCES "static"."assignment"("id")
	MATCH SIMPLE
	ON DELETE CASCADE
	ON UPDATE NO ACTION
	NOT DEFERRABLE;

-- Types of questions.
CREATE TYPE static.question_type AS ENUM ('fill', 'single', 'multiple');

-- Questions specific to a task.

-- This function is used to check if answers indexes are correct with regards
-- to choices. Used as a constraint.
CREATE FUNCTION check_question_answers_idx(
	answers int2[],
	choices_length int4)
RETURNS boolean AS
$$
DECLARE
	v_answer_idx int2;
BEGIN
	FOREACH v_answer_idx IN ARRAY answers LOOP
		IF v_answer_idx >= choices_length THEN
			RETURN FALSE;
		END IF;
	END LOOP;

	RETURN TRUE;
END;
$$
LANGUAGE plpgsql
IMMUTABLE STRICT;

CREATE TABLE static.question (
	id SERIAL NOT NULL,
	task_id int4 NOT NULL,
	ord int4 NOT NULL,
	type static.question_type NOT NULL,
	body text NOT NULL,
	choices text[] NOT NULL,
	answers int2[] NOT NULL,
	CHECK (check_question_answers_idx(answers, array_length(choices, 1))),
	UNIQUE (task_id, ord),
	PRIMARY KEY ("id")
);

ALTER TABLE "static"."question" ADD CONSTRAINT "Ref_question_to_task"
	FOREIGN KEY ("task_id") REFERENCES "static"."task"("id")
	MATCH SIMPLE
	ON DELETE CASCADE
	ON UPDATE NO ACTION
	NOT DEFERRABLE;
