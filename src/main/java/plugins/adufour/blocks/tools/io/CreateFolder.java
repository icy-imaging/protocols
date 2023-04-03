package plugins.adufour.blocks.tools.io;

import icy.file.FileUtil;
import icy.plugin.abstract_.Plugin;

import java.io.File;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;

public class CreateFolder extends Plugin implements IOBlock {
    VarFile parentFolder = new VarFile("Base folder", new File(System.getProperty("user.home")));

    VarString folderName = new VarString("Folder name", "myFolder");

    VarFile folder = new VarFile("New folder", null);

    @Override
    public void declareInput(VarList inputMap) {
        parentFolder.setDefaultEditorModel(new FileTypeModel(parentFolder.getValue().getPath(), FileMode.FOLDERS, null, true));
        inputMap.add("parent folder", parentFolder);
        inputMap.add("folder name", folderName);
    }

    @Override
    public void declareOutput(VarList outputMap) {
        outputMap.add("new folder", folder);
    }

    @Override
    public void run() {
        File parent = parentFolder.getValue();

        if (parent == null || !parent.exists() || !parent.isDirectory()) throw new VarException(parentFolder, "The parent folder does not exist or is not a folder");

        if (folderName.getValue().isEmpty()) throw new VarException(folderName, "The name of the folder cannot be empty");

        File newFolder = new File(parentFolder + File.separator + folderName.getValue());

        if (newFolder.exists() && !newFolder.isDirectory()) {
            throw new VarException(folderName, "Cannot create folder " + newFolder.getPath() + "\n=> A file with this name already exists");
        }

        FileUtil.createDir(newFolder);

        folder.setValue(newFolder);
    }
}
