package plugins.adufour.blocks.tools;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarMutableArray;

public class ListSize extends Plugin implements ToolsBlock
{
    private final VarMutableArray list = new VarMutableArray("List", null);
    
    private final VarInteger      size = new VarInteger("Size", 0);
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("list", list);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("size", size);
    }
    
    @Override
    public void run()
    {
        size.setValue(list.size());
    }
}
