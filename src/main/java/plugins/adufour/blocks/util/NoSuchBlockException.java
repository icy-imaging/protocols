package plugins.adufour.blocks.util;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;

public class NoSuchBlockException extends BlocksException
{
    private static final long serialVersionUID = 1L;
    
    public NoSuchBlockException(BlockDescriptor block)
    {
        super("Cannot find block '" + (block == null ? "null" : block.getName()) + "'", false);
    }
    
    public NoSuchBlockException(int ID)
    {
        super("Cannot find block with ID '" + ID + "'", false);
    }
    
    public NoSuchBlockException(int ID, WorkFlow container)
    {
        super("Cannot find block with ID '" + ID + "' in workflow '" + container.getBlockDescriptor().getName() + "'", false);
        
    }
}
