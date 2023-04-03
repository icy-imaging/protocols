package plugins.adufour.blocks.tools.input;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarFile;

/**
 * Input block reading a folder
 *
 * @author Alexandre Dufour
 */
public class Folder extends Plugin implements InputBlock {
    @Override
    public void run() {
    }

    @Override
    public void declareInput(VarList inputMap) {
        VarFile vf = new VarFile("folder", null);
        vf.setDefaultEditorModel(new FileTypeModel("", FileMode.FOLDERS, null, false));
        inputMap.add("folder", vf);
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }

}
