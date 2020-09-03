/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Region;
import geoalgovis.symbolplacement.Symbol;
import geoalgovis.symbolplacement.SymbolPlacementAlgorithm;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class LineupAlgorithm extends SymbolPlacementAlgorithm {

    @Override
    public Output doAlgorithm(Input input) {

        // creating an output creates the associated symbols, all the algorithm really needs to do is set their centers!
        // these are initialized to the anchors (some arbirary point inside the polygon)
        Output output = new Output(input);

        // sort regions by increasing weight
        // to make sure this doesn't mess up the input object, let's first copy the list
        List<Region> regions = new ArrayList(input.regions);
        regions.sort((r1, r2) -> Double.compare(r1.getWeight(), r2.getWeight()));

        double c = 0;
        for (Region r : regions) {
            c += r.getWeight();
            Symbol s = output.symbols.get(r.getIndex()); // nb: input and output symbols are in the same order, use the region's index to get the right symbol
            s.getCenter().set(c, 0); // note: could also use .translate() to move it about
            c += r.getWeight();
        }

        // and make sure to return the result
        return output;
    }

}
