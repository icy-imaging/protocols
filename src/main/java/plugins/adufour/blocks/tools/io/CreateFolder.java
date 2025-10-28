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
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;

import java.io.File;

public class CreateFolder extends Plugin implements IOBlock {
    private final VarFile parentFolder = new VarFile("Base folder", new File(System.getProperty("user.home")));

    private final VarString folderName = new VarString("Folder name", "myFolder");

    private final VarFile folder = new VarFile("New folder", null);

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        parentFolder.setDefaultEditorModel(new FileTypeModel(parentFolder.getValue().getPath(), FileMode.FOLDERS, null, true));
        inputMap.add("parent folder", parentFolder);
        inputMap.add("folder name", folderName);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("new folder", folder);
    }

    @Override
    public void run() {
        final File parent = parentFolder.getValue();

        if (parent == null || !parent.exists() || !parent.isDirectory()) throw new VarException(parentFolder, "The parent folder does not exist or is not a folder");

        if (folderName.getValue().isEmpty()) throw new VarException(folderName, "The name of the folder cannot be empty");

        final File newFolder = new File(parentFolder + File.separator + folderName.getValue());

        if (newFolder.exists() && !newFolder.isDirectory()) {
            throw new VarException(folderName, "Cannot create folder " + newFolder.getPath() + "\n=> A file with this name already exists");
        }

        FileUtil.createDir(newFolder);

        folder.setValue(newFolder);
    }
}
