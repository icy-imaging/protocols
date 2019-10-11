package plugins.adufour.protocols;

import icy.common.exception.UnsupportedFormatException;
import icy.file.FileImporter;
import icy.gui.frame.progress.FileFrame;
import icy.plugin.abstract_.Plugin;
import icy.system.thread.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.filechooser.FileFilter;

/**
 * Importer class that will automatically load a protocol file dropped inside Icy
 * 
 * @author Alexandre Dufour
 */
public class ProtocolImporter extends Plugin implements FileImporter
{
    @Override
    public boolean acceptFile(String path)
    {
        return path.toLowerCase().endsWith(".protocol") && new File(path).isFile();
    }
    
    @Override
    public List<FileFilter> getFileFilters()
    {
        FileFilter filter = new FileFilter()
        {
            @Override
            public boolean accept(File f)
            {
                return acceptFile(f.getPath());
            }
            
            @Override
            public String getDescription()
            {
                return "Icy protocols (.protocol)";
            }
        };
        
        return Arrays.asList(filter);
    }
    
    @Override
    public boolean load(final String path, FileFrame loadingFrame) throws UnsupportedFormatException, IOException
    {
        try
        {
            return ThreadUtil.invokeNow(new Callable<Boolean>()
            {
                @Override
                public Boolean call() throws Exception
                {
                    Protocols.loadWorkFlow(new File(path));
                    
                    return true;
                }
            });
            
        }
        catch (Exception e)
        {
            return false;
        }
    }
}