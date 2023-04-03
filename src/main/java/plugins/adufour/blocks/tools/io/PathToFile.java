package plugins.adufour.blocks.tools.io;

import icy.plugin.abstract_.Plugin;

import java.io.File;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarString;

public class PathToFile extends Plugin implements IOBlock {
    VarString path = new VarString("path", "", 1);

    VarFile file = new VarFile("file", null);

    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("input path", path);
    }

    @Override
    public void declareOutput(VarList outputMap) {
        outputMap.add("output file", file);
    }

    @Override
    public void run() {
        file.setValue(new File(path.getValue(true)));
    }
}
