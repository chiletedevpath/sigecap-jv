#!/bin/bash
# Lo ejecuta la imagen oficial de MySQL una sola vez, al inicializar el volumen.
# Crea la cuenta de la aplicación solo con los permisos del flujo implementado.
# Usa el cliente por socket: desde Windows el montaje es ejecutable y no recibe las funciones del entrypoint.
set -e
tablas="trabajador curso capacitacion participacion asistencia resultado certificado"
{
    echo "CREATE USER '${SIGECAP_DB_USER}'@'%' IDENTIFIED BY '${SIGECAP_DB_PASSWORD}';"
    for tabla in $tablas; do
        echo "GRANT SELECT, INSERT, UPDATE ON sigecap_jv.${tabla} TO '${SIGECAP_DB_USER}'@'%';"
    done
} | MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot
