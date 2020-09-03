package geoalgovis.networking;

import geoalgovis.problem.Problem;
import geoalgovis.gui.ProblemsPane;
import geoalgovis.gui.SettingsPane;
import geoalgovis.problem.Solution;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;

public class SolutionLibrary {

    public static String jsonLocation = null;
    public static long delay = 1000;
    private static Timer timer = new Timer();
    private static TimerTask task = null;
    private static final Set<Problem> toRefresh = new HashSet();

    private static HashMap<String, SolutionSet> perProblem;
    private static HashMap<String, SolutionSet> perTeam;
    private static String problemScores;
    private static String teamScores;
    private static SolutionSet all = new SolutionSet();
    private static final Object allLock = new Object();

    static void addSolution(Problem p, Solution s, JoinTeam t) {
        s.setName(md5(s.toString()));
        s.writeSolution(new File(SettingsPane.getExportPath() + File.separator + "contest" + File.separator + s.getName() + ".txt"));
        SolutionData data = new SolutionData(t, p, s);
        synchronized (allLock) {
            all.add(data);
            perTeam = null;
            perProblem = null;
            teamScores = null;
            problemScores = null;
        }
        writeContestData();
        updateJSONfiles(p);
    }

    static HashMap<String, SolutionSet> getTeamData() {
        synchronized (allLock) {
            if (perTeam == null) {
                perTeam = all.filterByTeam();
            }
            return perTeam;
        }
    }

    static SolutionSet getTeamData(String name) {
        return getTeamData().getOrDefault(name, null);
    }

    static HashMap<String, SolutionSet> allTeams() {
        HashMap<String, SolutionSet> allTeams = new HashMap<>();
        for (String team : Receiver.getTeams().keySet()) {
            allTeams.put(team, new SolutionSet());
        }
        return allTeams;
    }

    static HashMap<String, SolutionSet> getProblemData() {
        synchronized (allLock) {
            if (perProblem == null) {
                perProblem = all.filterByProblem();
            }
            return perProblem;
        }
    }

    static SolutionSet getProblemData(String name) {
        return getProblemData().getOrDefault(name, null);
    }

    static HashMap<String, SolutionSet> allProblems() {
        HashMap<String, SolutionSet> allProblems = new HashMap<>();
        for (String problem : ProblemsPane.getProblemSet().keySet()) {
            allProblems.put(problem, new SolutionSet());
        }
        return allProblems;
    }

    private static class TeamSummary implements Comparable<TeamSummary> {

        String name;
        int solved;
        double score;
        double points;

        @Override
        public int compareTo(TeamSummary o) {
            return -Double.compare(points, o.points);
        }
    }

    public static String teamScores(String team) {
        if (SolutionLibrary.allTeams().getOrDefault(team, null) == null) {
            return null;
        }

        String json = "{\r\n";
        json += "\t\"team\" : \"" + team + "\",\r\n";

        SolutionLibrary.SolutionSet set = SolutionLibrary.getTeamData(team);
        if (set != null && set.size() > 0) {
            json += "\t\"solved\" : " + set.totalValidSolutions() + ",\r\n";
            json += "\t\"score\" : " + set.totalScore() + ",\r\n";
            json += "\t\"points\" : " + set.totalPoints() + ",\r\n";
            json += "\t\"solutions\" : \r\n";
            json += "\t" + set.toJson((d1, d2) -> {
                return d1.submissionTime.compareTo(d2.submissionTime);
            }).replaceAll("\\r\\n", "\r\n\t");
            json += "\r\n";
        } else {
            json += "\t\"solved\" : 0,\r\n";
            json += "\t\"score\" : 0,\r\n";
            json += "\t\"points\" : 0,\r\n";
            json += "\t\"solutions\" : [] \r\n";
        }
        json += "}";
        return json;
    }

