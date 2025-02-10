package org.example;

import java.io.* ;
import java.util.* ;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

//This project can loaded the templates "index.html", "404.html" and the image "tokyo_ghoul.jpg"
//to the web server. All this is the manual process behind frameworks like Spring Boot to give responses
//to the users who wants to get information in your website through HTTP request

public final class ServidorWeb {
    public static void main(String argv[]) throws Exception {
        int puerto = 8080;

        ServerSocket serverSocket = new ServerSocket(puerto);

        System.out.println("Server waiting for connection...");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Connection Accepted");

            SolicitudHttp solicitudHttp = new SolicitudHttp(socket);

            Thread hilo = new Thread(solicitudHttp);
            hilo.start();

        }

    }
}

final class SolicitudHttp implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    public SolicitudHttp(Socket socket) throws Exception
    {
        this.socket = socket;
    }

    public void run() {
        try {
            proceseSolicitud();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void proceseSolicitud() throws Exception {
        OutputStream out = socket.getOutputStream();

        InputStream in = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String lineaDeSolicitud = reader.readLine();

        System.out.println();
        System.out.println(lineaDeSolicitud);

        String nombreArchivo = "";

        String linea = "";
        while ((linea = reader.readLine()) != null && !linea.isEmpty()) {
            //System.out.println(linea);

            StringTokenizer partesLinea = new StringTokenizer(lineaDeSolicitud);
            String method = partesLinea.nextToken();

            if (method.equals("GET")) {
                nombreArchivo = "." + partesLinea.nextToken();
                System.out.println("Archivo solicitado: " + nombreArchivo);
                break;
            }
        }

        var outBuffer = new BufferedOutputStream(socket.getOutputStream());
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(nombreArchivo);

        if (inputStream != null){

            File file = new File(ClassLoader.getSystemResource(nombreArchivo).toURI());
            long fileSize = file.length();
            enviarString("HTTP/1.1 200 OK" + CRLF, outBuffer);
            enviarString("Content-Type: " + contentType(nombreArchivo) + CRLF, outBuffer);
            enviarString("Content-Length: " + fileSize + CRLF, outBuffer);
            enviarString(CRLF, outBuffer);
            enviarBytes(inputStream, outBuffer);
            inputStream.close();

        } else {
            InputStream error = ClassLoader.getSystemResourceAsStream("404.html");
            enviarString("HTTP/1.0 404 Not Found" + CRLF, outBuffer);
            enviarString("Content-Type: text/html; charset=UTF-8" + CRLF, outBuffer);
            enviarString(CRLF, outBuffer);

            if (error != null){
                File file = new File(ClassLoader.getSystemResource("404.html").toURI());
                long fileSize = file.length();
                enviarString("Content-Length: " + fileSize + CRLF, outBuffer);
                enviarBytes(error, outBuffer);
            } else {
                String msg = "<html><body><h1> No se encontro el archivo </h1></body></html>";
                enviarString("Content-Length: " + msg.length() + CRLF + CRLF, outBuffer);
                enviarString(msg, outBuffer);
            }

        }

        outBuffer.flush();
        out.close();
        reader.close();
        socket.close();
    }

    private static void enviarString(String line, OutputStream os) throws Exception {
        os.write(line.getBytes(StandardCharsets.UTF_8));
    }

    private static void enviarBytes(InputStream fis, OutputStream os) throws Exception {

        byte[] buffer = new byte[1024];
        int bytes = 0;

        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String nombreArchivo) {
        if(nombreArchivo.endsWith(".htm") || nombreArchivo.endsWith(".html")) {
            return "text/html";
        }
        if(nombreArchivo.endsWith(".jpg")) {
            return "image/jpg";
        }
        if(nombreArchivo.endsWith(".png")) {
                return "image/png";
        }

        return "application/octet-stream";
    }


}
