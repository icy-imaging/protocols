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

import fr.icy.common.geom.shape.ShapeUtil;
import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.model.roi.ROI;
import fr.icy.model.roi.ROIUtil;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarROIArray;

import java.util.Arrays;
import java.util.List;

public class MergeROI extends Plugin implements ROIBlock {
    private final VarEnum<ShapeUtil.BooleanOperator> operation = new VarEnum<>("Merge operation", ShapeUtil.BooleanOperator.AND);

    private final VarROIArray roiIn = new VarROIArray("List of ROI");

    private final VarROIArray roiOut = new VarROIArray("Merged ROI");

    @Override
    public void run() {
        roiOut.setValue(new ROI[0]);

        try {
            final List<ROI> rois = Arrays.asList(roiIn.getValue());
            final ROI merge = ROIUtil.merge(rois, operation.getValue());
            if (merge != null)
                roiOut.add(merge);
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("List of ROI", roiIn);
        inputMap.add("Merge operation", operation);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("Merged ROI", roiOut);
    }
}
