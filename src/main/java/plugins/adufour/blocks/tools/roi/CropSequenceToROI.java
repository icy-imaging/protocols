package plugins.adufour.blocks.tools.roi;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.rectangle.Rectangle5D;
import icy.util.OMEUtil;
import icy.util.StringUtil;

import java.awt.Rectangle;
import java.util.ArrayList;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarGenericArray;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.lang.VarSequence;

public class CropSequenceToROI extends Plugin implements ROIBlock
{
    VarSequence                 input  = new VarSequence("Sequence to crop", null);
    
    VarROIArray                 rois   = new VarROIArray("List of ROI");
    
    VarGenericArray<Sequence[]> output = new VarGenericArray<Sequence[]>("List of crops", Sequence[].class, new Sequence[] {});
    
    @Override
    public void run()
    {
        Sequence seq = input.getValue(true);
        
        int nbROIs = rois.getValue(true).length;
        
        ArrayList<Sequence> crops = new ArrayList<Sequence>(nbROIs);
        
        int cpt = 1;
        int digitSize = 1 + (int) Math.log10(nbROIs);
        
        for (ROI roi : rois.getValue())
        {
            final Sequence result = new Sequence(OMEUtil.createOMEXMLMetadata(seq.getOMEXMLMetadata()));
            
            Rectangle5D.Integer region = roi.getBounds5D().toInteger();
            
            final Rectangle region2d = region.toRectangle2D().getBounds();
            final int startZ;
            final int endZ;
            final int startT;
            final int endT;
            
            if (region.isInfiniteZ())
            {
                startZ = 0;
                endZ = seq.getSizeZ();
            }
            else
            {
                startZ = Math.max(0, region.z);
                endZ = Math.min(seq.getSizeZ(), region.z + region.sizeZ);
            }
            if (region.isInfiniteT())
            {
                startT = 0;
                endT = seq.getSizeT();
            }
            else
            {
                startT = Math.max(0, region.t);
                endT = (int) Math.min(seq.getSizeT(), (long) region.t + (long) region.sizeT);
            }
            
            result.beginUpdate();
            try
            {
                for (int t = startT; t < endT; t++)
                {
                    for (int z = startZ; z < endZ; z++)
                    {
                        IcyBufferedImage img = seq.getImage(t, z);
                        
                        if (img != null) img = IcyBufferedImageUtil.getSubImage(img, region2d, region.c, region.sizeC);
                        
                        result.setImage(t - startT, z - startZ, img);
                    }
                }
            }
            finally
            {
                result.endUpdate();
            }
            
            result.setName(seq.getName() + "_crop" + StringUtil.toString(cpt, digitSize));
            crops.add(result);
            cpt++;
        }
        
        output.setValue(crops.toArray(new Sequence[crops.size()]));
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("sequence to crop", input);
        inputMap.add("list of ROI", rois);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("list of crops", output);
    }
    
}
