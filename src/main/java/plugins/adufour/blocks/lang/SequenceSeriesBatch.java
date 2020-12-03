package plugins.adufour.blocks.lang;

import java.io.File;
import java.io.IOException;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.file.SequenceFileImporter;
import icy.sequence.MetaDataUtil;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;
import plugins.kernel.importer.LociImporterPlugin;

/**
 * A Sequence series batch will iteratively load all series contained in a multi-series (e.g. Leica
 * .lif) file, allowing to process all the series with a single work-flow. It can be used in
 * combination with the {@link FileBatch File batch} to batch process entire folders of multi-series
 * files.
 * 
 * @author Alexandre Dufour
 */
public class SequenceSeriesBatch extends Batch
{
    private VarFile multiSeriesFile;

    private int nbSeries;

    private VarSequence element;

    @Override
    public VarFile getBatchSource()
    {
        if (multiSeriesFile == null)
        {
            // WARNING: do *not* change the name of this variable
            // why? see declareInput() and VarList.add()
            multiSeriesFile = new VarFile("Series file", null);
            multiSeriesFile.setDefaultEditorModel(new FileTypeModel(null, FileMode.FILES, null, false));
        }

        return multiSeriesFile;
    }

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
    public void initializeLoop()
    {
        if (multiSeriesFile.getValue() == null)
            throw new VarException(multiSeriesFile, "No file indicated");

        File f = multiSeriesFile.getValue();
        if (f.isDirectory())
            throw new VarException(multiSeriesFile, f.getAbsolutePath() + " is not a file");

        String path = f.getPath();

        try
        {
            final SequenceFileImporter importer = Loader.getSequenceFileImporter(path, true);

            // FIX: force un-grouping to get number of series (Stephane)
            if (importer instanceof LociImporterPlugin)
                ((LociImporterPlugin) importer).setGroupFiles(false);

            nbSeries = MetaDataUtil.getNumSeries(Loader.getOMEXMLMetaData(importer, path));
        }
        catch (UnsupportedFormatException e)
        {
            throw new VarException(multiSeriesFile, path + " is not an imaging file");
        }
        catch (IOException e)
        {
            throw new VarException(multiSeriesFile, "Unable to read " + path);
        }
    }

    @Override
    public void beforeIteration()
    {
        final String path = multiSeriesFile.getValue().getPath();
        final SequenceFileImporter importer = Loader.getSequenceFileImporter(path, true);

        // FIX: force un-grouping as we iterate over series (Stephane)
        if (importer instanceof LociImporterPlugin)
            ((LociImporterPlugin) importer).setGroupFiles(false);

        element.setValue(Loader.loadSequence(importer, path, getIterationCounter().getValue(), true));
    }

    @Override
    public boolean isStopConditionReached()
    {
        return getIterationCounter().getValue() == nbSeries;
    }
}
