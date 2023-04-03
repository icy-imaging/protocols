package plugins.adufour.blocks.lang;

import plugins.adufour.blocks.util.VarList;

/**
 * Interface indicating that implementing classes can be used in a block programming context
 *
 * @author Alexandre Dufour
 * @see WorkFlow
 */
public interface Block extends Runnable {
    /**
     * Fills the specified map with all the necessary input variables
     *
     * @param inputMap the list of input variables to fill
     */
    void declareInput(final VarList inputMap);

    /**
     * Fills the specified map with all the necessary output variables
     *
     * @param outputMap the list of output variables to fill
     */
    void declareOutput(final VarList outputMap);

    /**
     * Main method
     */
    void run();
}
