package plugins.adufour.blocks.tools.output;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarFile;

/**
 * Input block reading a file
 *
 * @author Alexandre Dufour
 */
public class FileOutput extends Plugin implements OutputBlock {
    @Override
    public void run() {
    }

    @Override
    public void declareInput(VarList inputMap) {
        VarFile vf = new VarFile("file", null);
        vf.setDefaultEditorModel(new FileTypeModel("", FileMode.FILES, null, false));
        inputMap.add("file", vf);
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }

}
