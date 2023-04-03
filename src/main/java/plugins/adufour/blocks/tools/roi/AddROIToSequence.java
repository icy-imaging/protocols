package plugins.adufour.blocks.tools.roi;

import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import icy.sequence.Sequence;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.lang.VarSequence;

/**
 * (Deprecated: Use plugins.tprovoost.sequenceblocks.add.AddRois instead)
 * Block to add one or several {@link ROI} to a Sequence.
 * <p>
 * class: plugins.adufour.blacks.tools.roi.AddROIToSequence
 *
 * @author Alexandre Dufour
 */
@Deprecated
public class AddROIToSequence extends Plugin implements ROIBlock {
    VarROIArray rois = new VarROIArray("ROI to add");

    VarSequence sequence = new VarSequence("Source", null);

    VarBoolean replace = new VarBoolean("Overwrite", false);

    @Override
    public void run() {
        Sequence s = sequence.getValue(true);

        if (replace.getValue()) s.removeAllROI();

        s.beginUpdate();
        for (ROI roi : rois.getValue(true))
            s.addROI(roi);
        s.endUpdate();

        s.saveXMLData();
    }

    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("target sequence", sequence);
        inputMap.add("input rois", rois);
        inputMap.add("replace existing", replace);
    }

    @Override
    public void declareOutput(VarList outputMap) {

    }
}
