package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import geoalgovis.symbolplacement.SymbolPlacementAlgorithm;
import nl.tue.geometrycore.geometry.Vector;

import java.util.Comparator;

public class CombinationAlgorithm extends SymbolPlacementAlgorithm {

    @Override
    public Output doAlgorithm(Input input) {
        Output output = new Output(input);

        Vector center = Util.getAvgCenter(output.symbols);

        for (Symbol s : output.symbols) {
            s.setCenter(center.clone());
            Vector dir = s.vectorToRegion();
            if (dir != null) s.getCenter().translate(dir);
        }

        // displaces two symbols minimally in case their centers are equivalent
        boolean valid;
        do {
            valid = true;
            outer: for (Symbol s1 : output.symbols) {
                for (Symbol s2 : output.symbols) {
                    if (s1.hashCode() >= s2.hashCode()) continue;
                    if (s1.getCenter().distanceTo(s2.getCenter()) == 0) {
                        s1.getCenter().translate(0.0001, 0.0001);
                        valid = false;
                        break outer;
                    }
                }
            }
        } while(!valid);

        new PushAlgorithm().pushRun(output, Util.CandidateGoals.Extrema);
        output.symbols.sort(Comparator.comparingDouble(Symbol::distanceToRegion));
        new PullBackAlgorithm().pullBack(output, null, null, 180.0, Util.CandidateGoals.Anchor);
        output.symbols.sort(Comparator.comparingDouble(Symbol::distanceToRegion));
        new PullBackAlgorithm().pullBack(output, null, null, null, Util.CandidateGoals.All);

        return output;
    }
}
