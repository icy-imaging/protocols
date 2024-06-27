/*
 * Copyright (c) 2010-2024. Institut Pasteur.
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

package plugins.adufour.blocks.lang;

import org.bioimageanalysis.icy.io.Loader;
import plugins.adufour.vars.lang.VarSequence;

import java.io.File;

/**
 * Similar to the {@link FileBatch File batch}, a Sequence file batch will read
 * all files of a user-defined folder, and additionally load and give access to
 * the sequence contained within these files (non-imaging files will be
 * skipped).<br>
 * Note for multi-series imaging files (e.g. Leica .lif): when loading
 * multi-series files, a dialog box will appear to let the user select the
 * series to process. However only the first selected series will be loaded. To
 * batch process entire multi-series files, use the {@link SequenceSeriesBatch
 * Sequence series batch} instead.
 *
 * @author Alexandre Dufour
 */
public class SequenceFileBatch extends FileBatch {
    VarSequence element;

    @Override
    public VarSequence getBatchElement() {
        if (element == null) {
            // WARNING: do *not* change the name of this variable
            // why? see declareInput() and VarList.add()
            element = new VarSequence("Sequence", null);
        }
        return element;
    }

    @Override
    public boolean accept(final File f) {
        return f.isDirectory() || super.accept(f) && Loader.isSupportedImageFile(f.getPath());
    }

    @Override
    public void beforeIteration() {
        try {
            element.setValue(Loader.loadSequence(files[getIterationCounter().getValue()].getPath(), 0, false));
            Thread.yield();
        }
        catch (final Exception e) {
            Thread.currentThread().interrupt();
        }
    }
}
