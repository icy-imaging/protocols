/*
 * Copyright (c) 2010-2023. Institut Pasteur.
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

package plugins.adufour.blocks.tools.ij;

import icy.imagej.ImageJUtil;
import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarImagePlus;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

public class SequenceToImagePlus extends Plugin implements IJBlock {
    private final VarSequence vs = new VarSequence("Icy Sequence", null);
    private final VarImagePlus vip = new VarImagePlus("ImagePlus", null);

    @Override
    public void run() {
        try {
            vip.setValue(ImageJUtil.convertToImageJImage(vs.getValue(true), null));
        }
        catch (final IllegalAccessError e) {
            throw new VarException(vs, e.getMessage());
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("Icy Sequence", vs);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        outputMap.add("IJ ImagePlus", vip);
    }

}
