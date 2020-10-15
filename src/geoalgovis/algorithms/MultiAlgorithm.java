package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;

import java.util.*;
import java.util.function.Function;

public class MultiAlgorithm extends SymbolPlacementAlgorithm {

    private static final boolean __show_output__ = true;

    @Override
    public Output doAlgorithm(Input input) {

        ArrayList<Result> results = new ArrayList<>();
        results.add(run(this::pullBackIncreasingRadi, input, "pullBackIncreasingRadi"));
        results.add(run(this::pullBackDecreasingRadi, input, "pullBackDecreasingRadi"));
        results.add(run(this::pullBackCentralFirst, input, "pullBackCentralFirst"));
        results.add(run(this::centerAreaSpread, input, "centerAreaSpread"));
        results.add(run(this::pushAlgorithm, input, "pushAlgorithm"));

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

    private Result run(Function<Output, Output> method, Input input, String algorithmName) {
        try {
            long startTime = System.nanoTime();
            Output output = new Output(input);
            output = method.apply(output);
            long endTime = System.nanoTime();
            return new Result(output, algorithmName, endTime - startTime);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

//    // place away all circles, use PullBack to place smallest radius first
//    private Result newRun(Output output) {
//        long startTime = System.nanoTime();
//
//        new LP().partitionedLpSolve(output, true, 5, 5);
//        new SwapAlgorithm().swapInvalid(output.symbols);
//        new LP().partitionedLpSolve(output, true, 5, 5);
//        new SwapAlgorithm().swapInvalid(output.symbols);
//        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, .33, 100);
//        new SwapAlgorithm().swapInvalid(output.symbols);
//        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, .33, 100);
//        new SwapAlgorithm().swapInvalid(output.symbols);
//        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, .33, 100);
//        new SwapAlgorithm().swapInvalid(output.symbols);
//        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, .33, 100);
//        new PushAlgorithm().pushRun(output, Util.CandidateGoals.Extrema, 1000d, 0.66, 2d);
//        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, .33, 100);
//        new PostProcessAlgorithm().postprocess(output);
//        new PushAlgorithm().pushRun(output, Util.CandidateGoals.Extrema, 1000d, 0.66, 2d);
//        new PostProcessAlgorithm().postprocess(output);
//
//        long endTime = System.nanoTime();
//        return new Result(output, "newRun", endTime - startTime);
//    }

    // place away all circles, use PullBack to place smallest radius first
    private Output pullBackIncreasingRadi(Output output) {
        output = Util.placeAway(output);
        output.symbols.sort(Comparator.comparingDouble(Circle::getRadius));
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.Anchor, true);
        new PostProcessAlgorithm().postprocess(output);
        return output;
    }

    // place away all circles, use PullBack to place largest radius first
    private Output pullBackDecreasingRadi(Output output) {
        output = Util.placeAway(output);
        output.symbols.sort(Comparator.comparingDouble(s -> -s.getRadius()));
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.Anchor, true);
        new PostProcessAlgorithm().postprocess(output);
        return output;
    }

    // place away all circles, use Pullback to place the symbol closest to the average anchor point first
    private Output pullBackCentralFirst(Output output) {
        Util.sortAroundPoint(output.symbols, Util.getAvgAnchor(output.symbols));
        Util.placeAway(output);
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.Anchor, true);
        new PostProcessAlgorithm().postprocess(output);
        return output;
    }

    // tries to place all symbols as close as possible to the center
    private Output centerAreaSpread(Output output) {
        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, null, null);
        Vector avgAnchor = Util.getAvgAnchor(output.symbols);
        output.symbols.sort(Comparator.comparingDouble(s -> s.getCenter().distanceTo(avgAnchor)));
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.Anchor, true);
        new PostProcessAlgorithm().postprocess(output);
        return output;
    }

    // performs the pushAlgorithm followed by postprocessing
    private Output pushAlgorithm(Output output) {
        new PushAlgorithm().pushRun(output, Util.CandidateGoals.All);
        new PostProcessAlgorithm().postprocess(output);
        return output;
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
