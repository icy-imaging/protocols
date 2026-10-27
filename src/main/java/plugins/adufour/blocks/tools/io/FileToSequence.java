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
import fr.icy.io.Loader;
import fr.icy.model.sequence.Sequence;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

import java.io.File;

/**
 * Utility block that reads a file from disk
 *
 * @author Alexandre Dufour
 */
@IcyPluginName("File to sequence")
@IcyPluginCategory("Protocols")
@IcyPluginTags({"Protocols", "IO", "File", "Sequence"})
public class FileToSequence extends Plugin implements IOBlock {
    private final VarFile inputFile = new VarFile("input file", null);
    private final VarSequence outputSequence = new VarSequence("sequence", null);
    private final VarInteger series = new VarInteger("Series", 0);

    @Override
    public void run() {
        try {
            final File file = inputFile.getValue(true);
            final Sequence sequence = Loader.loadSequence(file.getPath(), series.getValue(), false);
            if (sequence == null) throw new VarException(inputFile, "Cannot read " + file.getPath() + " into a sequence");
            outputSequence.setValue(sequence);
        }
        catch (final Exception e) {
            final File file = inputFile.getValue(true);
            throw new VarException(inputFile, "unable to read file " + file.getAbsolutePath());
        }
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("input file", inputFile);
        inputMap.add("Series", series);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("sequence", outputSequence);
    }

}
