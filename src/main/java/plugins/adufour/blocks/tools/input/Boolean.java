package plugins.adufour.blocks.tools.input;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarBoolean;

public class Boolean extends Plugin implements InputBlock
{
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("boolean", new VarBoolean("Boolean", false));
    }

    @Override
    public void declareOutput(VarList outputMap)
    {
    }

    @Override
    public void run()
    {
    }
}
