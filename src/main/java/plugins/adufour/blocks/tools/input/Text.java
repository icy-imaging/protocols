package plugins.adufour.blocks.tools.input;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarString;

/**
 * Text input
 *
 * @author Alexandre Dufour
 */
public class Text extends Plugin implements InputBlock {
    @Override
    public void run() {

    }

    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("text", new VarString("text", ""));
    }

    @Override
    public void declareOutput(VarList outputMap) {

    }
}
