package geoalgovis.problem;

import geoalgovis.gui.Viewable;
import java.io.*;

public abstract class Solution implements Viewable, Serializable {

    /**
     * Duration before solution was constructed
     */
    private long duration;
    private String name;

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
        
    public abstract boolean isValid();
    
    public abstract double computeQuality();

    @Override
    public abstract String toString();

    public boolean writeSolution(File exportFile) {
        try {
            if (exportFile.exists()) {
                exportFile.delete();
            }
            exportFile.createNewFile();
            FileOutputStream out = new FileOutputStream(exportFile);
            out.write(this.toString().getBytes());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
