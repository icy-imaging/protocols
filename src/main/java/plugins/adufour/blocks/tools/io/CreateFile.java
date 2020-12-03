package plugins.adufour.blocks.tools.io;

import java.io.File;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.gui.FileMode;
import plugins.adufour.vars.gui.model.FileTypeModel;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.VarException;
import icy.file.FileUtil;
import icy.plugin.abstract_.Plugin;

public class CreateFile extends Plugin implements IOBlock
{
    VarFile parentFolder = new VarFile("Base folder", new File(System.getProperty("user.home")));
    
    VarString fileName = new VarString("File name", "newFile");
    
    VarFile file = new VarFile("New file", null);
    
    VarBoolean overwrite = new VarBoolean("Overwrite", false);
    
    @Override
    public void declareInput(VarList inputMap)
    {
        parentFolder.setDefaultEditorModel(new FileTypeModel(parentFolder.getValue().getPath(), FileMode.FOLDERS, null, true));
        inputMap.add("parent folder", parentFolder);
        inputMap.add("file name", fileName);
        inputMap.add("overwrite", overwrite);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("new file", file);
    }
    
    @Override
    public void run()
    {
        File parent = parentFolder.getValue();
        
        if (parent == null || !parent.exists() || !parent.isDirectory()) throw new VarException(parentFolder, "The parent folder does not exist or is not a folder");
        
        if (fileName.getValue().isEmpty()) throw new VarException(fileName, "The name of the file cannot be empty");
        
        File newFile = new File(parentFolder + File.separator + fileName.getValue());
        
        if (!newFile.exists())
        {
            FileUtil.createFile(newFile);
        }
        else if (!newFile.isDirectory() && !overwrite.getValue())
        {
            throw new VarException(fileName, "Cannot create file " + newFile.getPath() + "\n=> A file with this name already exists");
        }
        
        file.setValue(newFile);
    }
}
