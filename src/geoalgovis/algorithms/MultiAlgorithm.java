package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;

import java.util.*;

public class MultiAlgorithm extends SymbolPlacementAlgorithm {

    private static final boolean __show_output__ = true;

    @Override
    public Output doAlgorithm(Input input) {

        ArrayList<Result> results = new ArrayList<>();

        boolean equalradi = true;
        for (Region region : input.regions) if (region.getWeight() != input.regions.get(0).getWeight()) equalradi = false;

        results.add(increasingRadi(new Output(input)));
        if (!equalradi) results.add(decreasingRadi(new Output(input)));
        results.add(centerAreaSpread(new Output(input)));
        results.add(centralFirst(new Output(input)));
        results.add(extremeCentralReplace(new Output(input)));

        // sort output and return best
        Collections.sort(results);
        if (__show_output__) {
            for (Result result : results) {
                System.out.println(result);
            }
            Result result = results.get(0);
            System.out.println(result.algorithmName + " performed best for " + result.output.input.instanceName());
        }

        return results.get(0).output;
    }

    // place away all circles, use PullBack to place smallest radius first
    private Result increasingRadi(Output output) {
        long startTime = System.nanoTime();
        output = Util.placeAway(output);
        output.symbols.sort(Comparator.comparingDouble(Circle::getRadius));
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.Anchor, true);
        new PostProcessAlgorithm().postprocess(output);
        long endTime = System.nanoTime();
        return new Result(output, "increasingRadi", endTime - startTime);
    }

    // place away all circles, use PullBack to place largest radius first
    private Result decreasingRadi(Output output) {
        long startTime = System.nanoTime();
        output = Util.placeAway(output);
        output.symbols.sort(Comparator.comparingDouble(s -> -s.getRadius()));
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.Anchor, true);
        new PostProcessAlgorithm().postprocess(output);
        long endTime = System.nanoTime();
        return new Result(output, "decreasingRadi", endTime - startTime);
    }

    private Result centerAreaSpread(Output output) {
        long startTime = System.nanoTime();
        output = Util.placeAway(output);
        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, null, null);
        new PostProcessAlgorithm().postprocess(output);
        long endTime = System.nanoTime();
        return new Result(output, "centerAreaSpread", endTime - startTime);
    }

    private Result centralFirst(Output output) {
        long startTime = System.nanoTime();
        Util.sortAroundPoint(output.symbols, Util.getAvgAnchor(output.symbols));
        Util.placeAway(output);
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.Anchor, true);
        new PostProcessAlgorithm().postprocess(output);
        long endTime = System.nanoTime();
        return new Result(output, "centralFirst", endTime - startTime);
    }

    private Result extremeCentralReplace(Output output) {
        long startTime = System.nanoTime();
        Vector center = Util.getAvgAnchor(output.symbols);
        for (Symbol s : output.symbols) {
            s.setCenter(center.clone());
            Vector dir = s.vectorToRegion();
            if (dir != null) s.getCenter().translate(dir);
        }
        Util.removeOverlappingCenters(output.symbols);
        new PushAlgorithm().pushRun(output, Util.CandidateGoals.Extrema);
        new PostProcessAlgorithm().postprocess(output);
        long endTime = System.nanoTime();
        return new Result(output, "extremeCentralReplace", endTime - startTime);
    }


    private class Result implements Comparable<Result>{

        final Output output;
        final String algorithmName;
        final double duration;
        final boolean is_valid;
        final double score;

        Result(Output output, String algorithmName, double duration) {
            this.output = output;
            this.algorithmName = algorithmName;
            this.duration = duration / 1000000000;
            this.is_valid = output.isValid();
            this.score = output.computeQuality();
            System.out.println(output.input.instanceName() + " finished running " + algorithmName);
        }

        @Override
        public String toString() {
            String s = algorithmName + " on " + output.input.instanceName() + " took " + duration + " seconds and scored " + score;
            if (is_valid) {
                return s + " (valid)";
            } else {
                return s + " (invalid)";
            }
        }

        @Override
        public int compareTo(Result that) {
            if (this.is_valid && !that.is_valid) return -1;
            if (!this.is_valid && that.is_valid) return 1;
            return Double.compare(this.score, that.score);
        }
    }
}
