/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.networking;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Team {
    String name;
    String secret;
    
    ConcurrentMap<ContestProblem,Submission> submissions = new ConcurrentHashMap();
}
