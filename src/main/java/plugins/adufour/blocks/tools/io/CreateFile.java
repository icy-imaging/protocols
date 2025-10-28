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
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;

import java.io.File;

public class CreateFile extends Plugin implements IOBlock {
    private final VarFile parentFolder = new VarFile("Base folder", new File(System.getProperty("user.home")));

    private final VarString fileName = new VarString("File name", "newFile");

    private final VarFile file = new VarFile("New file", null);

    private final VarBoolean overwrite = new VarBoolean("Overwrite", false);

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        parentFolder.setDefaultEditorModel(new FileTypeModel(parentFolder.getValue().getPath(), FileMode.FOLDERS, null, true));
        inputMap.add("parent folder", parentFolder);
        inputMap.add("file name", fileName);
        inputMap.add("overwrite", overwrite);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("new file", file);
    }

    @Override
    public void run() {
        final File parent = parentFolder.getValue();

        if (parent == null || !parent.exists() || !parent.isDirectory()) throw new VarException(parentFolder, "The parent folder does not exist or is not a folder");

        if (fileName.getValue().isEmpty()) throw new VarException(fileName, "The name of the file cannot be empty");

        final File newFile = new File(parentFolder + File.separator + fileName.getValue());

        if (!newFile.exists()) {
            FileUtil.createFile(newFile);
        }
        else if (!newFile.isDirectory() && !overwrite.getValue()) {
            throw new VarException(fileName, "Cannot create file " + newFile.getPath() + "\n=> A file with this name already exists");
        }

        file.setValue(newFile);
    }
}
