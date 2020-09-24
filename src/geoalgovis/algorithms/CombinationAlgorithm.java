package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import geoalgovis.symbolplacement.SymbolPlacementAlgorithm;
import nl.tue.geometrycore.geometry.Vector;

import java.util.Random;

public class CombinationAlgorithm extends SymbolPlacementAlgorithm {

    Random random = new Random(0);

    @Override
    public Output doAlgorithm(Input input) {
//        return case1(input);
//        return case2(input);
        return case3(input);
    }


    private Output case3(Input input) {
        Output output = new Output(input);
        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, null, null);
        new PostProcessAlgorithm().postprocess(output);
        return output;
    }

    private Output case2(Input input) {
        Output output = new Output(input);
        Util.sortAroundPoint(output.symbols, Util.getAvgAnchor(output.symbols));
        Util.placeAway(output);
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.Anchor);
        new PushAlgorithm().pushRun(output, Util.CandidateGoals.All);
        new CenterSpreadAlgorithm().centerSpread(output.symbols, Util.getAvgCenter(output.symbols));

        new PostProcessAlgorithm().postprocess(output);


        return output;
    }

    private Output case1(Input input) {
        Output output = new Output(input);

        Vector center = Util.getAvgCenter(output.symbols);

        for (Symbol s : output.symbols) {
            s.setCenter(center.clone());
            Vector dir = s.vectorToRegion();
            if (dir != null) s.getCenter().translate(dir);
        }
        Util.removeOverlappingCenters(output.symbols);
        new PushAlgorithm().pushRun(output, Util.CandidateGoals.Extrema);
        new PostProcessAlgorithm().postprocess(output);

        return output;
    }
}
