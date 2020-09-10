/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

import geoalgovis.gui.MainPane;
import geoalgovis.gui.ProblemsPane;
import geoalgovis.gui.SettingsPane;
import geoalgovis.networking.Receiver;
import geoalgovis.networking.ContestManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.swing.WindowConstants;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Settings.init("guisettings.txt");
        MainPane frame = new MainPane();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

}
