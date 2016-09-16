#!/bin/sh

DBNAME=formlogic
DBUSER=formlogic
DIR=`dirname $0`
DB_CREATE=${DIR}/../resources/sql/db_create.sql

set -xeu

echo "Recreating DB ${DBNAME}..."
dropdb --if-exists ${DBNAME}
createdb -O ${DBUSER} ${DBNAME} -E "UTF-8"

echo "Executing demo data..."
psql -v ON_ERROR_STOP=1 -U ${DBUSER} -d ${DBNAME} < ${DB_CREATE}

echo "Done!"
