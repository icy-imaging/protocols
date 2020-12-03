package plugins.adufour.blocks.util;

@SuppressWarnings("serial")
public class BlocksReloadedException extends BlocksException
{
    public BlocksReloadedException()
    {
        super("The plugin list has been updated", true);
    }
}
