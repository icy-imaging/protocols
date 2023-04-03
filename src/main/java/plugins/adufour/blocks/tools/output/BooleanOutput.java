package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarBoolean;

public class BooleanOutput extends Plugin implements OutputBlock {
    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("boolean", new VarBoolean("Boolean", false));
    }

    @Override
    public void declareOutput(VarList outputMap) {
        //
    }

    @Override
    public void run() {
        //
    }
}
