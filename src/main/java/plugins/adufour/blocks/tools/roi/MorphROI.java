/*
 * Copyright (c) 2010-2026. Institut Pasteur.
 *
 * This file is part of Icy.
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <https://www.gnu.org/licenses/>.
 */

package plugins.adufour.blocks.tools.roi;

import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.model.roi.ROI;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.model.IntegerRangeModel;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarROIArray;

abstract class MorphROI extends Plugin implements ROIBlock {
    protected enum MorphUnit {
        PIXELS("Radius (px)"), PERCENTAGE("Scale (%)");

        private final String displayText;

        MorphUnit(final String displayText) {
            this.displayText = displayText;
        }

        @Override
        public String toString() {
            return displayText;
        }
    }

    protected final VarROIArray roiIN = new VarROIArray("List of ROI");
    protected final VarInteger x = new VarInteger("Along X", 1);
    protected final VarInteger y = new VarInteger("Along Y", 1);
    protected final VarInteger z = new VarInteger("Along Z", 1);

    protected final VarEnum<MorphUnit> unit = new VarEnum<>("Unit", MorphUnit.PIXELS);

    protected VarROIArray roiOUT;

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
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
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("output ROI", roiOUT);
    }

    /**
     * Converts a scaling factor to a corresponding radius along the X axis (to the nearest pixel)
     *
     * @param roi the ROI to scale
     * @param pct the percentage factor
     * @return a radius that is directly usable with the
     * {@link ErodeROI#erodeROI(ROI, int, int, int)} and
     * {@link DilateROI#dilateROI(ROI, int, int, int)} methods
     * @author Joel Rogers, Alexandre Dufour
     */
    static int percentageToRadiusX(final @NotNull ROI roi, final int pct) {
        final double size = roi.getBounds5D().getSizeX();
        final double diff = (size * pct / 100);
        return (int) Math.round(diff * 0.5);
    }

    /**
     * Converts a scaling factor to a corresponding radius along the Y axis (to the nearest pixel)
     *
     * @param roi the ROI to rescale
     * @param pct the percentage factor
     * @return a radius that is directly usable with the
     * {@link ErodeROI#erodeROI(ROI, int, int, int)} and
     * {@link DilateROI#dilateROI(ROI, int, int, int)} methods
     * @author Joel Rogers, Alexandre Dufour
     */
    static int percentageToRadiusY(final @NotNull ROI roi, final int pct) {
        final double size = roi.getBounds5D().getSizeY();
        final double diff = (size * pct / 100);
        return (int) Math.round(diff * 0.5);
    }

    /**
     * Converts a scaling factor to a corresponding radius along the Z axis (to the nearest pixel)
     *
     * @param roi the ROI to rescale
     * @param pct the percentage factor
     * @return a radius that is directly usable with the
     * {@link ErodeROI#erodeROI(ROI, int, int, int)} and
     * {@link DilateROI#dilateROI(ROI, int, int, int)} methods
     * @author Joel Rogers, Alexandre Dufour
     */
    static int percentageToRadiusZ(final @NotNull ROI roi, final int pct) {
        final double size = roi.getBounds5D().getSizeZ();
        final double diff = (size * pct / 100);
        return (int) Math.round(diff * 0.5);
    }
}
