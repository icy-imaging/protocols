package plugins.adufour.blocks.tools.ij;

import icy.plugin.abstract_.Plugin;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarImagePlus;

public class GetActiveImagePlus extends Plugin implements IJBlock
{
    VarImagePlus ip = new VarImagePlus("ImagePlus", null);

    @Override
    public void declareInput(VarList inputMap)
    {
        //
    }

    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("ImagePlus", ip);
    }

    @Override
    public void run()
    {
        // Set the output image (if available) with the following priority
        ImagePlus imgPlus = WindowManager.getCurrentImage();
        // Default to the current "temporary" image (if any)
        if (imgPlus == null)
            imgPlus = WindowManager.getTempCurrentImage();

        ip.setValue(imgPlus);
    }
}
