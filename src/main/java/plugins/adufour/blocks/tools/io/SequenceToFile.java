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
import fr.icy.extension.plugin.annotation_.IcyPluginCategory;
import fr.icy.extension.plugin.annotation_.IcyPluginName;
import fr.icy.extension.plugin.annotation_.IcyPluginTags;
import fr.icy.io.Saver;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarSequence;

import java.io.File;

/**
 * Utility block that saves a sequence to disk
 *
 * @author Alexandre Dufour
 */
@IcyPluginName("Sequence to file")
@IcyPluginCategory("Protocols")
@IcyPluginTags({"Protocols", "IO", "Sequence", "File"})
public class SequenceToFile extends Plugin implements IOBlock {
    private final VarFile folder = new VarFile("File or folder", null);
    private final VarSequence sequence = new VarSequence("sequence", null);

    @Override
    public void run() {
        File f = new File(folder.getValue(true).getAbsolutePath());
        if (f.isDirectory()) {
            f = new File(f.getAbsolutePath() + File.separator + sequence.getValue(true) + ".tif");
        }
        Saver.save(sequence.getValue(true), f, false, false);
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        folder.setDefaultEditorModel(new FileTypeModel("", FileMode.FOLDERS, null, false));
        inputMap.add("folder", folder);
        inputMap.add("sequence", sequence);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
    }
}
