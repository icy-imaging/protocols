package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarIntegerArrayNative;

/**
 * Utility block reading an array of 32-bit integer values
 * 
 * @author Alexandre Dufour
 */
public class IntegersOutput extends Plugin implements OutputBlock
{
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("integers", new VarIntegerArrayNative("integers", new int[0]));
    }
    
    @Override
    public void run()
    {
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
    }
}
