# Compila con JDK y ejecuta con JRE; la ventana se dibuja en el servidor X indicado por DISPLAY.
FROM eclipse-temurin:21-jdk AS compilacion
ARG CONNECTOR_VERSION=26.7.0
WORKDIR /build
ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${CONNECTOR_VERSION}/mysql-connector-j-${CONNECTOR_VERSION}.jar lib/mysql-connector-j.jar
ADD https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/${CONNECTOR_VERSION}/mysql-connector-j-${CONNECTOR_VERSION}.jar.sha1 lib/mysql-connector-j.jar.sha1
# ADD descarga con permisos 600; el usuario sin privilegios necesita leer el JAR.
RUN echo "$(cut -c1-40 lib/mysql-connector-j.jar.sha1)  lib/mysql-connector-j.jar" | sha1sum -c - \
    && chmod 644 lib/mysql-connector-j.jar
COPY src src
RUN find src -name '*.java' > fuentes.txt \
    && javac --release 17 -encoding UTF-8 -d out @fuentes.txt

FROM eclipse-temurin:21-jre
# Bibliotecas X11 y fuentes que AWT/Swing necesitan fuera de un escritorio Linux.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libxext6 libxrender1 libxtst6 libxi6 fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --create-home --uid 1001 sigecap
WORKDIR /app
COPY --from=compilacion /build/out out
COPY --from=compilacion /build/lib/mysql-connector-j.jar lib/mysql-connector-j.jar
USER sigecap
ENTRYPOINT ["java", "-Dawt.useSystemAAFontSettings=on", "-cp", "out:lib/mysql-connector-j.jar", "pe.utp.sigecapjv.vista.FrmPrincipal"]
