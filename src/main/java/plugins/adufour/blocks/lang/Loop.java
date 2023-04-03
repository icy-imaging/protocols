package plugins.adufour.blocks.lang;

import icy.system.IcyHandledException;

import java.util.ArrayList;
import java.util.List;

import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.tools.ReLoop;
import plugins.adufour.blocks.util.BlocksException;
import plugins.adufour.blocks.util.LoopException;
import plugins.adufour.blocks.util.ScopeException;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarInteger;

/**
 * Special work flow that will repeat its contents forever until is it manually stopped by the user.
 *
 * @author Alexandre Dufour
 * @see Batch
 * @see RangeLoop
 * @see FileBatch
 * @see SequenceFileBatch
 * @see SequenceSeriesBatch
 */
public class Loop extends WorkFlow {
    private final ArrayList<Var<?>> loopVariables = new ArrayList<>();

    private VarInteger iterationCounter;

    public final VarBoolean stopOnFirstError = new VarBoolean("Stop on first error", false);

    public Loop() {
        super(false);

        declareLoopVariables(loopVariables);

        // always add the iteration counter in last position
        loopVariables.add(iterationCounter);
    }

    @Override
    public BlockDescriptor getBlock(int blockID) {
        if (blockID == -1)
            return getBlockDescriptor();

        return super.getBlock(blockID);
    }

    @Override
    public BlockDescriptor getInputOwner(Var<?> var) {
        return loopVariables.contains(var) ? getBlockDescriptor() : super.getInputOwner(var);
    }

    @Override
    public BlockDescriptor getOutputOwner(Var<?> var) {
        return loopVariables.contains(var) ? getBlockDescriptor() : super.getOutputOwner(var);
    }

    public String getInputVarID(Var<?> variable) {
        for (Var<?> innerVar : loopVariables)
            if (variable == innerVar) {
                return getBlockDescriptor().inputVars.getID(variable);
            }

        return super.getInputVarID(variable);
    }

    public String getOutputVarID(Var<?> variable) {
        for (Var<?> innerVar : loopVariables)
            if (variable == innerVar) {
                return getBlockDescriptor().outputVars.getID(variable);
            }

        return super.getOutputVarID(variable);
    }

    public VarInteger getIterationCounter() {
        return iterationCounter;
    }

    /**
     * Adds the specified variable to the list of inner loop variables, i.e., variables describing
     * the parameters of the loop, which can be referenced from inside the loop by other blocks.
     *
     * @param loopVar the variable to register
     * @deprecated The process of declaring loop variables has changed. Override
     * {@link #declareLoopVariables(List)} instead.
     */
    @Deprecated
    protected void addLoopVariable(Var<?> loopVar) {
        loopVariables.add(loopVar);
    }

    /**
     * @param var the variable to look for
     * @return true if <code>var</code> is a loop variable, false otherwise
     */
    public boolean isLoopVariable(Var<?> var) {
        return loopVariables.contains(var);
    }

    public List<Var<?>> getLoopVariables() {
        return loopVariables;
    }

    @Override
    public void run() {
        iterationCounter.setValue(0);
        initializeLoop();

        List<IcyHandledException> exceptions = new ArrayList<>();

        while (!Thread.currentThread().isInterrupted() && !isStopConditionReached()) {
            // FIXED (Stephane)
            // we need to reset to dirty state for all contained blocks before the iteration
            // as we never expect any contained block to keep value from previous iteration

            for (BlockDescriptor block : this) {
                // put block back in dirty state
                if (block.getStatus() == BlockStatus.READY)
                    block.setStatus(BlockStatus.DIRTY);
            }

            beforeIteration();

            try {
                super.run();
            }
            catch (IcyHandledException e) {
                exceptions.add(e);
                if (stopOnFirstError.getValue())
                    break;
            }

            iterationCounter.setValue(iterationCounter.getValue() + 1);
            afterIteration();
        }

        if (exceptions.size() > 0) {
            String message = "The following errors occurred during the loop:\n\n";
            for (Exception e : exceptions)
                message += " - " + e.getMessage() + "\n";
            throw new BlocksException(message, true);
        }
    }

    /**
     * Initializes (or resets) internal loop structures such as iterators or counters. This method
     * is called once before actually starting the loop. Note that the iteration counter is already
     * reset before calling this method.
     */
    public void initializeLoop() {
        // nothing to do in an infinite loop
    }

    /**
     * Called before the current iteration takes place (i.e. before calling the {@link #run()}
     * method. This method is typically used to fetch array or iterator values necessary for the
     * core code execution.
     */
    public void beforeIteration() {
        // nothing to do in an infinite loop
    }

    /**
     * Notifies that the current iteration has finished, allowing the loop to handle custom
     * increments or iterators to move forward before executing the next iteration. Note that the
     * iteration counter is automatically increased before calling this method.
     */
    public void afterIteration() {
        // release other threads for a very short time
        // (especially the AWT, to receive user interruption and display stuff)
        Thread.yield();

    }

    /**
     * NB: this method must be called by overriding methods to ensure that the loop can be
     * interrupted correctly upon request
     *
     * @return true if the stopping condition is reached, false otherwise.
     */
    public boolean isStopConditionReached() {
        // infinite loop
        return false;
    }

    @Override
    protected <T> void checkScope(Link<T> link) throws ScopeException {
        try {
            super.checkScope(link);
        }
        catch (ScopeException e) {
            // authorize links coming from variable loops
            if (!loopVariables.contains(link.srcVar))
                throw e;
        }
    }

    @Override
    protected <T> void checkLoop(Link<T> link) throws LoopException {
        try {
            super.checkLoop(link);
        }
        catch (LoopException e) {
            // authorize loops only if the destination is a "ReLoop" block
            if (link.dstBlock.getBlock() instanceof ReLoop) {
                ReLoop reLoop = (ReLoop) link.dstBlock.getBlock();

                if (link.dstVar == reLoop.reloopValue)
                    return;
            }

            throw e;
        }
    }

    @Override
    public void declareOutput(VarList outputMap) {
        super.declareOutput(outputMap);

        iterationCounter = new VarInteger("iteration", 0);
        iterationCounter.setEnabled(false);
        outputMap.add("iteration", iterationCounter);
    }

    /**
     * Declares the necessary loop variables by adding them to the specified list. Loop variables
     * describe the parameters of the loop, which can be referenced from inside the loop by other
     * blocks.
     *
     * @param loopVars the variable to register
     */
    public void declareLoopVariables(List<Var<?>> loopVars) {
        // a loop has no inner variable
    }
}
