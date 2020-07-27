package plugins.adufour.protocols.livebar;

import icy.file.FileUtil;
import icy.file.xml.XMLPersistent;
import icy.network.NetworkUtil;
import icy.plugin.PluginLoader;
import icy.util.XMLUtil;

import java.awt.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.ImageIcon;

import org.w3c.dom.Node;
import plugins.adufour.protocols.Protocols;

public class ProtocolDescriptor implements XMLPersistent
{
    private String filePath;
    
    private String fileDownloadURL;
    
    private String url;
    
    private String name;
    
    private String description;
    
    private String author;
    
    public ProtocolDescriptor(Node node)
    {
        loadFromXML(node);
    }
    
    public String getName()
    {
        return name;
    }
    
    public ImageIcon getIcon()
    {
        return PluginLoader.getPlugin(Protocols.class.getName()).getIcon();
    }
    
    public String getWeb()
    {
        return url;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public String getAuthor()
    {
        return author;
    }
    
    public Image getImage()
    {
        return null;
    }
    
    public File getFile()
    {
        try
        {
            FileOutputStream stream = new FileOutputStream(filePath);
            stream.write(NetworkUtil.download(fileDownloadURL, null, false));
            stream.flush();
            stream.close();
            
            return new File(filePath);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public boolean loadFromXML(Node node)
    {
        this.name = XMLUtil.getValue(XMLUtil.getElement(node, "name"), "No name");
        
        this.author = XMLUtil.getValue(XMLUtil.getElement(node, "author"), "No author");
        
        this.url = XMLUtil.getValue(XMLUtil.getElement(node, "url"), "http://icy.bioimageanalysis.org");
        
        this.description = XMLUtil.getValue(XMLUtil.getElement(node, "shortDescription"), "No description");
        
        // create the download folder if it doesn't exist yet
        FileUtil.createDir(Protocols.downloadedProtocolFolder);
        
        this.filePath = Protocols.downloadedProtocolFolder + File.separator + name + " v" + XMLUtil.getValue(XMLUtil.getElement(node, "version"), "0") + ".xml";
        
        this.fileDownloadURL = XMLUtil.getValue(XMLUtil.getElement(node, "fileurl"), "");
        
        return true;
    }
    
    @Override
    public boolean saveToXML(Node node)
    {
        // TODO Auto-generated method stub
        return false;
    }
    
}
