package plugins.adufour.protocols.livebar;

import icy.plugin.PluginDescriptor;
import icy.util.StringUtil;

import java.awt.Image;

import javax.swing.ImageIcon;

import org.pushingpixels.flamingo.api.common.RichTooltip;

/**
 * 
 * @author Stephane Dallongeville
 * @author Alexandre Dufour
 * 
 */
public class ProtocolRichToolTip extends RichTooltip
{
    public ProtocolRichToolTip(ProtocolDescriptor plugin)
    {
        super();
        
        final String name = plugin.getName();
        final String description = plugin.getDescription();
        final String website = plugin.getWeb();
        final String author = plugin.getAuthor();
        final ImageIcon plugIcon = plugin.getIcon();
        final Image plugImg = plugin.getImage();
        
        setTitle(name);
        if (plugIcon != PluginDescriptor.DEFAULT_ICON) setMainImage(plugIcon.getImage());
        
        if (!StringUtil.isEmpty(description))
        {
            for (String str : description.split("\n"))
                if (!StringUtil.isEmpty(str)) addDescriptionSection(str);
        }
        if (!StringUtil.isEmpty(website)) addDescriptionSection(website);
        if (!StringUtil.isEmpty(author)) addDescriptionSection(author);
        
        if (plugImg != PluginDescriptor.DEFAULT_IMAGE) setFooterImage(plugin.getImage());
    }
    
}
