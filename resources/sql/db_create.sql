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

-- Assignments.
CREATE TABLE static.assignment (
	id SERIAL NOT NULL,
	name text NOT NULL,
	PRIMARY KEY("id")
);

-- Tasks specific to assignments.
CREATE TABLE static.task (
	id int4 NOT NULL,
	assignment_id int4 NOT NULL,
	contents text NOT NULL,
	PRIMARY KEY("id", "assignment_id")
);

-- Types of questions.
CREATE TYPE static.question_type AS ENUM ('fill', 'single', 'multiple');

-- Questions specific to a task.
CREATE TABLE static.question (
	id int4 NOT NULL,
	task_id int4 NOT NULL,
	type static.question_type NOT NULL,
	body text NOT NULL,
	answer text NOT NULL,
	PRIMARY KEY ("id", "task_id")
);
