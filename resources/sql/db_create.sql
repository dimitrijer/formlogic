-- User table.
CREATE TABLE public.user(
	id SERIAL NOT NULL,
	email text NOT NULL,
	password text NOT NULL,
	admin boolean NOT NULL DEFAULT false,
	PRIMARY KEY("id")
);
