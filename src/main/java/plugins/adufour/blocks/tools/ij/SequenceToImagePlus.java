package plugins.adufour.blocks.tools.ij;

import icy.imagej.ImageJUtil;
import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarImagePlus;
import plugins.adufour.vars.lang.VarSequence;

public class SequenceToImagePlus extends Plugin implements IJBlock
{
    VarSequence  vs  = new VarSequence("Icy Sequence", null);
    VarImagePlus vip = new VarImagePlus("ImagePlus", null);
    
    @Override
    public void run()
    {
        vip.setValue(ImageJUtil.convertToImageJImage(vs.getValue(true), null));
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("Icy Sequence", vs);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("IJ ImagePlus", vip);
    }
    
}
