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

package plugins.adufour.blocks.tools.input;

import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarString;

/**
 * Text input
 *
 * @author Alexandre Dufour
 */
public class Text extends Plugin implements InputBlock {
    @Override
    public void run() {

    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("text", new VarString("text", ""));
    }

    @Override
    public void declareOutput(final VarList outputMap) {

    }
}
