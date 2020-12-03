package plugins.adufour.blocks.tools.roi;

import icy.plugin.abstract_.Plugin;
import icy.roi.ROI;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.model.IntegerRangeModel;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarROIArray;

abstract class MorphROI extends Plugin implements ROIBlock
{
    protected enum MorphUnit
    {
        PIXELS("Radius (px)"), PERCENTAGE("Scale (%)");
        
        private final String displayText;
        
        private MorphUnit(String displayText)
        {
            this.displayText = displayText;
        }
        
        @Override
        public String toString()
        {
            return displayText;
        }
    }
    
    VarROIArray roiIN = new VarROIArray("List of ROI");
    VarInteger  x     = new VarInteger("Along X", 1);
    VarInteger  y     = new VarInteger("Along Y", 1);
    VarInteger  z     = new VarInteger("Along Z", 1);
    
    VarEnum<MorphUnit> unit = new VarEnum<MorphUnit>("Unit", MorphUnit.PIXELS);
    
    VarROIArray roiOUT;
    
    @Override
    public void declareInput(VarList inputMap)
    {
        x.setDefaultEditorModel(new IntegerRangeModel(1, 0, 100, 1));
        y.setDefaultEditorModel(new IntegerRangeModel(1, 0, 100, 1));
        z.setDefaultEditorModel(new IntegerRangeModel(1, 0, 100, 1));
        inputMap.add("input ROI", roiIN);
        inputMap.add("X radius", x);
        inputMap.add("Y radius", y);
        inputMap.add("Z radius", z);
        inputMap.add("unit", unit);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("output ROI", roiOUT);
    }
    
    /**
     * Converts a scaling factor to a corresponding radius along the X axis (to the nearest pixel)
     * 
     * @author Joel Rogers, Alexandre Dufour
     * @param roi
     *            the ROI to scale
     * @param pct
     *            the percentage factor
     * @return a radius that is directly usable with the
     *         {@link ErodeROI#erodeROI(ROI, int, int, int)} and
     *         {@link DilateROI#dilateROI(ROI, int, int, int)} methods
     */
    static int percentageToRadiusX(ROI roi, int pct)
    {
        double size = roi.getBounds5D().getSizeX();
        double diff = (size * pct / 100);
        return (int) Math.round(diff * 0.5);
    }
    
    /**
     * Converts a scaling factor to a corresponding radius along the Y axis (to the nearest pixel)
     * 
     * @author Joel Rogers, Alexandre Dufour
     * @param roi
     *            the ROI to rescale
     * @param pct
     *            the percentage factor
     * @return a radius that is directly usable with the
     *         {@link ErodeROI#erodeROI(ROI, int, int, int)} and
     *         {@link DilateROI#dilateROI(ROI, int, int, int)} methods
     */
    static int percentageToRadiusY(ROI roi, int pct)
    {
        double size = roi.getBounds5D().getSizeY();
        double diff = (size * pct / 100);
        return (int) Math.round(diff * 0.5);
    }
    
    /**
     * Converts a scaling factor to a corresponding radius along the Z axis (to the nearest pixel)
     * 
     * @author Joel Rogers, Alexandre Dufour
     * @param roi
     *            the ROI to rescale
     * @param pct
     *            the percentage factor
     * @return a radius that is directly usable with the
     *         {@link ErodeROI#erodeROI(ROI, int, int, int)} and
     *         {@link DilateROI#dilateROI(ROI, int, int, int)} methods
     */
    static int percentageToRadiusZ(ROI roi, int pct)
    {
        double size = roi.getBounds5D().getSizeZ();
        double diff = (size * pct / 100);
        return (int) Math.round(diff * 0.5);
    }
}
