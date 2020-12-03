package plugins.adufour.blocks.tools.ij;

import icy.plugin.abstract_.Plugin;
import ij.IJ;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFile;

public class CallIJMacro extends Plugin implements IJBlock
{
    VarFile macroFile = new VarFile("Macro file", null);
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("Macro file", macroFile);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        
    }
    
    @Override
    public void run()
    {
        IJ.runMacroFile(macroFile.getValue(true).getPath());
    }
    
}
