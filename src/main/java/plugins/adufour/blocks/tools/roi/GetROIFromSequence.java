package plugins.adufour.blocks.tools.roi;

import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import icy.sequence.Sequence;

import java.util.ArrayList;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.lang.VarSequence;

public class GetROIFromSequence extends Plugin implements ROIBlock {
    public enum ROIFilter {
        SELECTED, NON_SELECTED, ALL
    }

    VarEnum<ROIFilter> filter = new VarEnum<ROIFilter>("ROI to get", ROIFilter.ALL);

    VarROIArray rois = new VarROIArray("List of ROI");

    VarSequence sequence = new VarSequence("Source", null);

    @Override
    public void run() {
        Sequence s = sequence.getValue();

        if (s == null) {
            rois.setValue(new ROI[]{});
            return;
        }

        ArrayList<ROI> inputROI = s.getROIs();

        switch (filter.getValue()) {
            case SELECTED:
                for (int i = 0; i < inputROI.size(); i++)
                    if (!inputROI.get(i).isSelected()) inputROI.remove(i--);
                break;
            case NON_SELECTED:
                for (int i = 0; i < inputROI.size(); i++)
                    if (inputROI.get(i).isSelected()) inputROI.remove(i--);
                break;
            case ALL:
                break;

            default:
                throw new UnsupportedOperationException("Unknown ROI selection option: " + filter.getValueAsString());
        }

        rois.setValue(inputROI.toArray(new ROI[inputROI.size()]));
    }

    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("input sequence", sequence);
        inputMap.add("selection state of ROI to extract", filter);
    }

    @Override
    public void declareOutput(VarList outputMap) {
        outputMap.add("extracted rois", rois);
    }
}
