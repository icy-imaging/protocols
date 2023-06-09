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

import icy.plugin.abstract_.Plugin;
import icy.system.IcyHandledException;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarImagePlus;
import plugins.adufour.vars.lang.VarString;

public class CallIJPlugin extends Plugin implements IJBlock {
    private final VarImagePlus varIp = new VarImagePlus("Input ImagePlus", null);

    private final VarString pluginName = new VarString("plug-in name", "");

    private final VarString pluginParams = new VarString("parameters", "");

    private final VarImagePlus varActiveIP = new VarImagePlus("Output (active) ImagePlus", null);

    @Override
    public void run() {
        try {
            final ImagePlus imgPlus = varIp.getValue(false);
            final String params = pluginParams.getValue(false);

            if (imgPlus != null)
                IJ.run(imgPlus, pluginName.getValue(true), params);
            else
                IJ.run(pluginName.getValue(true), params);

            ImagePlus output = null;

            try {
                // Set the output image (if available) with the following priority
                output = WindowManager.getCurrentImage();
            }
            catch (final RuntimeException e) {
                // just in case
            }

            // Default to the current "temporary" image (if any)
            if (output == null)
                output = WindowManager.getTempCurrentImage();
            // Default to the input image (may have been modified "in-place"
            if (output == null)
                output = varIp.getValue();

            varActiveIP.setValue(output);
        }
        catch (final RuntimeException e) {
            throw new IcyHandledException(e.getLocalizedMessage());
        }
    }

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("Input ImagePlus", varIp);
        inputMap.add("ImageJ plug-in name", pluginName);
        inputMap.add("ImageJ plug-in parameters", pluginParams);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        outputMap.add("Output ImagePlus", varActiveIP);
    }
}
