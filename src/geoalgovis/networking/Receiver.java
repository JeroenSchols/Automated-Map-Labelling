package geoalgovis.networking;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import geoalgovis.Settings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Receiver implements HttpHandler {

    public static ContestManager cm;
    private static HttpServer server;

    public static void startContest() throws IOException {
        if (server != null) {
            stopContest();
        }
        int port = Settings.getInt("contestPort", 2345);
        int threads = Settings.getInt("threads", 20);
        String context = Settings.getValue("context","/");
        System.out.println("Starting server");
        System.out.println("  port: " + port);
        System.out.println("  theads: " + threads);
        System.out.println("  context: " + context);
        server = HttpServer.create(new InetSocketAddress(port), 2*threads);
        server.createContext(context, new Receiver());
        server.setExecutor((ThreadPoolExecutor) Executors.newFixedThreadPool(threads));
        server.start();
    }

    public static void stopContest() {
        server.stop(0);
        server = null;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(he.getRequestBody()));
        String answer = getAnswer(in);
        in.close();

        byte[] bytes = answer.getBytes();
        he.sendResponseHeaders(200, bytes.length);
        OutputStream out = he.getResponseBody();
        out.write(bytes);
        out.flush();
        out.close();
    }

    private String getAnswer(BufferedReader in) {
        try {

            String line = in.readLine();

            String[] split = line.split("\t");
            switch (split.length) {
                case 2: {
                    Team team = cm.checkTeam(split[0], split[1]);
                    if (team == null) {
                        return "Team not found, shared secret is incorrect, or invalid characters are used.";
                    }
                    return "";
                }
                case 3: {
                    StringBuilder sb = new StringBuilder();
                    line = in.readLine();
                    while (line != null) {
                        sb.append(line).append("\n");
                        line = in.readLine();
                    }
                    Team team = cm.checkTeam(split[0], split[1]);
                    if (team == null) {
                        return "Team not found, shared secret is incorrect, or invalid characters are used.";
                    }
                    ContestProblem problem = cm.getProblem(split[2]);
                    if (problem == null) {
                        return "Cannot find problem for submitted solution";
                    }
                    cm.addSolution(team, problem, sb.toString());
                    return "";
                }
                default:
                    return "Unexpected message format";
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            return "Unexpected error";
        } catch (IOException ex) {
            Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            return "IO exception in server";
        }
    }
}
