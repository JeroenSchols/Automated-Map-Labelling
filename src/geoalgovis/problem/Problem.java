package geoalgovis.problem;

import geoalgovis.gui.Viewable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;

public abstract class Problem<TOutput extends Solution> implements Viewable {

    private String instanceName;
    private HashMap<String, TOutput> solutions;

    /**
     * The name of the general problem. Unique per class.
     *
     * @return Name of the problem
     */
    public abstract String generalName();

    public String instanceName() {
        return instanceName;
    }
    
    public abstract boolean isValidInstance();

    /**
     * File that should contain a problem definition
     *
     * @param f
     */
    public final void loadProblem(File f) {
        try {
            instanceName = f.getName().substring(0, f.getName().length() - 4);

            loadProblem(fileToString(f));
        } catch (Exception e) {
            System.err.println("An " + e.getClass().getSimpleName() + " occurred while loading the file");
        }

    }

    /**
     * File that should contain a problem definition
     *
     * @param f
     */
    public abstract void loadProblem(String f);

    /**
     * Represent problem as a String
     */
    public abstract String writeProblem();

    /**
     * Load solution from file
     *
     * @param f
     * @return
     */
    public final TOutput loadSolution(String name, File f) {
        return loadSolution(name, fileToString(f));
    }

    public final TOutput loadSolution(String name, String solution) {
        TOutput s = loadSolution(solution);
        if (s != null) {
            putSolution(name, s);
        }
        return s;
    }
    public TOutput loadSolution(File file) {
        return loadSolution(fileToString(file));
    }

    public abstract TOutput loadSolution(String solution);

    /**
     * adds or updates a solution in the set
     *
     * @param name identifier for the solution
     * @param solution representation of the solution
     */
    public void putSolution(String name, TOutput solution) {
        solutions = solutions == null ? new HashMap<>() : solutions;
        if (name != null && !name.isEmpty()) {
            solutions.put(name, solution);
        }
    }

    /**
     * Removes a solution from the set of solutions
     *
     * @param name identifier of the solution
     */
    public void removeSolution(String name) {
        solutions = solutions == null ? new HashMap<>() : solutions;
        if (name != null && !name.isEmpty()) {
            solutions.remove(name);
        }
    }

    /**
     * Get solution known by a specified name. If not known null will be
     * returned
     *
     * @param name identifier of the solution
     * @return Solution format
     */
    public Solution getSolution(String name) {
        solutions = solutions == null ? new HashMap<>() : solutions;
        try {
            if (name != null && !name.isEmpty()) {
                return solutions.get(name);
            }
        } catch (ClassCastException e) {
        }
        return null;
    }

    public Collection<TOutput> getAllSolutions() {
        if (solutions != null) {
            return solutions.values();
        } else {
            return new ArrayList<>();
        }
    }

    private static String fileToString(File f) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(f.toPath(), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
            return contentBuilder.toString();
        } catch (IOException e) {
            System.err.println("Error occurred while coverting " + f.getAbsolutePath() + " to text");
            e.printStackTrace();
        }
        return null;

    }

}
