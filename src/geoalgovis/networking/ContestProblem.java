/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.networking;

import geoalgovis.problem.Problem;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class ContestProblem {

    String name;
    Problem problem;

    boolean jsonupdate = false;

    Submission best = null;

    ConcurrentMap<Team, Submission> submissions = new ConcurrentHashMap();

    void updateBest() {
        best = null;
        for (Submission sub : submissions.values()) {
            if (sub.valid && (best == null || sub.score < best.score)) {
                best = sub;
            }
        }
    }
}
