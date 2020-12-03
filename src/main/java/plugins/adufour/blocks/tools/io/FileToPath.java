package plugins.adufour.blocks.tools.io;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarString;

public class FileToPath extends Plugin implements IOBlock
{
    VarString path = new VarString("path", "", 1);
    
    VarFile   file = new VarFile("file", null);
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("input file", file);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("output path", path);
    }
    
    @Override
    public void run()
    {
        path.setValue(file.getValue() == null ? null : file.getValue().getPath());
    }
}
