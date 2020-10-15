package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;

import java.util.Comparator;

class PostProcessAlgorithm {

    private static final boolean __show_output__ = false;

    /**
     * performs all postprocessing algorithms until convergence
     * guarantees that a solution remains valid when the input is valid
     * guarantees that a solution quality will not reduce then the solution is valid
     * does not guarantee that an invalid solution becomes valid
     *
     * uses all default parameters
     *
     * @param output a output to be post-processed
     */
    void postprocess(Output output) {
        this.postprocess(output,null,null,null,null, null, Util.CandidateGoals.Extrema, null);
    }

    /**
     * performs all postprocessing algorithms until convergence
     * guarantees that a solution remains valid when the input is valid
     * guarantees that a solution quality will not reduce then the solution is valid
     * does not guarantee that an invalid solution becomes valid
     *
     * @param output a output to be post-processed
     * @param max_iter_process the maximum number of iterations to apply postprocessing, terminates earlier when converged
     * @param max_iter_pullback the maximum number of iterations to apply pullBack, terminates earlier when converged
     * @param min_delta_process the minimum relative change to consider until convergence of postprocessing is not reached
     * @param min_delta_pullback the minimum relative change to consider until convergence of pullback is not reached
     * @param radi_count the number of alignment-lines to consider
     * @param cGoals which goals to try pulling towards using pullBack
     * @param uniform whether rays are uniformly distributed
     */
    void postprocess(Output output, Integer max_iter_process, Integer max_iter_pullback, Double min_delta_process, Double min_delta_pullback, Integer radi_count, Util.CandidateGoals cGoals, Boolean uniform) {
        if (max_iter_process == null) max_iter_process = 25;
        if (min_delta_process == null) min_delta_process = 0.0001;

        double current_quality = output.computeQuality();
        double prev_quality = 2*current_quality;
        if (__show_output__) System.out.println("postprocessing iteration 0 = " + current_quality);
        for (int iter = 0; iter < max_iter_process && (1+min_delta_process)*current_quality < prev_quality; iter++) {
            prev_quality = current_quality;
            output.symbols.sort(Comparator.comparingDouble(Symbol::distanceToRegion));
            new SwapAlgorithm().swap(output.symbols);
            new PullBackAlgorithm().pullBack(output, max_iter_pullback, min_delta_pullback, 1, Util.CandidateGoals.Anchor, true);
            new PullBackAlgorithm().pullBack(output, max_iter_pullback, min_delta_pullback, radi_count, cGoals, uniform);
            current_quality = output.computeQuality();
            if (__show_output__) System.out.println("postprocessing iteration " + (iter + 1) + " = " + current_quality);
        }

        // indicate when this run did not converge properly
        if (__show_output__ && (1+min_delta_process)*current_quality < prev_quality) System.err.println(
                "postprocess did not converge on " + output.getName() +
                        ", with max_iter = " + max_iter_process +
                        ", and min_delta = " + min_delta_process
        );
    }
}
