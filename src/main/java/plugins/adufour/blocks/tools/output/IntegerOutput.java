package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarInteger;

/**
 * Utility block reading a 32-bit integer value
 * 
 * @author Alexandre Dufour
 */
public class IntegerOutput extends Plugin implements OutputBlock
{
    @Override
    public void declareInput(VarList inputMap)
    {
        VarInteger vi = new VarInteger("integer", 0);
        inputMap.add("integer", vi);
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
