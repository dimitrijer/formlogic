#!/bin/sh
# Provisioning script for Vagrant box.

set -xeu

# Install and init Postgres and JDK.
yum install -y postgresql-server postgresql-contrib java-1.8.0-openjdk-headless
postgresql-setup initdb

# Allow local access (both IP and Unix sockets - psql) for everyone.
sed -i "s/\(peer\|ident\)/trust/g" /var/lib/pgsql/data/pg_hba.conf
systemctl enable postgresql
systemctl start postgresql

# Get Leiningen.
cd /usr/bin
wget https://raw.github.com/technomancy/leiningen/stable/bin/lein
chmod a+x /usr/bin/lein

# Don't panic when run as root.
echo "export LEIN_ROOT=true" >> /root/.bashrc
# Bind nREPL port to public interface.
echo "export LEIN_REPL_HOST=192.168.33.12" >> /root/.bashrc
lein --version 2>/dev/null

# Create DB role.
createuser -U postgres -d -l formlogic

# Prepare DB.
cd /formlogic
sh ./bin/demo_data.sql
