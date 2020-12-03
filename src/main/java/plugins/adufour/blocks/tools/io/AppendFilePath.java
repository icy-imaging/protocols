package plugins.adufour.blocks.tools.io;

import java.io.File;

import icy.file.FileUtil;
import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarString;

public class AppendFilePath extends Plugin implements IOBlock
{
    VarMutable    in        = new VarMutable("Current file", null);
    
    VarFile    out       = new VarFile("New file", null);
    
    VarBoolean removeExt = new VarBoolean("Remove extension", false);
    
    VarString  suffix    = new VarString("Add suffix", "");
    
    @Override
    public void run()
    {
        Object input = in.getValue(true);
        String newPath = "";
        
        if (input instanceof File)
            newPath = ((File)input).getAbsolutePath();
        else newPath = input.toString();
        
        if (removeExt.getValue())
        {
            String fileName = FileUtil.getFileName(newPath, false);
            newPath = FileUtil.getDirectory(newPath) + fileName;
        }
        
        newPath += suffix.getValue();
        
        out.setValue(new File(newPath));
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("input file", in);
        inputMap.add("remove ext.", removeExt);
        inputMap.add("suffix", suffix);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("output file", out);
    }
    
}
