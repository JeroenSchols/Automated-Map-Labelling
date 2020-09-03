package geoalgovis.networking;

import java.io.Serializable;

public class JoinTeam implements Serializable {
    String teamName;
    String sharedSecret;
    boolean approved;
    String error;

    public JoinTeam(String teamName, String sharedSecret){
        this.teamName = teamName;
        this.sharedSecret = sharedSecret;
        this.approved = false;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String toJson(){
        return "{\"teamName\":\""+this.getTeamName()+"\", " +
                "\"sharedSecret\":\""+this.getSharedSecret()+"\", " +
                "\"approved\":\""+String.valueOf(this.approved)+"\"}";
    }

    public static JoinTeam fromJson(String json){
        try{
            String object = json.split("^\\s*\\{\\s*")[1].split("\\s*\\{\\s*$")[0];
            String[] parts = json.split(",");
            if(parts.length != 3){
                return null;
            }
            String teamName = parts[0].split(":")[1].split("\"")[1] ;
            String sharedSecret = parts[1].split(":")[1].split("\"")[1] ;
            String approved = parts[2].split(":")[1].split("\"")[1] ;
            JoinTeam t = new JoinTeam(teamName, sharedSecret);
            if(approved.equalsIgnoreCase("true")){
                t.setApproved(true);
            }else{
                t.setApproved(false);
            }
            return t;
        }catch(Exception e){}
        return null;
    }
}
