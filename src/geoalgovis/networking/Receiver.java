package geoalgovis.networking;

import geoalgovis.problem.Problem;
import geoalgovis.gui.ProblemsPane;
import geoalgovis.gui.SettingsPane;
import geoalgovis.problem.Solution;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Receiver {

    private static ServerSocket listener;
    private static HashMap<ManagedConnection, Thread> openConnections;
    private static HashMap<String, JoinTeam> teams = new HashMap<>();

    private static Thread dispatcherThread;
    private static Runnable dispatcher = new Runnable() {
        @Override
        public void run() {
            while (!Thread.interrupted() && listener != null && !listener.isClosed()) {
                Socket conn = null;
                try {
                    conn = listener.accept();
                    ManagedConnection m = new ManagedConnection(conn);
                    Thread t = new Thread(m);
                    t.start();
                    openConnections.put(m, t);
                } catch (IOException e) {
                    try {
                        System.err.println("Dropped connection");
                        if (conn != null) {
                            conn.close();
                        }
                    } catch (Exception a) {
                    }
                }
            }
        }
    };

    public static void startContest(int contestPort, int httpPort) {
        File f = new File(SettingsPane.getExportPath() + File.separator + "contest");
        f.mkdirs();
        readTeams();
        SolutionLibrary.loadContestData();
        if (httpPort > 0) {
            HttpServer.start(httpPort);
        }
        if (dispatcherThread == null) {
            try {
                listener = new ServerSocket(contestPort);
                openConnections = new HashMap<>();
                dispatcherThread = new Thread(dispatcher);
                dispatcherThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void stopContest() {
        close();
    }

    public static void close(ManagedConnection m) {
        m.close();
        Thread t = openConnections.get(m);
        t.interrupt();
        openConnections.remove(m);
    }

    public static void close() {
        if (openConnections != null) {
            for (ManagedConnection m : openConnections.keySet()) {
                close(m);
            }
            openConnections = null;
        }
        HttpServer.stop();
        if (dispatcherThread != null) {
            dispatcherThread.interrupt();
            dispatcherThread = null;
            if (listener != null) {
                try {
                    listener.close();
                    listener = null;
                } catch (IOException e) {
                }
            }
        }
    }

    public static JoinTeam registerTeam(JoinTeam t) {
        JoinTeam existingTeam = teams.getOrDefault(t.getTeamName(), null);
        if (t.getTeamName() == null || t.getTeamName().isEmpty()) {
            t.setApproved(false);
            t.setError("Teamname cannot be empty");
            return t;
        }
        boolean alphaNumericOnly = true;
        for (char c : t.getTeamName().toCharArray()) {
            if ('a' <= c && c <= 'z') {
                // OK
            } else if ('A' <= c && c <= 'Z') {
                // OK
            } else if ('0' <= c && c <= '9') {
                // OK
            } else {
                alphaNumericOnly = false;
                break;
            }
        }
        if (!alphaNumericOnly) {
            t.setApproved(false);
            t.setError("Teamname can only contain letters (upper/lowercase) or numbers");
            return t;
        }
        if (existingTeam != null) {
            if (!existingTeam.getSharedSecret().equals(t.getSharedSecret())) {
                t.setApproved(false);
                t.setError("The shared secret is incorrect for this team.");
                return t;
            }
        }
        t.setApproved(true);
        teams.put(t.getTeamName(), t);
        writeTeams();
        return t;
    }

    public static void registerSolution(JoinTeam t, ContestSolution cs) {
        Problem p = ProblemsPane.getProblemSet().getOrDefault(cs.getProblemName(), null);
        if (p == null) {
            return;
        }
        Solution s = p.loadSolution("generic", cs.getSolutionRepresentation());
        if (s == null) {
            return;
        }
        SolutionLibrary.addSolution(p, s, t);
    }

    private static void writeTeams() {
        File f = new File(SettingsPane.getExportPath() + File.separator + "contest" + File.separator + "teams.txt");
        try {
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            String json = "[\r\n";
            int i = 0;
            for (String team : teams.keySet()) {
                if (i != 0) {
                    json += ",\r\n";
                }
                i++;
                json += "\t" + teams.get(team).toJson();
            }
            json += "\r\n]";
            PrintWriter w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f)));
            w.println(json);
            w.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readTeams() {
        File file = new File(SettingsPane.getExportPath() + File.separator + "contest" + File.separator + "teams.txt");
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                String json = new String(data, "UTF-8");
                String objects = json.split("\\s*\\[\\s*")[1].split("\\s*\\]\\s*")[0].trim();
                String stripedBrackets = objects.split("^\\{")[1].split("\\}$")[0];
                String[] allObjectsUnquoted = stripedBrackets.split("\\s*\\}\\s*,\\s*\\{\\s*");
                HashMap<String, JoinTeam> tempTeams = new HashMap<>();
                for (String obj : allObjectsUnquoted) {
                    JoinTeam t = JoinTeam.fromJson("{" + obj + "}");
                    if (t != null) {
                        tempTeams.put(t.getTeamName(), t);
                    }
                }
                teams = tempTeams;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static HashMap<String, JoinTeam> getTeams() {
        return teams != null ? teams : new HashMap<>();
    }

    public static boolean contestStarted() {
        if (dispatcherThread != null) {
            return dispatcherThread.isAlive();
        } else {
            return false;
        }
    }

}
