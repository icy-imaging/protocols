package plugins.adufour.blocks.tools.io;

import icy.file.Saver;
import icy.plugin.abstract_.Plugin;

import java.io.File;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarSequence;

/**
 * Utility block that saves a sequence to disk
 *
 * @author Alexandre Dufour
 */
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
    public void declareInput(VarList inputMap) {
        folder.setDefaultEditorModel(new FileTypeModel("", FileMode.FOLDERS, null, false));
        inputMap.add("folder", folder);
        inputMap.add("sequence", sequence);
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }
}
