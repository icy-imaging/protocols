package plugins.adufour.blocks.tools.input;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarDouble;

/**
 * Input block reading a 64-bit double-precision floating-point value
 * 
 * @author Alexandre Dufour
 */
public class Decimal extends Plugin implements InputBlock
{
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("decimal", new VarDouble("decimal", 0.0));
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
