package geoalgovis.networking;

import geoalgovis.problem.Problem;
import geoalgovis.gui.ProblemsPane;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class HttpServer {

    private static ServerSocket httpServer;
    private static Thread serverThread;

    private static Runnable httpAccepter = new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted() && !httpServer.isClosed()) {
                try {
                    HttpHandler handler = new HttpHandler(httpServer.accept());
                    new Thread(handler).start();
                } catch (SocketException e) {
                    System.err.println("Http channel closed");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public static void start(int port) {
        try {
            if (serverThread == null || serverThread.isInterrupted()) {
                httpServer = new ServerSocket(port);
                serverThread = new Thread(httpAccepter);
                serverThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        try {
            if (serverThread != null && !serverThread.isInterrupted()) {
                httpServer.close();
                serverThread.interrupt();
                serverThread = null;
                httpServer = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class HttpHandler implements Runnable {

        Socket connection;

        public HttpHandler(Socket s) {
            super();
            connection = s;
        }

        @Override
        public void run() {
            try {
                String response = calculateResponse();
                if (response != null) {
                    answer(response);
                } else {
                    throwError();
                }
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String calculateResponse() throws IOException {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = in.readLine();
                String[] parts = line.split(" ");
                String address = parts[1];
                address = address.replaceAll("\\\\", "/");
                parts = address.split("/");
                if (parts.length >= 2 && parts[1].equals("problem")) {
                    if (parts.length < 3 || parts[2].isEmpty()) {
                        return SolutionLibrary.problemScores();
                    } else {
                        Problem p = ProblemsPane.getProblemSet().getOrDefault(parts[2], null);
                        return SolutionLibrary.problemScores(p);                        
                    }
                } else if (parts.length >= 3 && parts[1].equals("team") && !parts[2].isEmpty()) {
                    return SolutionLibrary.teamScores(parts[2]);
                } else {
                    return SolutionLibrary.teamScores();
                }
            } catch (Exception e) {
                return null;
            }
        }

        private void answer(String output) throws IOException {
            String headers = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "Content-Length: ";
            String outputAfterHeaders = "\r\n\r\n";
            String httpOutput = headers + output.length() + outputAfterHeaders + output;
            PrintWriter out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
            out.println(httpOutput);
            out.close();
        }

        private void throwError() throws IOException {
            String httpOutput = "HTTP/1.1 404 Not Found";
            PrintWriter out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
            out.println(httpOutput);
            out.close();
        }
    }
}
