-- Ejecutar con una cuenta administrativa en la instancia utilizada por Java.
-- Ajustar la cuenta y el host si la instalación utiliza otros valores.
USE sigecap_jv;
GRANT DELETE ON sigecap_jv.curso TO 'sigecap_app'@'%';
