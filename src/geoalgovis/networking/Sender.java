package geoalgovis.networking;

import geoalgovis.problem.Problem;
import geoalgovis.Settings;
import geoalgovis.problem.Solution;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class Sender {

    private static boolean joined = false;

    public static String joinContest() {
        joined = false;
        String conn = Settings.getValue("connectionString", null);
        String team = Settings.getValue("teamName", null);
        String secret = Settings.getValue("sharedSecret", null);
        if (conn == null || conn.trim().isEmpty()) {
            return "Specify a host URL";
        }
        if (team == null || team.trim().isEmpty()) {
            return "Specify a team name";
        }
        if (secret == null || secret.trim().isEmpty()) {
            return "Specify a secret";
        }

        try {
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();

            HttpPost request = new HttpPost(conn);
            StringEntity params = new StringEntity(team + "\t" + secret, ContentType.create("text/plain", "UTF-8"));
            request.setEntity(params);

            CloseableHttpResponse response = httpClient.execute(request);
            if (response == null) {
                System.err.println("No server response");
                httpClient.close();
                return "No server response";
            }
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");

            if (responseString != null && !responseString.isEmpty()) {
                response.close();
                httpClient.close();
                System.err.println("Error: " + responseString);
                return responseString;
            }

            response.close();
            httpClient.close();
            joined = true;
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Connection error occurred";
        }

    }

    public static void leaveContest() {
        joined = false;
    }

    public static void sendSolution(Problem p, Solution s) {
        if (joined) {
            String conn = Settings.getValue("connectionString", null);
            String team = Settings.getValue("teamName", null);
            String secret = Settings.getValue("sharedSecret", null);
            if (conn == null || conn.trim().isEmpty()) {
                return;
            }
            if (team == null || team.trim().isEmpty()) {
                return;
            }
            if (secret == null || secret.trim().isEmpty()) {
                return;
            }

            try {
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost request = new HttpPost(conn);
                StringEntity params = new StringEntity(team + "\t" + secret + "\t" + p.instanceName() + "\n" + s.toString(), ContentType.create("text/plain", "UTF-8"));
                request.setEntity(params);

                CloseableHttpResponse response = httpClient.execute(request);
                if (response == null) {
                    System.err.println("No server response");
                    httpClient.close();
                    return;
                }
                HttpEntity entity = response.getEntity();
                String responseString = EntityUtils.toString(entity, "UTF-8");

                System.out.println("" + responseString);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static String getConnectionString() {
        return Settings.getValue("connectionString", "");
    }

    public static void setConnectionString(String connectionString) {
        Settings.setValue("connectionString", connectionString);
    }

    public static String getTeamName() {
        return Settings.getValue("teamName", "");
    }

    public static void setTeamName(String teamName) {
        Settings.setValue("teamName", teamName);
    }

    public static String getSharedSecret() {
        return Settings.getValue("sharedSecret", "");
    }

    public static void setSharedSecret(String sharedSecret) {
        if (sharedSecret != null) {
            sharedSecret = sharedSecret.trim();
        }
        Settings.setValue("sharedSecret", sharedSecret);
    }

    public static boolean isJoinedContest() {
        return joined;
    }

}
