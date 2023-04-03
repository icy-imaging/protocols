package plugins.adufour.blocks.tools.roi;

import java.util.Arrays;
import java.util.List;

import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import icy.roi.ROIUtil;
import icy.util.ShapeUtil.BooleanOperator;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarROIArray;

public class MergeROI extends Plugin implements ROIBlock {
    VarEnum<BooleanOperator> operation = new VarEnum<>("Merge operation", BooleanOperator.AND);

    VarROIArray roiIn = new VarROIArray("List of ROI");

    VarROIArray roiOut = new VarROIArray("Merged ROI");

    @Override
    public void run() {
        roiOut.setValue(new ROI[0]);

        try {
            List<ROI> rois = Arrays.asList(roiIn.getValue());
            ROI merge = ROIUtil.merge(rois, operation.getValue());
            if (merge != null)
                roiOut.add(merge);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("List of ROI", roiIn);
        inputMap.add("Merge operation", operation);
    }

    @Override
    public void declareOutput(VarList outputMap) {
        outputMap.add("Merged ROI", roiOut);
    }

}
