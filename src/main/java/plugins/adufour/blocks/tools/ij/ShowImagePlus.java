package plugins.adufour.blocks.tools.ij;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarImagePlus;

public class ShowImagePlus extends Plugin implements IJBlock {
    VarImagePlus ip = new VarImagePlus("ImagePlus", null);

    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("ImagePlus", ip);
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }

    @Override
    public void run() {
        ip.getValue(true).show();
    }

}
