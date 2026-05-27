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
import fr.icy.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarString;

import java.io.File;

public class AppendFilePath extends Plugin implements IOBlock {
    private final VarMutable in = new VarMutable("Current file", null);

    private final VarFile out = new VarFile("New file", null);

    private final VarBoolean removeExt = new VarBoolean("Remove extension", false);

    private final VarString suffix = new VarString("Add suffix", "");

    @Override
    public void run() {
        final Object input = in.getValue(true);
        String newPath;

        if (input instanceof File)
            newPath = ((File) input).getAbsolutePath();
        else newPath = input.toString();

        if (removeExt.getValue()) {
            final String fileName = FileUtil.getFileName(newPath, false);
            newPath = FileUtil.getDirectory(newPath) + fileName;
        }

        newPath += suffix.getValue();

        out.setValue(new File(newPath));
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("input file", in);
        inputMap.add("remove ext.", removeExt);
        inputMap.add("suffix", suffix);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("output file", out);
    }

}
