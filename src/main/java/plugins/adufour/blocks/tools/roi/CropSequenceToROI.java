/*
 * Copyright (c) 2010-2024. Institut Pasteur.
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

import org.bioimageanalysis.icy.common.geom.rectangle.Rectangle5D;
import org.bioimageanalysis.icy.common.string.StringUtil;
import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import org.bioimageanalysis.icy.model.OMEUtil;
import org.bioimageanalysis.icy.model.image.IcyBufferedImage;
import org.bioimageanalysis.icy.model.image.IcyBufferedImageUtil;
import org.bioimageanalysis.icy.model.roi.ROI;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarGenericArray;
import plugins.adufour.vars.lang.VarROIArray;
import plugins.adufour.vars.lang.VarSequence;

import java.awt.*;
import java.util.ArrayList;

public class CropSequenceToROI extends Plugin implements ROIBlock {
    private final VarSequence input = new VarSequence("Sequence to crop", null);

    private final VarROIArray rois = new VarROIArray("List of ROI");

    private final VarGenericArray<Sequence[]> output = new VarGenericArray<>("List of crops", Sequence[].class, new Sequence[]{});

    @Override
    public void run() {
        final Sequence seq = input.getValue(true);

        final int nbROIs = rois.getValue(true).length;

        final ArrayList<Sequence> crops = new ArrayList<>(nbROIs);

        int cpt = 1;
        final int digitSize = 1 + (int) Math.log10(nbROIs);

        for (final ROI roi : rois.getValue()) {
            final Sequence result = new Sequence(OMEUtil.createOMEXMLMetadata(seq.getOMEXMLMetadata()));

            final Rectangle5D.Integer region = roi.getBounds5D().toInteger();

            final Rectangle region2d = region.toRectangle2D().getBounds();
            final int startZ;
            final int endZ;
            final int startT;
            final int endT;

            if (region.isInfiniteZ()) {
                startZ = 0;
                endZ = seq.getSizeZ();
            }
            else {
                startZ = Math.max(0, region.z);
                endZ = Math.min(seq.getSizeZ(), region.z + region.sizeZ);
            }
            if (region.isInfiniteT()) {
                startT = 0;
                endT = seq.getSizeT();
            }
            else {
                startT = Math.max(0, region.t);
                endT = (int) Math.min(seq.getSizeT(), (long) region.t + (long) region.sizeT);
            }

            result.beginUpdate();
            try {
                for (int t = startT; t < endT; t++) {
                    for (int z = startZ; z < endZ; z++) {
                        IcyBufferedImage img = seq.getImage(t, z);

                        if (img != null) img = IcyBufferedImageUtil.getSubImage(img, region2d, region.c, region.sizeC);

                        result.setImage(t - startT, z - startZ, img);
                    }
                }
            }
            finally {
                result.endUpdate();
            }

            result.setName(seq.getName() + "_crop" + StringUtil.toString(cpt, digitSize));
            crops.add(result);
            cpt++;
        }

        output.setValue(crops.toArray(new Sequence[0]));
    }

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("sequence to crop", input);
        inputMap.add("list of ROI", rois);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        outputMap.add("list of crops", output);
    }
}
