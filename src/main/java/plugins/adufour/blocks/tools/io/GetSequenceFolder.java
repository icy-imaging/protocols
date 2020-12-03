package plugins.adufour.blocks.tools.io;

import java.io.File;

import icy.file.FileUtil;
import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.tools.sequence.SequenceBlock;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

public class GetSequenceFolder extends Plugin implements SequenceBlock, IOBlock
{
    VarSequence sequence = new VarSequence("Sequence", null);
    
    VarFile folder = new VarFile("Folder", null);
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("Sequence", sequence);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("Folder", folder);
    }
    
    @Override
    public void run()
    {
        String filePath = sequence.getValue(true).getFilename();
        if (filePath == null) throw new VarException(sequence, "[Get sequence folder]: the selected sequence has not been saved on disk");
        
        String folderPath = FileUtil.getDirectory(filePath);
        folder.setValue(new File(folderPath));
    }
}
