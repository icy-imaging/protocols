package plugins.adufour.blocks.util;

/**
 * Convenience class for the Blocks framework that allows optimizing bug finding / reporting /
 * fixing.
 * 
 * @author Alexandre Dufour
 */
public class BlocksException extends RuntimeException
{
    private static final long serialVersionUID = 666L;
    
    public final boolean      catchException;
    
    /**
     * Creates a new exception with the specified message and catching behavior.
     * 
     * @param message
     *            the error message bound the current exception
     * @param catchException
     *            true if the exception should be caught within the EzPlug layer, false to let the
     *            exception pass to the global exception manager
     */
    public BlocksException(String message, boolean catchException)
    {
        super(message);
        this.catchException = catchException;
    }
}
