package plugins.adufour.blocks.lang;

import java.io.File;

import icy.file.Loader;
import plugins.adufour.vars.lang.VarSequence;

/**
 * Similar to the {@link FileBatch File batch}, a Sequence file batch will read
 * all files of a user-defined folder, and additionally load and give access to
 * the sequence contained within these files (non-imaging files will be
 * skipped).<br>
 * Note for multi-series imaging files (e.g. Leica .lif): when loading
 * multi-series files, a dialog box will appear to let the user select the
 * series to process. However only the first selected series will be loaded. To
 * batch process entire multi-series files, use the {@link SequenceSeriesBatch
 * Sequence series batch} instead.
 * 
 * @author Alexandre Dufour
 */
public class SequenceFileBatch extends FileBatch
{
    VarSequence element;

    @Override
    public VarSequence getBatchElement()
    {
        if (element == null)
        {
            // WARNING: do *not* change the name of this variable
            // why? see declareInput() and VarList.add()
            element = new VarSequence("Sequence", null);
        }
        return element;
    }

    @Override
    public boolean accept(File f)
    {
        return f.isDirectory() || super.accept(f) && Loader.isSupportedImageFile(f.getPath());
    }

    @Override
    public void beforeIteration()
    {
        try
        {
            element.setValue(Loader.loadSequence(files[getIterationCounter().getValue()].getPath(), 0, false));
            Thread.yield();
        }
        catch (Exception e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
