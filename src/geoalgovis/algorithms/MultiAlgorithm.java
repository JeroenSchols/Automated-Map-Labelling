package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.curved.Circle;

import java.util.*;

public class MultiAlgorithm extends SymbolPlacementAlgorithm {

    @Override
    public Output doAlgorithm(Input input) {

        ArrayList<Result> results = new ArrayList<>();

        results.add(pullBackAlgorithmIncreasingRadi(new Output(input)));
        results.add(pullBackAlgorithmDecreasingRadi(new Output(input)));

        Collections.sort(results);

        for (Result result : results) System.out.println(result);

        return results.get(0).output;
    }

    private Result pullBackAlgorithmIncreasingRadi(Output output) {
        long startTime = System.nanoTime();
        output = Util.placeAway(output);
        output.symbols.sort(Comparator.comparingDouble(Circle::getRadius));
        output = new PullBackAlgorithm().pullBack(output, true, null, null, null);
        long endTime = System.nanoTime();
        return new Result(output, "pullBackAlgorithmIncreasingRadi", endTime - startTime);
    }

    private Result pullBackAlgorithmDecreasingRadi(Output output) {
        long startTime = System.nanoTime();
        output = Util.placeAway(output);
        output.symbols.sort(Comparator.comparingDouble(s -> -s.getRadius()));
        output = new PullBackAlgorithm().pullBack(output, true, null, null, null);
        long endTime = System.nanoTime();
        return new Result(output, "pullBackAlgorithmDecreasingRadi", endTime - startTime);
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
                return algorithmName + " solving " + output.getName() + " took " + duration + " sec and gave score " + score + "(valid)";
            } else {
                return algorithmName + " solving " + output.getName() + " took " + duration + " sec and gave score " + score + "(valid)";
            }
        }

        @Override
        public int compareTo(Result that) {
            if (this.is_valid && !that.is_valid) return 1;
            if (!this.is_valid && that.is_valid) return -1;
            return Double.compare(that.score, this.score);
        }
    }
}
