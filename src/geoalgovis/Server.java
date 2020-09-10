/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

import geoalgovis.networking.Receiver;
import geoalgovis.networking.ContestManager;
import java.io.File;
import java.io.IOException;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Server {

    public static void main(String[] args) throws IOException, InterruptedException {

        Settings.init("serversettings.txt");

        //read config
        int contestPort = Settings.getInt("contestPort", 2345);
        File problemDirectory = new File(Settings.getValue("problemDirectory", "problems"));
        File outputDirectory = new File(Settings.getValue("outputDirectory", "output"));
        String jsonLocation = Settings.getValue("jsonLocation", null);
        long jsonDelay = Settings.getInt("jsonDelay", 1000);
        int threads = Settings.getInt("threads", 20);
        String context = Settings.getValue("context","/");

        System.out.println("-------------- Final settings ----------------");
        System.out.println("Working with settings: ");
        System.out.println("\tcontestPort = " + contestPort);
        System.out.println("\tproblemDirectory = " + problemDirectory.getAbsolutePath());
        System.out.println("\toutputDirectory = " + outputDirectory.getAbsolutePath());
        System.out.println("\tjsonLocation = " + (jsonLocation == null ? "null" : jsonLocation));
        System.out.println("\tjsonDelay = " + jsonDelay);
        System.out.println("\tthreads = " + threads);
        System.out.println("\tcontext = " + context);
        System.out.println("------------ End Final settings --------------");

        ContestManager cm = new ContestManager(jsonLocation, jsonDelay);

        if (!problemDirectory.exists()) {
            problemDirectory.mkdirs();
        }
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        cm.initialize();

        Receiver.cm = cm;
        Receiver.startContest();

        System.out.println("Contest started");
    }
}
