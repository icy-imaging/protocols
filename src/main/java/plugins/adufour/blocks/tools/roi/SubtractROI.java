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
import fr.icy.extension.plugin.annotation_.IcyPluginCategory;
import fr.icy.extension.plugin.annotation_.IcyPluginName;
import fr.icy.extension.plugin.annotation_.IcyPluginTags;
import fr.icy.model.roi.ROI;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarROIArray;

import java.util.ArrayList;

@IcyPluginName("Substract ROI")
@IcyPluginCategory("Protocols")
@IcyPluginTags({"Protocols", "ROI", "Substract"})
public class SubtractROI extends Plugin implements ROIBlock {
    private final VarROIArray roiA = new VarROIArray("List of ROI #1");

    private final VarROIArray roiB = new VarROIArray("List of ROI #2");

    private final VarROIArray roiOut = new VarROIArray("Subtracted ROI");

    @Override
    public void run() {
        try {
            roiOut.setValue(subtractROI(roiA.getValue(true), roiB.getValue(true)));
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("List of ROI #1", roiA);
        inputMap.add("List of ROI #2", roiB);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("subtraction output ROI", roiOut);
    }

    /**
     * Subtracts a set of ROI from another
     *
     * @param roiA ROIs to be subtracted from.
     * @param roiB ROIs subtracted from the first group of ROIs.
     * @return A - B. The group of ROIs resulting from the subtraction from each element of the group A and each element of the group B.
     * @throws InterruptedException          if the user interrupts the process
     * @throws UnsupportedOperationException if the operation cannot be done
     */
    public static ROI @NotNull [] subtractROI(final ROI @NotNull [] roiA, final ROI[] roiB) throws UnsupportedOperationException, InterruptedException {
        final ArrayList<ROI> out = new ArrayList<>(roiA.length);

        for (final ROI a : roiA) {
            ROI subtraction = a.getCopy();

            for (final ROI b : roiB)
                subtraction = subtraction.getSubtraction(b);

            if (subtraction == null || subtraction.isEmpty())
                continue;

            if (!subtraction.getBounds5D().isEmpty())
                out.add(subtraction);
        }

        return out.toArray(new ROI[0]);
    }
}
