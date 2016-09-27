#!/bin/sh

DBADMIN=${DBADMIN:-dimitrijer}
DBNAME=formlogic
DBUSER=formlogic
DIR=`dirname $0`
SCRIPTS_DIR=${DIR}/../resources/sql
SCRIPTS=(db_create.sql
	 assignments.sql
	 users.sql)

set -xeu

echo "Recreating DB ${DBNAME}..."
dropdb -U ${DBADMIN} --if-exists ${DBNAME}
createdb -U ${DBADMIN} -O ${DBUSER} ${DBNAME} -E "UTF-8"

echo "Executing demo data..."
for script in ${SCRIPTS[@]}; do
	psql -v ON_ERROR_STOP=1 -U ${DBUSER} -d ${DBNAME} < ${SCRIPTS_DIR}/${script}
done

echo "Done!"
