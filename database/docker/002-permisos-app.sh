#!/bin/bash
# Solo se ejecuta al inicializar un volumen nuevo.
set -euo pipefail
# NO_BACKSLASH_ESCAPES conserva barras; duplicar apostrofos conserva el literal SQL.
clave_sql=${SIGECAP_DB_PASSWORD//\'/\'\'}
{
    printf "SET SESSION sql_mode='NO_BACKSLASH_ESCAPES';\n"
    printf "CREATE USER 'sigecap_app'@'%%' IDENTIFIED BY '%s';\n" "$clave_sql"
    for tabla in trabajador curso capacitacion participacion asistencia resultado certificado usuario rol permiso rol_permiso; do
        printf "GRANT SELECT, INSERT, UPDATE ON sigecap_jv.%s TO 'sigecap_app'@'%%';\n" "$tabla"
    done
    printf "GRANT DELETE ON sigecap_jv.rol_permiso TO 'sigecap_app'@'%%';\n"
} | MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot
