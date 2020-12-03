package plugins.adufour.blocks.tools.roi;

import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import icy.type.point.Point5D;

import java.awt.geom.Point2D;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi3d.ROI3DArea;

public class TranslateROI extends Plugin implements ROIBlock
{
    VarROIArray inputROI  = new VarROIArray("Input ROI");
    
    VarInteger  xShift    = new VarInteger("X shift", 0);
    VarInteger  yShift    = new VarInteger("Y shift", 0);
    VarInteger  zShift    = new VarInteger("Z shift", 0);
    
    VarROIArray outputROI = new VarROIArray("Translated ROI");
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("input ROI", inputROI);
        inputMap.add("X shift", xShift);
        inputMap.add("Y shift", yShift);
        inputMap.add("Z shift", zShift);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("output ROI", outputROI);
    }
    
    @Override
    public void run()
    {
        outputROI.setValue(new ROI[0]);
        
        for (ROI roi : inputROI)
        {
            if (Thread.currentThread().isInterrupted()) break;
            
            if (roi.canSetPosition())
            {
                ROI shiftedROI = roi.getCopy();
                Point5D pos = shiftedROI.getPosition5D();
                pos.setX(pos.getX() + xShift.getValue());
                pos.setY(pos.getY() + yShift.getValue());
                pos.setZ(pos.getZ() + zShift.getValue());
                shiftedROI.setPosition5D(pos);
                outputROI.add(shiftedROI);
            }
            else
            {
                if (roi instanceof ROI3DArea)
                {
                    ROI3DArea shiftedROI = new ROI3DArea();
                    
                    for (ROI2DArea slice : (ROI3DArea) roi)
                    {
                        ROI2DArea sliceCopy = (ROI2DArea) slice.getCopy();
                        
                        Point2D p2 = slice.getPosition2D();
                        p2.setLocation(p2.getX() + xShift.getValue(), p2.getY() + yShift.getValue());
                        sliceCopy.setPosition2D(p2);
                        
                        shiftedROI.setSlice(slice.getZ() + zShift.getValue(), sliceCopy);
                    }
                    
                    outputROI.add(shiftedROI);
                }
            }
            
        }
    }
    
}
