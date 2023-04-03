package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarDoubleArrayNative;

/**
 * Utility block reading an array of double-precision floating-point values
 *
 * @author Alexandre Dufour
 */
public class DecimalsOutput extends Plugin implements OutputBlock {
    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("decimals", new VarDoubleArrayNative("decimals", new double[0]));
    }

    @Override
    public void run() {
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }
}
