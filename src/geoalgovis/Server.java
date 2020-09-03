/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

import geoalgovis.gui.ProblemsPane;
import geoalgovis.gui.Settings;
import geoalgovis.gui.SettingsPane;
import geoalgovis.networking.Receiver;
import geoalgovis.networking.SolutionLibrary;
import java.io.File;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Server {

    public static void main(String[] args) {

        //read config
        int contestPort = Settings.getInt("contestPort", 2345);
        int httpPort = Settings.getInt("httpPort", -1);
        File problemDirectory = new File(Settings.getValue("problemDirectory", "problems"));
        File outputDirectory = new File(Settings.getValue("outputDirectory", "output"));
        String jsonLocation = Settings.getValue("jsonLocation", null);
        long jsonDelay = Settings.getInt("jsonDelay", 1000);

        System.out.println("-------------- Final settings ----------------");
        System.out.println("Working with settings: ");
        System.out.println("\tcontestPort = " + contestPort);
        System.out.println("\thttpPort = " + httpPort);
        System.out.println("\tproblemDirectory = " + problemDirectory.getAbsolutePath());
        System.out.println("\toutputDirectory = " + outputDirectory.getAbsolutePath());
        System.out.println("\tjsonLocation = " + (jsonLocation == null ? "null" : jsonLocation));
        System.out.println("\tjsonDelay = " + jsonDelay);
        System.out.println("------------ End Final settings --------------");

        SolutionLibrary.delay = jsonDelay;
        SolutionLibrary.jsonLocation = jsonLocation;

        if (!problemDirectory.exists()) {
            problemDirectory.mkdirs();
        }
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        //load problems
        SettingsPane.setExportPath(outputDirectory);
        ProblemsPane pane = new ProblemsPane(null, problemDirectory);

        Receiver.startContest(contestPort, httpPort);

        System.out.println("Contest started");
    }
}
