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

package plugins.adufour.blocks.tools.input;

import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.extension.plugin.annotation_.IcyPluginCategory;
import fr.icy.extension.plugin.annotation_.IcyPluginName;
import fr.icy.extension.plugin.annotation_.IcyPluginTags;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarDoubleArrayNative;

/**
 * Utility block reading an array of double-precision floating-point values
 *
 * @author Alexandre Dufour
 */
@IcyPluginName("Decimals")
@IcyPluginCategory("Protocols")
@IcyPluginTags({"Protocols", "Input", "Decimals"})
public class Decimals extends Plugin implements InputBlock {
    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("decimals", new VarDoubleArrayNative("decimals", new double[0]));
    }

    @Override
    public void run() {
    }

    @Override
    public void declareOutput(final VarList outputMap) {
    }
}
