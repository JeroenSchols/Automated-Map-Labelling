package geoalgovis.networking;

import geoalgovis.problem.Problem;
import geoalgovis.gui.Settings;
import geoalgovis.problem.Solution;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

public class Sender {

    private static String connectionString;
    private static String teamName;
    private static String sharedSecret;
    private static Socket contestChannel;
    private static ObjectOutputStream outStream;
    private static ObjectInputStream inStream;
    private static JoinTeam team;

    public static String joinContest(){
        team = new JoinTeam(getTeamName(), getSharedSecret());
        try{
            URL url = new URL(getConnectionString());
            Socket socket = new Socket(url.getHost(), 2345);
            ObjectOutputStream oo = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream oi = new ObjectInputStream(socket.getInputStream());
            oo.writeObject(team);
            JoinTeam t = (JoinTeam) oi.readObject();
            if(!t.isApproved()){
                if(t.getError() != null){
                    return t.getError();
                }else{
                    return "An unexpected error occurred while registering your team";
                }
            }
            setContestChannel(socket);
            setOutStream(oo);
            setInStream(oi);
            return null;
        }catch(MalformedURLException e){
            e.printStackTrace();
            System.err.println("Malformed URL exception: "+e.getMessage());
            return "The contest url is not valid";
        }catch(UnknownHostException e){
            e.printStackTrace();
            System.err.println("Host not valid: "+e.getMessage());
            return "The contest url is not valid";
        }catch(IOException e){
            e.printStackTrace();
            System.err.println("Problem establishing connection to contest: "+e.getMessage());
            return "The contest url is not valid";
        }catch(ClassCastException | ClassNotFoundException e){
            e.printStackTrace();
            System.err.println("An unexpected error was encountered: " + e.getMessage());
            return "An unexpected error was encountered";
        }catch(Exception e){
            //for very unexpected errors
            e.printStackTrace();
            System.err.println("An unexpected error was encountered: " + e.getMessage());
            return "An unexpected error was encountered";
        }
    }

    public static void leaveContest(){
        close();
    }

    public static void sendSolution(Problem p, Solution s){
        if(isJoinedContest()) {
            ContestSolution cs = new ContestSolution(team, p.instanceName(), s.toString());
            if(contestChannel != null && outStream != null && !contestChannel.isOutputShutdown()){
                try {
                    outStream.writeObject(cs);
                }catch(IOException e){
                    e.printStackTrace();
                }
            }else{
                close();
                System.err.println("The contest has been closed by the server");
            }
        }
    }

    public static String getConnectionString() {
        if(connectionString == null){
            connectionString = Settings.getValue("connectionString", "");
            return connectionString;
        }
        return connectionString;
    }

    public static void setConnectionString(String connectionString) {
        Sender.connectionString = connectionString;
        Settings.setValue("connectionString", Sender.connectionString);
    }

    public static String getTeamName() {
        if(teamName == null){
            teamName = Settings.getValue("teamName", "");
            return teamName;
        }
        return teamName;
    }

    public static void setTeamName(String teamName) {
        Sender.teamName = teamName;
        Settings.setValue("teamName", Sender.teamName);
    }

    public static String getSharedSecret() {
        if(sharedSecret == null){
            sharedSecret = Settings.getValue("sharedSecret", "");
            return sharedSecret;
        }
        return sharedSecret;
    }

    public static void setSharedSecret(String sharedSecret) {
        if(sharedSecret != null){
            sharedSecret = sharedSecret.trim();
        }
        Sender.sharedSecret = sharedSecret;
        Settings.setValue("sharedSecret", Sender.sharedSecret);
    }

    public static boolean isJoinedContest() {
        if(contestChannel == null){
            return false;
        }
        if(!contestChannel.isOutputShutdown() && !contestChannel.isInputShutdown() && !contestChannel.isClosed()){
            return true;
        }else{
            return false;
        }
    }

    private static Socket getContestChannel() {
        return contestChannel;
    }

    private static void setContestChannel(Socket contestChannel) {
        Sender.contestChannel = contestChannel;
    }

    private static ObjectOutputStream getOutStream() {
        return outStream;
    }

    private static void setOutStream(ObjectOutputStream outStream) {
        Sender.outStream = outStream;
    }

    private static ObjectInputStream getInStream() {
        return inStream;
    }

    private static void setInStream(ObjectInputStream inStream) {
        Sender.inStream = inStream;
    }

    private static void close(){
        try {
            if (contestChannel != null && inStream != null && !contestChannel.isInputShutdown()) {
                inStream.close();
                inStream = null;
            }
            if (contestChannel != null && outStream != null && !contestChannel.isOutputShutdown()) {
                outStream.close();
                outStream = null;
            }
            if(contestChannel != null && !contestChannel.isClosed()){
                contestChannel.close();
                contestChannel = null;
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
