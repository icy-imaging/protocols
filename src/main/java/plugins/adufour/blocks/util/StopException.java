package plugins.adufour.blocks.util;

@SuppressWarnings("serial")
public class StopException extends RuntimeException
{
    public StopException()
    {
        super("This workflow was interrupted prematurely");
    }
}
