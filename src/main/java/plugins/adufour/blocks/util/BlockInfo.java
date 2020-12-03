package plugins.adufour.blocks.util;

/**
 * @deprecated Using this interface is no longer recommended, as the information provided here can
 *             not be parsed either online or via the plug-in installer. Block information (name and
 *             description) will soon appear on the Icy web site (and the plug-in loader).
 * @author Alexandre Dufour
 * 
 */
@Deprecated
public interface BlockInfo
{
    /**
     * @return The title of the block (displays on the graphical interface)
     */
    String getName();
    
    /**
     * @return A short description of the block (will appear as a tool tip text when hovering on the
     *         block title)
     */
    String getDescription();
}
