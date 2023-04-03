package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarSequence;

/**
 * Utility block reading a {@link icy.sequence.Sequence} object
 *
 * @author Alexandre Dufour
 */
public class SequenceOutput extends Plugin implements OutputBlock {
    private final VarSequence vs = new VarSequence("sequence", null);

    @Override
    public void run() {
    }

    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("input sequence", vs);
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }

    public VarSequence getVariable() {
        return vs;
    }
}