    public static String teamScores() {
        synchronized (allLock) {
            if (teamScores == null) {
                List<TeamSummary> teams = new ArrayList();
                HashMap<String, SolutionSet> allTeamData = all.filterByTeam(allTeams());
                for (String team : allTeamData.keySet()) {
                    SolutionSet teamSolutions = allTeamData.get(team);
                    TeamSummary ts = new TeamSummary();
                    ts.name = team;
                    ts.solved = teamSolutions.totalValidSolutions();
                    ts.points = teamSolutions.totalPoints();
                    ts.score = teamSolutions.totalScore();
                    teams.add(ts);
                }

                Collections.sort(teams);

                String output = "[\r\n";
                for (int i = 0; i < teams.size(); i++) {
                    TeamSummary ts = teams.get(i);
                    output += "\t{\"team\":\"" + ts.name + "\", \"solved\":" + ts.solved + ", \"score\":" + ts.score + ", \"points\":" + ts.points;

                    if (i != teams.size() - 1) {
                        output += "},\r\n";
                    } else {
                        output += "}\r\n";
                    }
                }
                output += "]";
                teamScores = output;
            }
            return teamScores;
        }
    }

    public static String problemScores(Problem p) {
        if (p == null) {
            return null;
        }

        String json = "{\r\n";
        json += "\t\"problem\" : \"" + p.instanceName() + "\",\r\n";
        json += "\t\"solutions\" : \r\n";
        if (allProblems().getOrDefault(p.instanceName(), null) != null) {
            SolutionSet set = getProblemData(p.instanceName());
            if (set != null && set.size() > 0) {
                json += "\t" + set.toJson((SolutionData d1, SolutionData d2) -> {
                    return -Double.compare(d1.getPoints(), d2.getPoints());
                }).replaceAll("\\r\\n", "\\\r\\\n\\\t") 
                        + "\r\n";
            } else {
                json += "\t[]\r\n";
            }
        } else {
            json += "\t[]\r\n";
        }
        //TODO: write out problem....
        // json += "\t\"definition\" : \"" + p.writeProblem().replaceAll("\\r\\n", "\\\\n\\\\r") + "\"";
        json += "}";
        return json;
    }

    public static String problemScores() {
        synchronized (allLock) {
            if (problemScores == null) {
                HashMap<String, SolutionSet> allProblemData = all.filterByProblem(allProblems());

                String output = "[\r\n";
                List<String> problemsSorted = new ArrayList(allProblemData.keySet());
                problemsSorted.sort((p, q) -> {
                    try {
                        int pi = Integer.parseInt(p);
                        int qi = Integer.parseInt(q);
                        return Integer.compare(pi, qi);
                    } catch (NumberFormatException ex) {
                        return p.compareTo(q);
                    }
                });
                Iterator<String> i = problemsSorted.iterator();
                while (i.hasNext()) {
                    String problem = i.next();
                    SolutionSet sols = allProblemData.get(problem);
                    if (sols.best == null) {
                        output += "\t{\"problemName\":\"" + problem + "\", \"score\":\"-\", \"teamName\":\"-\", \"solutionName\":\"-\", \"valid\":\"-\", \"submissionTime\":\"-\"";
                    } else {
                        output += "\t{\"problemName\":\"" + problem + "\", \"score\":\"" + sols.best.getScore() + "\", \"teamName\":\"" + sols.best.getTeamName() + "\", \"solutionName\":\"" + sols.best.getSolutionName() + "\", \"valid\":" + sols.best.getValid() + ", \"submissionTime\":\"" + sols.best.getSubmissionTime() + "\"";
                    }
                    if (i.hasNext()) {
                        output += "},\r\n";
                    } else {
                        output += "}\r\n";
                    }
                }
                output += "]";
                problemScores = output;
            }
            return problemScores;
        }
    }

