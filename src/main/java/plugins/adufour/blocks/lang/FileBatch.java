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

import org.bioimageanalysis.icy.Icy;
import org.bioimageanalysis.icy.gui.frame.progress.AnnounceFrame;
import org.bioimageanalysis.icy.io.FileUtil;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

/**
 * A file batch is a work-flow that will execute repeatedly on every file of a
 * user-selected folder. Files can be retrieved from sub-folders if necessary,
 * and filtered by extension. Note however that the files are given "as-is": it
 * is up to the user to indicate inside the work-flow how these files should be
 * read (for the particular case of image files, use the
 * {@link SequenceFileBatch Sequence file batch} instead).
 *
 * @author Alexandre Dufour
 */
public class FileBatch extends Batch implements FileFilter {
    // loop variables //

    private VarFile element;

    private VarFile folder;

    private VarString extension;

    private VarBoolean includeSubFolders;

    // local variables //

    /**
     * The list of files containing the elements to process
     */
    protected File[] files;

    @Override
    public Var<?> getBatchSource() {
        if (folder == null) {
            // WARNING: do *not* change the name of this variable
            // why? see declareInput() and VarList.add()
            folder = new VarFile("folder", null);
            folder.setDefaultEditorModel(new FileTypeModel(null, FileMode.FOLDERS, null, false));
        }
        return folder;
    }

    @Override
    public boolean accept(final File f) {
        final String ext = extension.getValue();
        return f.isDirectory() || ext.isEmpty() || f.getPath().toLowerCase().endsWith(ext.toLowerCase());
    }

    @Override
    public Var<?> getBatchElement() {
        if (element == null) {
            // WARNING: do *not* change the name of this variable
            // why? see declareInput() and VarList.add()
            element = new VarFile("file", null);
        }
        return element;
    }

    @Override
    public void initializeLoop() {
        if (folder.getValue() == null)
            throw new VarException(folder, "No folder indicated");

        final File file = folder.getValue();
        if (!file.isDirectory())
            throw new VarException(folder, file.getAbsolutePath() + " is not a folder");

        AnnounceFrame process = null;

        if (!Icy.getMainInterface().isHeadLess())
            process = new AnnounceFrame("Listing files...");

        files = FileUtil.getFiles(file, this, includeSubFolders.getValue(), false, false);

        if (process != null)
            process.close();
    }

    @Override
    public void beforeIteration() {
        element.setValue(files[getIterationCounter().getValue()]);
    }

    @Override
    public boolean isStopConditionReached() {
        return getIterationCounter().getValue() == files.length;
    }

    @Override
    public void declareInput(final VarList inputMap) {
        super.declareInput(inputMap);

        inputMap.add("extension", extension = new VarString("extension", ""));
        inputMap.add("Include sub-folders", includeSubFolders = new VarBoolean("Include sub-folders", true));
    }

    @Override
    public void declareLoopVariables(final List<Var<?>> loopVariables) {
        super.declareLoopVariables(loopVariables);

        loopVariables.add(extension);
        loopVariables.add(includeSubFolders);
    }
}
