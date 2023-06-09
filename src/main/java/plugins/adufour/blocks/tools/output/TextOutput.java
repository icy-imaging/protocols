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

package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarString;

/**
 * Text input
 *
 * @author Alexandre Dufour
 */
public class TextOutput extends Plugin implements OutputBlock {
    @Override
    public void run() {
    }

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("text", new VarString("text", ""));
    }

    @Override
    public void declareOutput(final VarList outputMap) {
    }
}
