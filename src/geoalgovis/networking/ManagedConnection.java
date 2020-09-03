package geoalgovis.networking;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ManagedConnection implements Runnable, Closeable {

    Socket conn;
    JoinTeam team;
    ObjectInputStream inStream;
    ObjectOutputStream outStream;

    public ManagedConnection(Socket socket) throws IOException {
        conn = socket;
        inStream = new ObjectInputStream(socket.getInputStream());
        outStream = new ObjectOutputStream(socket.getOutputStream());
    }

    public void run(){
        try {
            if (registerTeam()) {
                while (!Thread.interrupted()) {
                    receiveSolution();
                }
            }
        }catch (Exception e){}
        Receiver.close(this);
    }

    public void close(){
        if(inStream != null && !conn.isInputShutdown()){
            try {
                inStream.close();
            }catch (IOException e){}
        }
        if(outStream != null){
            try {
                outStream.close();
            }catch(IOException e){}
        }
        if(conn != null){
            try{
                conn.close();
            }catch (IOException e){}
        }
    }

    public boolean registerTeam() throws ClassCastException, IOException, ClassNotFoundException{
        JoinTeam t = (JoinTeam) inStream.readObject();
        team = Receiver.registerTeam(t);
        outStream.writeObject(team);
        if(t.isApproved()){
            return true;
        }
        return false;
    }

    public void receiveSolution() throws ClassCastException, IOException, ClassNotFoundException{
        ContestSolution s = (ContestSolution) inStream.readObject();
        Receiver.registerSolution(team, s);
    }
}
