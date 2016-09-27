INSERT INTO "public"."user"(email, password, admin)
VALUES('dimitrijer@etf.rs', md5('qwe123')::text, true);

INSERT INTO "public"."user"(email, password)
values ('rd090112d@student.etf.rs', md5('qwe123')::text);
