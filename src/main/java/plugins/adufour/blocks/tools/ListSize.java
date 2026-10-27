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

package plugins.adufour.blocks.tools;

import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.extension.plugin.annotation_.IcyPluginCategory;
import fr.icy.extension.plugin.annotation_.IcyPluginName;
import fr.icy.extension.plugin.annotation_.IcyPluginTags;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarMutableArray;

@IcyPluginName("Get list size")
@IcyPluginCategory("Protocols")
@IcyPluginTags({"Protocols", "Tool", "List", "Size"})
public class ListSize extends Plugin implements ToolsBlock {
    private final VarMutableArray list = new VarMutableArray("List", null);

    private final VarInteger size = new VarInteger("Size", 0);

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("list", list);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("size", size);
    }

    @Override
    public void run() {
        size.setValue(list.size());
    }
}
