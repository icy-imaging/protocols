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
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarImagePlus;

public class ShowImagePlus extends Plugin implements IJBlock {
    private final VarImagePlus ip = new VarImagePlus("ImagePlus", null);

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("ImagePlus", ip);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
    }

    @Override
    public void run() {
        ip.getValue(true).show();
    }

}
