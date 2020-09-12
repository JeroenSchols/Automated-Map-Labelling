/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.symbolplacement;

import geoalgovis.algorithms.GravitationalAlgorithm;
import geoalgovis.algorithms.MultiAlgorithm;
import geoalgovis.algorithms.PullBackAlgorithm;
import geoalgovis.problem.ProblemDefinition;
import geoalgovis.algorithms.LineupAlgorithm;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class SymbolPlacement extends ProblemDefinition<Input,Output,SymbolPlacementAlgorithm> {

    @Override
    public Input createInputInstance() {
        return new Input();
    }

    @Override
    public Output createOutputFor(Input input) {
        return new Output(input);
    }

    @Override
    public SymbolPlacementAlgorithm[] getAlgorithms() {
        return new SymbolPlacementAlgorithm[]{
            new LineupAlgorithm(),
            new GravitationalAlgorithm(),
            new PullBackAlgorithm(),
            new MultiAlgorithm()
        };
    }
}
