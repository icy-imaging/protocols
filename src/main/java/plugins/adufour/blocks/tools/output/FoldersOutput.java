package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeListModel;
import plugins.adufour.vars.lang.VarFileArray;

/**
 * Input block reading a list of folders
 *
 * @author Alexandre Dufour
 */
public class FoldersOutput extends Plugin implements OutputBlock {
    @Override
    public void run() {
    }

    @Override
    public void declareInput(VarList inputMap) {
        VarFileArray vf = new VarFileArray("folders", new java.io.File[0]);
        vf.setDefaultEditorModel(new FileTypeListModel("", FileMode.FOLDERS, null, false));
        inputMap.add("folders", vf);
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }

}
