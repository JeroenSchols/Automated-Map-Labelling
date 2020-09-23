package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.curved.Circle;

import java.util.*;

public class MultiAlgorithm extends SymbolPlacementAlgorithm {

    private static final boolean __show_output__ = true;

    @Override
    public Output doAlgorithm(Input input) {

        ArrayList<Result> results = new ArrayList<>();

        boolean equalradi = true;
        for (Region region : input.regions) if (region.getWeight() != input.regions.get(0).getWeight()) equalradi = false;

        results.add(increasingRadiPullBack(new Output(input)));
        if (!equalradi) results.add(decreasingRadiPullBack(new Output(input)));
        results.add(avgCenterSpreadPullBack(new Output(input)));

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
    private Result increasingRadiPullBack(Output output) {
        long startTime = System.nanoTime();
        output = Util.placeAway(output);
        output.symbols.sort(Comparator.comparingDouble(Circle::getRadius));
        new PullBackAlgorithm().pullBack(output, null, null, null, null);
        long endTime = System.nanoTime();
        return new Result(output, "increasingRadiPullBack", endTime - startTime);
    }

    // place away all circles, use PullBack to place largest radius first
    private Result decreasingRadiPullBack(Output output) {
        long startTime = System.nanoTime();
        output = Util.placeAway(output);
        output.symbols.sort(Comparator.comparingDouble(s -> -s.getRadius()));
        new PullBackAlgorithm().pullBack(output, null, null, null, null);
        long endTime = System.nanoTime();
        return new Result(output, "decreasingRadiPullBack", endTime - startTime);
    }

    // spread all circles around the average initial center, pull back in order of spread
    private Result avgCenterSpreadPullBack(Output output) {
        long startTime = System.nanoTime();
        new CenterSpreadAlgorithm().centerSpread(output.symbols, Util.getAvgCenter(output.symbols));
        new PullBackAlgorithm().pullBack(output, null, null, null, null);
        long endTime = System.nanoTime();
        return new Result(output, "avgCenterSpreadPullBack", endTime - startTime);
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
        }

        @Override
        public String toString() {
            if (is_valid) {
                return output.input.instanceName() + " solved by " + algorithmName + " took " + duration + " and scored " + score + " (valid)";
            } else {
                return output.input.instanceName() + " solved by " + algorithmName + " took " + duration + " and scored " + score + " (invalid)";
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
