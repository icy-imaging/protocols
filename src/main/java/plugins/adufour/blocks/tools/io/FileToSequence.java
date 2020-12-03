package plugins.adufour.blocks.tools.io;

import icy.file.Loader;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;

import java.io.File;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

/**
 * Utility block that reads a file from disk
 * 
 * @author Alexandre Dufour
 */
public class FileToSequence extends Plugin implements IOBlock
{
    VarFile     inputFile      = new VarFile("input file", null);
    VarSequence outputSequence = new VarSequence("sequence", null);
    VarInteger  series         = new VarInteger("Series", 0);
    
    @Override
    public void run()
    {
        try
        {
            File file = inputFile.getValue(true);
            Sequence sequence = Loader.loadSequence(file.getPath(), series.getValue(), false);
            if (sequence == null) throw new VarException(inputFile, "Cannot read " + file.getPath() + " into a sequence");
            outputSequence.setValue(sequence);
        }
        catch (Exception e)
        {
            File file = inputFile.getValue(true);
            throw new VarException(inputFile, "unable to read file " + file.getAbsolutePath());
        }
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("input file", inputFile);
        inputMap.add("Series", series);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("sequence", outputSequence);
    }
    
}
