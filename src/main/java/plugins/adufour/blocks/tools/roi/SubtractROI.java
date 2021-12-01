package plugins.adufour.blocks.tools.roi;

import java.util.ArrayList;

import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarROIArray;

public class SubtractROI extends Plugin implements ROIBlock
{
    VarROIArray roiA = new VarROIArray("List of ROI #1");

    VarROIArray roiB = new VarROIArray("List of ROI #2");

    VarROIArray roiOut = new VarROIArray("Subtracted ROI");

    @Override
    public void run()
    {
        try
        {
            roiOut.setValue(subtractROI(roiA.getValue(true), roiB.getValue(true)));
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("List of ROI #1", roiA);
        inputMap.add("List of ROI #2", roiB);
    }

    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("subtraction output ROI", roiOut);
    }

    /**
     * Subtracts a set of ROI from another
     * 
     * @param roiA
     *        ROIs to be subtracted from.
     * @param roiB
     *        ROIs subtracted from the first group of ROIs.
     * @return A - B. The group of ROIs resulting from the subtraction from the each element of the group A and each element of the group B.
     * @throws InterruptedException
     * @throws UnsupportedOperationException
     */
    public static ROI[] subtractROI(ROI[] roiA, ROI[] roiB) throws UnsupportedOperationException, InterruptedException
    {
        ArrayList<ROI> out = new ArrayList<ROI>(roiA.length);

        for (ROI a : roiA)
        {
            ROI subtraction = a.getCopy();

            for (ROI b : roiB)
                subtraction = subtraction.getSubtraction(b);

            if (subtraction == null || subtraction.isEmpty())
                continue;

            if (!subtraction.getBounds5D().isEmpty())
                out.add(subtraction);
        }

        return out.toArray(new ROI[out.size()]);
    }
}
