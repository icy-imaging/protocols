package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarSequenceArray;

/**
 * Utility block reading a list of {@link icy.sequence.Sequence} objects
 *
 * @author Alexandre Dufour
 */
public class SequencesOutput extends Plugin implements OutputBlock {
    @Override
    public void run() {
    }

    @Override
    public void declareInput(VarList inputMap) {
        VarSequenceArray vsa = new VarSequenceArray("sequence");
        inputMap.add("input sequence", vsa);
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }
}
