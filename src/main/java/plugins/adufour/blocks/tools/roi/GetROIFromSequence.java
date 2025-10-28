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

import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarEnum;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.lang.VarSequence;

import java.util.ArrayList;

public class GetROIFromSequence extends Plugin implements ROIBlock {
    public enum ROIFilter {
        SELECTED, NON_SELECTED, ALL
    }

    private final VarEnum<ROIFilter> filter = new VarEnum<>("ROI to get", ROIFilter.ALL);

    private final VarROIArray rois = new VarROIArray("List of ROI");

    private final VarSequence sequence = new VarSequence("Source", null);

    @Override
    public void run() {
        final Sequence s = sequence.getValue();

        if (s == null) {
            rois.setValue(new ROI[]{});
            return;
        }

        final ArrayList<ROI> inputROI = s.getROIs();

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

        rois.setValue(inputROI.toArray(new ROI[0]));
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("input sequence", sequence);
        inputMap.add("selection state of ROI to extract", filter);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("extracted rois", rois);
    }
}
