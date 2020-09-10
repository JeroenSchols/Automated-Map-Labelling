/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.networking;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Submission {

    Team team;
    ContestProblem problem;
    boolean valid;
    double score;

    double computePoints() {
        if (!valid) {
            return 0;
        }
        // 0: not best
        // 1: best of its team for the problem
        // 2: best over all teams for the problem

        if (problem.best == this) {
            return 10.0;
        } else if (score <= 0.0001) {
            return 10.0;
        } else {
            return 10 * Math.sqrt(problem.best.score) / Math.sqrt(score);
        }
    }
}
