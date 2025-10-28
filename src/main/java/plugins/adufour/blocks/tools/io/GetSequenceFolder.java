/*
 * Copyright (c) 2010-2025. Institut Pasteur.
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

import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import org.bioimageanalysis.icy.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.tools.sequence.SequenceBlock;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.util.VarException;

import java.io.File;

public class GetSequenceFolder extends Plugin implements SequenceBlock, IOBlock {
    private final VarSequence sequence = new VarSequence("Sequence", null);

    private final VarFile folder = new VarFile("Folder", null);

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("Sequence", sequence);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("Folder", folder);
    }

    @Override
    public void run() {
        final String filePath = sequence.getValue(true).getFilename();
        if (filePath == null) throw new VarException(sequence, "[Get sequence folder]: the selected sequence has not been saved on disk");

        final String folderPath = FileUtil.getDirectory(filePath);
        folder.setValue(new File(folderPath));
    }
}
