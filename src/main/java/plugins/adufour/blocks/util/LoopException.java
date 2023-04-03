package plugins.adufour.blocks.util;

import plugins.adufour.blocks.lang.WorkFlow;

/**
 * Exception raised whenever a loop is detected in an {@link WorkFlow}
 *
 * @author Alexandre Dufour
 */
public class LoopException extends BlocksException {
    public LoopException() {
        super("Cannot create loops inside a workflow", true);
    }
}