    static void loadContestData() {
        File f = new File(SettingsPane.getExportPath() + File.separator + "contest" + File.separator + "contest.txt");
        if (f.exists()) {
            String fileContent = "";
            try (FileInputStream s = new FileInputStream(f)) {
                byte[] contents = new byte[1024];

                int bytesRead = 0;
                while ((bytesRead = s.read(contents)) != -1) {
                    fileContent += new String(contents, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (allLock) {
                all = SolutionSet.fromJson(fileContent);
                if (all == null) {
                    all = new SolutionSet();
                }
            }
        }

        synchronized (toRefresh) {
            toRefresh.addAll(ProblemsPane.getProblems());
        }
        writeAllJSONfiles();
    }

    static void writeContestData() {
        System.out.println("writing contest data");
        File f = new File(SettingsPane.getExportPath() + File.separator + "contest" + File.separator + "contest.txt");
        if (f.exists()) {
            f.delete();
        }
        try (PrintWriter w = new PrintWriter(f)) {
            w.println(all.toJson(null));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void updateJSONfiles(Problem p) {
        if (jsonLocation != null) {
            synchronized (toRefresh) {
                toRefresh.add(p);
            }
            if (task != null) {
                task.cancel();
                task = null;
            }
            task = new TimerTask() {
                @Override
                public void run() {
                    writeAllJSONfiles();
                }
            };
            timer.schedule(task, delay);
        }
    }

    private static void writeAllJSONfiles() {
        if (jsonLocation != null) {
            synchronized (allLock) {
                System.out.println("Writing all");
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
                    BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dir, "problems.json")));
                    writer.write(problemScores());
                    writer.close();

                    synchronized (toRefresh) {
                        for (Problem p : toRefresh) {
                            writer = new BufferedWriter(new FileWriter(new File(problemDir, p.instanceName() + ".json")));
                            writer.write(problemScores(p));
                            writer.close();
                        }
                        toRefresh.clear();
                    }

                    writer = new BufferedWriter(new FileWriter(new File(dir, "teams.json")));
                    writer.write(teamScores());
                    writer.close();

                    for (String team : allTeams().keySet()) {
                        writer = new BufferedWriter(new FileWriter(new File(teamDir, team + ".json")));
                        writer.write(teamScores(team));
                        writer.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                System.out.println("done writing all");
            }
        }
    }

    protected static class SolutionSet extends HashSet<SolutionData> {

        SolutionData best = null;

        public SolutionSet() {
            super();
        }

        public static SolutionSet fromJson(String json) {
            try {
                String objects = json.split("\\[")[1].split("\\]")[0].trim();
                String stripedBrackets = objects.split("^\\{")[1].split("\\}$")[0];
                String[] allObjectsUnquoted = stripedBrackets.split("\\s*\\}\\s*,\\s*\\{\\s*");
                SolutionSet set = new SolutionSet();
                for (String solutionData : allObjectsUnquoted) {
                    SolutionData d = SolutionData.fromJson("{" + solutionData + "}");
                    if (d != null) {
                        set.add(d);
                    }
                }
                return set;
            } catch (Exception e) {
                System.err.println("Error on loading JSON");
            }
            return null;
        }

        @Override
        public boolean add(SolutionData e) {
            boolean added = super.add(e);
            if (added && e.valid && (best == null || e.score < best.score)) {
                best = e;
            }
            return added;
        }

        public String toJson(Comparator<SolutionData> comp) {
            List<SolutionData> ordered = new ArrayList(this);
            if (comp != null) {
                ordered.sort(comp);
            }

            String json = "[\r\n";
            int i = 0;
            for (SolutionData d : ordered) {
                if (i != 0) {
                    json += ",\r\n";
                }
                i++;
                json += "\t" + d.toJson();
            }
            json += "\r\n]";
            return json;
        }

        public HashMap<String, SolutionSet> filterByTeam() {
            return filterByTeam(new HashMap<String, SolutionSet>());
        }

        public HashMap<String, SolutionSet> filterByTeam(HashMap<String, SolutionSet> map) {
            for (SolutionData data : this) {
                if (!map.containsKey(data.getTeamName())) {
                    map.put(data.getTeamName(), new SolutionSet());
                }
                map.get(data.getTeamName()).add(data);
            }
            return map;
        }

        public HashMap<String, SolutionSet> filterByProblem() {
            return filterByProblem(new HashMap<>());
        }

        public HashMap<String, SolutionSet> filterByProblem(HashMap<String, SolutionSet> map) {
            for (SolutionData data : this) {
                if (!map.containsKey(data.getProblemName())) {
                    map.put(data.getProblemName(), new SolutionSet());
                }
                map.get(data.getProblemName()).add(data);
            }
            return map;
        }

        public Double totalScore() {
            Map<String, Double> problemToScore = new HashMap();
            for (SolutionData d : this) {
                double oldscore = problemToScore.getOrDefault(d.problemName, Double.POSITIVE_INFINITY);
                if (d.valid && d.score < oldscore) {
                    problemToScore.put(d.problemName, d.score);
                }
            }
            Double score = 0.0;
            for (Double s : problemToScore.values()) {
                score += s;
            }
            return score;
        }

        public Double totalPoints() {
            Map<String, SolutionData> problemToScore = new HashMap();
            for (SolutionData d : this) {
                SolutionData oldbest = problemToScore.getOrDefault(d.problemName, null);
                if (d.valid && (oldbest == null || d.score < oldbest.score)) {
                    problemToScore.put(d.problemName, d);
                }
            }
            HashMap<String, SolutionSet> problemData = getProblemData();
            Double points = 0.0;
            for (Entry<String, SolutionData> e : problemToScore.entrySet()) {
                SolutionData best = problemData.get(e.getKey()).best;
                if (best == e.getValue()) {
                    points += 10.0;
                } else {
                    points += 10.0 * Math.sqrt(best.score) / Math.sqrt(e.getValue().score);
                }
            }
            return points;
        }

        public int totalValidSolutions() {
            Set<String> solved = new HashSet();
            int score = 0;
            for (SolutionData d : this) {
                if (d.valid && !solved.contains(d.problemName)) {
                    score++;
                    solved.add(d.problemName);
                }
            }
            return score;
        }

        public SolutionData getBestValidScore() {
            return best;
        }
    }

    protected static class SolutionData {

        String teamName;
        String problemName;
        String solutionName;
        boolean valid;
        Double score;
        Date submissionTime;

        public SolutionData() {

        }

        public SolutionData(JoinTeam t, Problem p, Solution s) {
            teamName = t.getTeamName();
            problemName = p.instanceName();
            solutionName = s.getName();
            valid = s.isValid();
            score = s.computeQuality();
            submissionTime = new Date();
        }

        public String getTeamName() {
            return teamName;
        }

        public void setTeamName(String teamName) {
            this.teamName = teamName;
        }

        public String getProblemName() {
            return problemName;
        }

        public void setProblemName(String problemName) {
            this.problemName = problemName;
        }

        public String getSolutionName() {
            return solutionName;
        }

        public void setSolutionName(String solutionName) {
            this.solutionName = solutionName;
        }

        public boolean getValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Date getSubmissionTime() {
            return submissionTime;
        }

        public void setSubmissionTime(Date submissionTime) {
            this.submissionTime = submissionTime;
        }

        public double getPoints() {
            if (!valid) {
                return 0;
            }
            // 0: not best
            // 1: best of its team for the problem
            // 2: best over all teams for the problem
            SolutionSet problemset = getProblemData().get(problemName);
            if (problemset.best == this) {
                return 10.0;
            }
            SolutionSet teamset = problemset.filterByTeam().get(teamName);
            if (teamset.best == this) {
                return 10 * Math.sqrt(problemset.best.score) / Math.sqrt(teamset.best.score);
            } else {
                return 0;
            }
        }

        public String toJson() {
            return "{\"teamName\":\"" + teamName
                    + "\", \"problemName\":\"" + problemName
                    + "\", \"solutionName\":\"" + solutionName
                    + "\", \"valid\":" + valid
                    + ", \"score\": "
                    + score + ", \"points\": " + getPoints()
                    + ", \"submissionTime\":\"" + submissionTime.getTime()
                    + "\" }";
        }

        public static SolutionData fromJson(String data) {
            String[] variables = null;
            try {
                SolutionData s = new SolutionData();
                variables = data.split("\\{")[1].split("\\}")[0].split(",");
                s.teamName = variables[0].split(":")[1].split("\"")[1];
                s.problemName = variables[1].split(":")[1].split("\"")[1];
                s.solutionName = variables[2].split(":")[1].split("\"")[1];
                s.valid = variables[3].split(":")[1].trim().split(" ")[0].trim().equalsIgnoreCase("true");
                s.score = Double.parseDouble(variables[4].split(":")[1].trim().split(" ")[0].trim());
                if (variables.length > 5) {
                    s.submissionTime = new Date(Long.parseLong(variables[6].split(":")[1].split("\"")[1]));
                } else {
                    s.submissionTime = new Date();
                }
                return s;
            } catch (Exception e) {
                System.err.println("Error on loading JSON");
                return null;
            }
        }

    }

    private static String md5(String text) {
        try {
            byte[] bytesOfMessage = text.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] theDigest = md.digest(bytesOfMessage);
            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, theDigest);

            // Convert message digest into hex value
            return no.toString(16);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String sha512(String input) {
        try {
            // getInstance() method is called with algorithm SHA-512
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            // return the HashText
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

}
