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

package plugins.adufour.blocks.tools.io;

import fr.icy.extension.plugin.abstract_.Plugin;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarString;

public class FileToPath extends Plugin implements IOBlock {
    private final VarString path = new VarString("path", "", 1);

    private final VarFile file = new VarFile("file", null);

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("input file", file);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("output path", path);
    }

    @Override
    public void run() {
        path.setValue(file.getValue() == null ? null : file.getValue().getPath());
    }
}
