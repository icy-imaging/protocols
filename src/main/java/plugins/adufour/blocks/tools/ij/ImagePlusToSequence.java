package plugins.adufour.blocks.tools.ij;

import icy.imagej.ImageJUtil;
import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarImagePlus;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

public class ImagePlusToSequence extends Plugin implements IJBlock
{
    VarImagePlus vip = new VarImagePlus("IJ ImagePlus", null);
    VarSequence vs = new VarSequence("Icy Sequence", null);

    @Override
    public void run()
    {
        try
        {
            vs.setValue(ImageJUtil.convertToIcySequence(vip.getValue(true), null));
        }
        catch (IllegalAccessError e)
        {
            throw new VarException(vip, e.getMessage());
        }
        catch (VarException e)
        {
            throw e;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("IJ ImagePlus", vip);
    }

    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("Icy Sequence", vs);
    }

}
