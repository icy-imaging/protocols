package plugins.adufour.blocks.tools.io;

import icy.file.Loader;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFileArray;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

/**
 * Utility block that reads multiple image files from disk into a {@link Sequence}
 * 
 * @author Alexandre Dufour
 */
public class FilesToSequence extends Plugin implements IOBlock
{
    VarFileArray inputFiles     = new VarFileArray("input file", new File[] {});
    VarSequence  outputSequence = new VarSequence("sequence", null);
    
    @Override
    public void run()
    {
        try
        {
            File[] files = inputFiles.getValue(true);
            List<String> paths = new ArrayList<String>(files.length);

            for (int i = 0; i < files.length; i++)
                if (files[i] != null) paths.add(files[i].getPath());
            
            outputSequence.setValue(Loader.loadSequence(paths, false));
        }
        catch (Exception e)
        {
            throw new VarException(inputFiles, "unable to read files");
        }
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("input file", inputFiles);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("sequence", outputSequence);
    }
    
}
