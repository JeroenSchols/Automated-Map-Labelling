package geoalgovis.networking;

import geoalgovis.Settings;
import geoalgovis.problem.Problem;
import geoalgovis.problem.Solution;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ContestManager {

    public final String jsonLocation;
    public final long delay;

    private final Timer timer = new Timer();
    private TimerTask task = null;
    private final ExportLock lock = new ExportLock();

    private final ConcurrentMap<String, Team> teams = new ConcurrentHashMap();
    private final ConcurrentMap<String, ContestProblem> problems = new ConcurrentHashMap();

    public ContestManager(String jsonLocation, long delay) {
        this.jsonLocation = jsonLocation;
        this.delay = delay;
    }

    private class ExportLock {

        boolean exporting = false;
        int updating = 0;

        public synchronized void startExport() throws InterruptedException {
            exporting = true;
            while (updating > 0) {
                wait();
            }
        }

        public synchronized void endExport() {
            exporting = false;
            notifyAll();
        }

        public synchronized void startUpdate() throws InterruptedException {
            while (exporting) {
                wait();
            }
            updating++;
        }

        public synchronized void endUpdate() {
            updating--;
            notifyAll();
        }
    }

    public Team checkTeam(String team, String secret) throws InterruptedException {

        if (!team.matches("[A-Za-z0-9]+") || !secret.matches("[A-Za-z0-9]+")) {
            return null;
        }
        
        Team t = teams.get(team);
        if (t == null) {
            System.out.println("New team: " + t.name);
            lock.startUpdate();
            t = new Team();
            t.name = team;
            t.secret = secret;
            teams.put(team, t);
            File teamdir = new File(Settings.getValue("outputDirectory"), t.name + "_" + t.secret);
            teamdir.mkdirs();
            lock.endUpdate();
            return t;
        } else if (t.secret.equals(secret)) {
            return t;
        } else {
            return null;
        }
    }

    public ContestProblem getProblem(String problem) {
        return problems.get(problem);
    }

    public void addSolution(Team team, ContestProblem problem, String solution) throws InterruptedException {
        lock.startUpdate();

        Solution s = problem.problem.loadSolution(solution);
        boolean valid = s.isValid();
        double score = s.computeQuality();

        synchronized (team) {

            Submission sub = team.submissions.get(problem);
            if (sub == null) {
                sub = new Submission();
                sub.problem = problem;
                sub.team = team;
                sub.score = Double.POSITIVE_INFINITY;
                sub.valid = false;
                problem.submissions.put(team, sub);
                team.submissions.put(problem, sub);
            }

            if ((valid && !sub.valid) || (valid == sub.valid && sub.score > score)) {
                System.out.println("Better solution " + team.name + " -- " + problem.name);
                // better solution
                sub.score = score;
                sub.valid = valid;
                updateJSONfiles(problem);
                File teamdir = new File(Settings.getValue("outputDirectory"), team.name + "_" + team.secret);
                teamdir.mkdirs();

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(teamdir, problem.name + ".txt")))) {
                    writer.write(s.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ContestManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        lock.endUpdate();
    }

    private String teamSummaryJSON(TeamSum ts) {
        if (ts == null) {
            return null;
        }

        String json = "{\r\n";
        json += "\t\"team\" : \"" + ts.team.name + "\",\r\n";

        List<Submission> subs = new ArrayList(ts.team.submissions.values());
        subs.sort((s, t) -> {
            return -Double.compare(s.computePoints(), t.computePoints());
        });

        boolean first = true;
        json += "\t\"solved\" : " + ts.valid + ",\r\n";
        json += "\t\"score\" : " + ts.score + ",\r\n";
        json += "\t\"points\" : " + ts.points + ",\r\n";
        json += "\t\"solutions\" : [\r\n";
        for (Submission sub : subs) {
            if (first) {
                first = false;
            } else {
                json += ",\r\n";
            }
            json += "\t\t{\"problemName\":\"" + sub.problem.name
                    + "\", \"valid\":" + sub.valid
                    + ", \"score\": " + sub.score
                    + ", \"points\": " + sub.computePoints()
                    + "}";
        }
        json += "\r\n\t]\r\n";
        json += "}";
        return json;
    }

    private String teamOverviewJSON(List<TeamSum> tsums) {

        tsums.sort((s, t) -> {
            return -Double.compare(s.points, t.points);
        });

        boolean first = true;
        String output = "[\r\n";
        for (TeamSum ts : tsums) {
            if (first) {
                first = false;
            } else {
                output += ",\r\n";
            }

            output += "\t{\"team\":\"" + ts.team.name + "\", \"solved\":" + ts.valid + ", \"score\":" + ts.score + ", \"points\":" + ts.points + "}";

        }
        output += "\r\n]";
        return output;
    }

    private String problemSummaryJSON(ContestProblem p) {
        if (p == null) {
            return null;
        }

        String json = "{\r\n";
        json += "\t\"problem\" : \"" + p.name + "\",\r\n";

        List<Submission> subs = new ArrayList(p.submissions.values());
        subs.sort((s, t) -> {
            return -Double.compare(s.score, t.score);
        });

        boolean first = true;
        json += "\t\"solutions\" : [\r\n";
        for (Submission sub : subs) {
            if (first) {
                first = false;
            } else {
                json += ",\r\n";
            }
            json += "\t\t{\"teamName\":\"" + sub.team.name
                    + "\", \"valid\":" + sub.valid
                    + ", \"score\": " + sub.score
                    + ", \"points\": " + sub.computePoints()
                    + "}";
        }
        json += "\r\n\t]\r\n";
        json += "}";
        return json;
    }

    private String problemOverviewJSON() {
        List<ContestProblem> sortproblems = new ArrayList(problems.values());

        sortproblems.sort((s, t) -> {
            return -s.name.compareTo(t.name);
        });

        boolean first = true;
        String output = "[\r\n";
        for (ContestProblem problem : sortproblems) {

            if (first) {
                first = false;
            } else {
                output += ",\r\n";
            }

            if (problem.best == null) {
                output += "\t{\"problemName\":\"" + problem.name + "\", \"score\":\"-\", \"teamName\":\"-\", \"valid\":\"-\"}";
            } else {
                output += "\t{\"problemName\":\"" + problem.name + "\", \"score\":\"" + problem.best.score + "\", \"teamName\":\"" + problem.best.team.name + "\", \"valid\":\"" + problem.best.valid + "\"}";
            }
        }
        output += "\r\n]";
        return output;
    }

    public void initialize() throws InterruptedException {
        // load problems
        System.out.println("Loading problems");
        for (File file : new File(Settings.getValue("problemDirectory")).listFiles()) {
            if (!file.isDirectory()) {

                Problem p = Settings.definition.createInputInstance();
                p.loadProblem(file);
                if (!p.isValidInstance()) {
                    return;
                }
                ContestProblem cp = new ContestProblem();
                cp.name = p.instanceName();
                cp.problem = p;
                cp.jsonupdate = true;
                problems.put(cp.name, cp);
            }
        }
        System.out.println("  Loaded " + problems.size() + " problems");
        // load teams and solutions
        System.out.println("Loading teams");
        for (File file : new File(Settings.getValue("outputDirectory")).listFiles()) {
            if (file.isDirectory()) {

                Team team = new Team();
                String[] split = file.getName().split("_");
                team.name = split[0];
                team.secret = split[1];
                teams.put(team.name, team);
                System.out.println("  " + team.name);
                for (File sub : file.listFiles()) {
                    String instance = sub.getName().substring(0, sub.getName().length() - 4);
                    ContestProblem p = problems.get(instance);

                    Solution sol = p.problem.loadSolution(sub);
                    Submission submission = new Submission();
                    submission.team = team;
                    submission.problem = p;
                    submission.score = sol.computeQuality();
                    submission.valid = sol.isValid();
                    team.submissions.put(p, submission);
                    p.submissions.put(team, submission);
                }
            }
        }
        writeAllJSONfiles();
    }

    private void updateJSONfiles(ContestProblem p) {
        if (jsonLocation != null) {
            synchronized (timer) {
                p.jsonupdate = true;
                if (task == null) {
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            task = null;
                            try {
                                writeAllJSONfiles();
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ContestManager.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    };
                    timer.schedule(task, delay);
                }
            }
        }
    }

    private class TeamSum {

        Team team;
        int valid;
        double points;
        double score;
    }

    private void writeAllJSONfiles() throws InterruptedException {
        if (jsonLocation != null) {
            lock.startExport();

            System.out.println("Writing all json files " + new Date());
            File dir = new File(jsonLocation);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File problemDir = new File(dir, "problem");
            if (!problemDir.exists()) {
                problemDir.mkdir();
            }
            File teamDir = new File(dir, "team");
            if (!teamDir.exists()) {
                teamDir.mkdir();
            }

            try {

                for (ContestProblem p : problems.values()) {
                    if (!p.jsonupdate) {
                        continue;
                    }
                    p.updateBest();
                }

                List<TeamSum> tmap = new ArrayList();
                for (Team team : teams.values()) {
                    TeamSum ts = new TeamSum();
                    ts.points = 0;
                    ts.valid = 0;
                    ts.score = 0;
                    ts.team = team;
                    tmap.add(ts);
                    for (Submission s : team.submissions.values()) {
                        if (s.valid) {
                            ts.valid++;
                            ts.points += s.computePoints();
                            ts.score += s.score;
                        }
                    }
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir, "problems.json")));
                writer.write(problemOverviewJSON());
                writer.close();

                for (ContestProblem p : problems.values()) {
                    if (!p.jsonupdate) {
                        continue;
                    }
                    writer = new BufferedWriter(new FileWriter(new File(problemDir, p.name + ".json")));
                    writer.write(problemSummaryJSON(p));
                    writer.close();
                    p.jsonupdate = false;
                }

                writer = new BufferedWriter(new FileWriter(new File(dir, "teams.json")));
                writer.write(teamOverviewJSON(tmap));
                writer.close();

                for (TeamSum ts : tmap) {
                    writer = new BufferedWriter(new FileWriter(new File(teamDir, ts.team.name + ".json")));
                    writer.write(teamSummaryJSON(ts));
                    writer.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.out.println(" Done");

            lock.endExport();
        }
    }
}
