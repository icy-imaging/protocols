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

package plugins.adufour.blocks.tools.output;

import fr.icy.extension.plugin.abstract_.Plugin;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeListModel;
import plugins.adufour.vars.lang.VarFileArray;

/**
 * Input block reading a list of files
 *
 * @author Alexandre Dufour
 */
public class FilesOutput extends Plugin implements OutputBlock {
    @Override
    public void run() {
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        final VarFileArray vf = new VarFileArray("files", new java.io.File[0]);
        vf.setDefaultEditorModel(new FileTypeListModel("", FileMode.FILES, null, false));
        inputMap.add("files", vf);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
    }
}
