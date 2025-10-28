/*
 * Copyright (c) 2010-2025. Institut Pasteur.
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

import org.bioimageanalysis.extension.kernel.roi.roi2d.ROI2DArea;
import org.bioimageanalysis.extension.kernel.roi.roi3d.ROI3DArea;
import org.bioimageanalysis.icy.common.geom.point.Point5D;
import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarROIArray;

import java.awt.geom.Point2D;

public class TranslateROI extends Plugin implements ROIBlock {
    private final VarROIArray inputROI = new VarROIArray("Input ROI");

    private final VarInteger xShift = new VarInteger("X shift", 0);
    private final VarInteger yShift = new VarInteger("Y shift", 0);
    private final VarInteger zShift = new VarInteger("Z shift", 0);

    private final VarROIArray outputROI = new VarROIArray("Translated ROI");

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("input ROI", inputROI);
        inputMap.add("X shift", xShift);
        inputMap.add("Y shift", yShift);
        inputMap.add("Z shift", zShift);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("output ROI", outputROI);
    }

    @Override
    public void run() {
        outputROI.setValue(new ROI[0]);

        for (final ROI roi : inputROI) {
            if (Thread.currentThread().isInterrupted()) break;

            if (roi.canSetPosition()) {
                final ROI shiftedROI = roi.getCopy();
                final Point5D pos = shiftedROI.getPosition5D();
                pos.setX(pos.getX() + xShift.getValue());
                pos.setY(pos.getY() + yShift.getValue());
                pos.setZ(pos.getZ() + zShift.getValue());
                shiftedROI.setPosition5D(pos);
                outputROI.add(shiftedROI);
            }
            else {
                if (roi instanceof ROI3DArea) {
                    final ROI3DArea shiftedROI = new ROI3DArea();

                    for (final ROI2DArea slice : (ROI3DArea) roi) {
                        final ROI2DArea sliceCopy = (ROI2DArea) slice.getCopy();

                        final Point2D p2 = slice.getPosition2D();
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
