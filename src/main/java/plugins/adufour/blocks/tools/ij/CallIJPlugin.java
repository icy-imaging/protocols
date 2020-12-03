package plugins.adufour.blocks.tools.ij;

import icy.plugin.abstract_.Plugin;
import icy.system.IcyHandledException;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarImagePlus;
import plugins.adufour.vars.lang.VarString;

public class CallIJPlugin extends Plugin implements IJBlock
{
    VarImagePlus varIp = new VarImagePlus("Input ImagePlus", null);
    
    VarString pluginName = new VarString("plug-in name", "");
    
    VarString pluginParams = new VarString("parameters", "");
    
    VarImagePlus varActiveIP = new VarImagePlus("Output (active) ImagePlus", null);
    
    @Override
    public void run()
    {
        try
        {
            IJ.run(varIp.getValue(true), pluginName.getValue(true), pluginParams.getValue(true));
            // Set the output image (if available) with the following priority
            ImagePlus output = WindowManager.getCurrentImage();
            if (output == null)
            {
                // Default to the current "temporary" image (if any)
                output = WindowManager.getTempCurrentImage();
            }
            if (output == null)
            {
                // Default to the input image (may have been modified "in-place"
                output = varIp.getValue();
            }
            varActiveIP.setValue(output);
        }
        catch (RuntimeException e)
        {
            throw new IcyHandledException(e.getLocalizedMessage());
        }
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("Input ImagePlus", varIp);
        inputMap.add("ImageJ plug-in name", pluginName);
        inputMap.add("ImageJ plug-in parameters", pluginParams);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("Output ImagePlus", varActiveIP);
    }
}
