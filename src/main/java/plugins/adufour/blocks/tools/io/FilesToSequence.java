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

package plugins.adufour.blocks.tools.io;

import icy.file.Loader;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFileArray;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility block that reads multiple image files from disk into a {@link Sequence}
 *
 * @author Alexandre Dufour
 */
public class FilesToSequence extends Plugin implements IOBlock {
    private final VarFileArray inputFiles = new VarFileArray("input file", new File[]{});
    private final VarSequence outputSequence = new VarSequence("sequence", null);

    @Override
    public void run() {
        try {
            final File[] files = inputFiles.getValue(true);
            final List<String> paths = new ArrayList<>(files.length);

            for (final File file : files)
                if (file != null)
                    paths.add(file.getPath());

            outputSequence.setValue(Loader.loadSequence(paths, false));
        }
        catch (final Exception e) {
            throw new VarException(inputFiles, "unable to read files");
        }
    }

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("input file", inputFiles);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        outputMap.add("sequence", outputSequence);
    }

}
