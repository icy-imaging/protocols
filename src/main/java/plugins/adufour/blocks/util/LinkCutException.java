package plugins.adufour.blocks.util;

@SuppressWarnings("serial")
public class LinkCutException extends BlocksException
{
    public LinkCutException(String s)
    {
        super(s, true);
    }
}
