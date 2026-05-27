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
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarFile;

/**
 * Input block reading a file
 *
 * @author Alexandre Dufour
 */
public class File extends Plugin implements InputBlock {
    @Override
    public void run() {
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        final VarFile vf = new VarFile("file", null);
        vf.setDefaultEditorModel(new FileTypeModel("", FileMode.FILES, null, false));
        inputMap.add("file", vf);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
    }

}
