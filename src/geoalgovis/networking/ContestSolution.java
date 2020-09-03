package geoalgovis.networking;

import java.io.Serializable;

public class ContestSolution implements Serializable {
    private String solutionRepresentation;
    private String problemName;
    private JoinTeam team;

    public ContestSolution(JoinTeam team, String problemName, String solutionRepresentation){
        this.team = team;
        this.problemName = problemName;
        this.solutionRepresentation = solutionRepresentation;
    }

    public String getSolutionRepresentation() {
        return solutionRepresentation;
    }

    public void setSolutionRepresentation(String solutionRepresentation) {
        this.solutionRepresentation = solutionRepresentation;
    }

    public String getProblemName() {
        return problemName;
    }

    public void setProblemName(String problemName) {
        this.problemName = problemName;
    }

    public JoinTeam getTeam() {
        return team;
    }

    public void setTeam(JoinTeam team) {
        this.team = team;
    }
}
