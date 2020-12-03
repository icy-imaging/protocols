package plugins.adufour.blocks.util;

public class NoSuchLinkException extends BlocksException
{
    private static final long serialVersionUID = 1L;
    
    public NoSuchLinkException(String message)
    {
        super(message, false);
    }
    
}
