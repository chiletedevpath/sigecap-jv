package pe.utp.sigecapjv.util;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class CorreoRecuperacion {
    private CorreoRecuperacion() { }

    public static void enviar(String destino, String codigo) {
        String clave = claveLocal();
        String remitente = System.getenv("SIGECAP_CORREO_REMITENTE");
        if (remitente == null || remitente.isBlank()) {
            String local = System.getenv("LOCALAPPDATA");
            if (local != null) {
                try { remitente = Files.readString(Path.of(local,"SIGECAP","remitente.txt")).strip(); }
                catch (java.io.IOException ignorada) { /* Puede configurarse mediante variables de entorno. */ }
            }
        }
        if (remitente == null || remitente.isBlank())
            throw new IllegalStateException("El envío de correos aún no está configurado.");
        String cuerpo = "{\"sender\":{\"name\":\"SIGECAP J&V\",\"email\":" + json(remitente)
                + "},\"to\":[{\"email\":" + json(destino) + "}],\"subject\":\"Recuperar contraseña - SIGECAP J&V\","
                + "\"textContent\":" + json("Su código de recuperación es: " + codigo
                + "\nVence en 10 minutos y solo puede usarse una vez.\nSi no solicitó este cambio, ignore este correo.") + "}";
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(15)).header("api-key", clave)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8)).build();
            var respuesta = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
                    .send(peticion, HttpResponse.BodyHandlers.discarding());
            if (respuesta.statusCode() != 201) throw new IllegalStateException("No se pudo enviar el código. Intente más tarde.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió el envío del código.");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("No se pudo enviar el código. Revise su conexión e intente nuevamente.");
        }
    }

    private static String claveLocal() {
        String clave = System.getenv("SIGECAP_BREVO_API_KEY");
        if (clave != null && !clave.isBlank()) return clave;
        String local = System.getenv("LOCALAPPDATA");
        if (local == null || !Files.isRegularFile(Path.of(local, "SIGECAP", "brevo-key.dpapi")))
            throw new IllegalStateException("El envío de correos aún no está configurado.");
        // Windows descifra únicamente para el usuario que guardó la clave.
        String script = "$ErrorActionPreference='Stop'; Import-Module (Join-Path $PSHOME 'Modules/Microsoft.PowerShell.Security/Microsoft.PowerShell.Security.psd1'); "
                + "$s=Get-Content -Raw -LiteralPath (Join-Path $env:LOCALAPPDATA 'SIGECAP/brevo-key.dpapi') | ConvertTo-SecureString; "
                + "$p=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($s); try { [Console]::Out.Write([Runtime.InteropServices.Marshal]::PtrToStringBSTR($p)) } "
                + "finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($p); $s.Dispose() }";
        try {
            String windows = System.getenv("SystemRoot");
            Process proceso = new ProcessBuilder(Path.of(windows,"System32","WindowsPowerShell","v1.0","powershell.exe").toString(),
                    "-NoProfile", "-NonInteractive", "-Command", script).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            proceso.getOutputStream().close();
            if (!proceso.waitFor(10, TimeUnit.SECONDS)) { proceso.destroyForcibly(); throw new IllegalStateException("No se pudo leer la configuración de correo."); }
            String resultado = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            if (proceso.exitValue() != 0 || resultado.isBlank()) throw new IllegalStateException("No se pudo leer la configuración de correo.");
            return resultado;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Se interrumpió la lectura de configuración.");
        } catch (java.io.IOException ex) { throw new IllegalStateException("No se pudo leer la configuración de correo."); }
    }

    private static String json(String texto) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 32) b.append(String.format("\\u%04x", (int)c));
            else b.append(c);
        }
        return b.append('"').toString();
    }
}
