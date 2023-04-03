package plugins.adufour.blocks.lang;

import java.awt.Point;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import icy.gui.dialog.ConfirmDialog;
import icy.main.Icy;
import icy.math.UnitUtil;
import icy.plugin.abstract_.Plugin;
import icy.system.IcyHandledException;
import icy.system.thread.ThreadUtil;
import icy.util.StringUtil;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.tools.input.InputBlock;
import plugins.adufour.blocks.tools.output.OutputBlock;
import plugins.adufour.blocks.util.BlockListener;
import plugins.adufour.blocks.util.BlocksException;
import plugins.adufour.blocks.util.LinkCutException;
import plugins.adufour.blocks.util.LoopException;
import plugins.adufour.blocks.util.NoSuchBlockException;
import plugins.adufour.blocks.util.NoSuchLinkException;
import plugins.adufour.blocks.util.NoSuchVariableException;
import plugins.adufour.blocks.util.ScopeException;
import plugins.adufour.blocks.util.StopException;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.blocks.util.VarListListener;
import plugins.adufour.blocks.util.WorkFlowListener;
import plugins.adufour.protocols.gui.MainFrame;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarObject;
import plugins.adufour.vars.util.VarException;

public class WorkFlow extends Plugin implements Block, Iterable<BlockDescriptor>, BlockListener, WorkFlowListener {
    private final BlockDescriptor descriptor = new BlockDescriptor(-1, this);

    private final HashSet<WorkFlowListener> listeners = new HashSet<>();

    private final ArrayList<BlockDescriptor> orderedBlocks = new ArrayList<>();

    private final boolean topLevel;

    private final ArrayList<Link<?>> links = new ArrayList<>();

    private final VarListListener inputVarListener = new VarListListener() {
        @Override
        public void variableAdded(VarList list, Var<?> variable) {
            descriptor.addInput(getInputVarID(variable), variable);
        }

        @Override
        public void variableRemoved(VarList list, Var<?> variable) {
            descriptor.removeInput(variable);
        }
    };

    private final VarListListener outputVarListener = new VarListListener() {
        @Override
        public void variableAdded(VarList list, Var<?> variable) {
            descriptor.addOutput(getOutputVarID(variable), variable);
        }

        @Override
        public void variableRemoved(VarList list, Var<?> variable) {
            descriptor.removeOutput(variable);
        }
    };

    private Thread executionThread;

    private boolean userInterruption = false;

    private ArrayList<BlockDescriptor> blockSelection = new ArrayList<>();
    private ArrayList<Link<?>> linkSelection = new ArrayList<>();

    private static final OutputStream nullStream = new OutputStream() {
        @Override
        public void write(int b) {
        }
    };

    /**
     * The stream where the execution log is sent.
     */
    private PrintStream logStream = new PrintStream(nullStream);

    public void setLogStream(PrintStream logStream) {
        this.logStream = logStream;
    }

    /**
     * Searches recursively for the top-level stream where the execution log will be sent
     *
     * @return The top-level stream where the log is sent
     */
    public PrintStream getLogStream() {
        return descriptor.getContainer() == null ? logStream : descriptor.getContainer().getLogStream();
    }

    public WorkFlow() {
        this(false);
    }

    public WorkFlow(boolean topLevel) {
        super();

        this.topLevel = topLevel;
    }

    public void addBlock(BlockDescriptor blockInfo) {
        blockInfo.setContainer(this);

        orderedBlocks.add(blockInfo);
        blockInfo.addBlockListener(this);

        if (blockInfo.isWorkFlow())
            ((WorkFlow) blockInfo.getBlock()).addListener(this);

        // FIX: sometime getContainer() isn't yet initialized even for internal workflow (Stephane)
        // if (descriptor.getContainer() != null)
        if (!descriptor.isTopLevelWorkFlow()) {
            // Add the variables from the block to the work flow itself
            // this is to allow variable exposing

            // Retrieve input variables
            for (Var<?> inputVar : blockInfo.inputVars) {
                // FIX: avoid to duplicate variable (Stephane)
                if (!descriptor.inputVars.contains(inputVar))
                    descriptor.addInput(getInputVarID(inputVar), inputVar);
            }
            // Retrieve output variables
            for (Var<?> outputVar : blockInfo.outputVars) {
                // FIX: avoid to duplicate variable (Stephane)
                if (!descriptor.outputVars.contains(outputVar))
                    descriptor.addOutput(getOutputVarID(outputVar), outputVar);
            }

            blockInfo.inputVars.addVarListListener(inputVarListener);
            blockInfo.outputVars.addVarListListener(outputVarListener);
        }

        blockAdded(this, blockInfo);
    }

    /**
     * @param block Block to add.
     * @return Descriptor relative to this workflow of the added block.
     * @deprecated Use {@link #addBlock(BlockDescriptor)} instead.
     */
    @Deprecated
    public BlockDescriptor addBlock(Block block) {
        return addBlock(-1, block, new Point());
    }

    /**
     * @param ID       Block identifier.
     * @param block    Block to be added.
     * @param location Position to put the block at.
     * @return Descriptor relative to this workflow of the added block.
     * @deprecated Use {@link #addBlock(BlockDescriptor)} instead.
     */
    @Deprecated
    public BlockDescriptor addBlock(int ID, Block block, Point location) {
        BlockDescriptor blockDescriptor;

        if (block instanceof WorkFlow) {
            blockDescriptor = ((WorkFlow) block).descriptor;
            blockDescriptor.setContainer(this);
            blockDescriptor.setLocation(location.x, location.y);
        }
        else {
            blockDescriptor = new BlockDescriptor(ID, block, this, location);
        }

        addBlock(blockDescriptor);

        return blockDescriptor;
    }

    /**
     * Links the specified variables
     *
     * @param <T>      Type of the variable to be linked.
     * @param srcBlock the source block
     * @param srcArgID the unique ID of the source (output) variable
     * @param dstBlock the destination block
     * @param dstArgID the unique ID of the destination (input) variable
     * @return the newly created link
     * @throws NoSuchBlockException    if either the source or destination block is not in this work flow
     * @throws NoSuchVariableException if either the source or destination variable is not in the corresponding block
     */
    public <T> Link<T> addLink(BlockDescriptor srcBlock, String srcArgID, BlockDescriptor dstBlock, String dstArgID)
            throws NoSuchBlockException, NoSuchVariableException, ClassCastException {
        if (!orderedBlocks.contains(srcBlock))
            throw new NoSuchBlockException(srcBlock);
        if (!orderedBlocks.contains(dstBlock))
            throw new NoSuchBlockException(dstBlock);

        Var<T> srcVar = srcBlock.outputVars.get(srcArgID);
        if (srcVar == null)
            throw new NoSuchVariableException(srcBlock, srcArgID);

        Var<T> dstVar = dstBlock.inputVars.get(dstArgID);
        if (dstVar == null)
            throw new NoSuchVariableException(dstBlock, dstArgID);

        return addLink(srcBlock, srcVar, dstBlock, dstVar);
    }

    /**
     * Creates a link between the given variables and re-schedules the blocks accordingly
     *
     * @param <T>      Type of the value of the link that is generated.
     * @param srcBlock Start-point block descriptor of the link.
     * @param srcVar   Start-point variable instance of the link.
     * @param dstBlock End-point block descriptor of the link.
     * @param dstVar   End-point variable instance of the link.
     * @return Link instance connecting source and destination blocks.
     * @throws LoopException           if a loop is detected in the current scope
     * @throws NoSuchVariableException if the variable cannot be found in the current scope
     * @throws ClassCastException      If the link is not valid, meaning that the two specified variables are of
     *                                 incompatible types.
     */
    public <T> Link<T> addLink(BlockDescriptor srcBlock, Var<T> srcVar, BlockDescriptor dstBlock, Var<T> dstVar)
            throws LoopException, ClassCastException {
        Link<T> link = checkLink(srcBlock, srcVar, dstBlock, dstVar);

        dstVar.setReference(srcVar);

        // re-order the boxes to make sure srcBlock runs before dstBlock
        if (orderedBlocks.indexOf(link.srcBlock) > orderedBlocks.indexOf(link.dstBlock))
            reOrder(link.srcBlock, link.dstBlock);

        links.add(link);

        for (WorkFlowListener listener : listeners) {
            listener.linkAdded(this, link);
        }

        return link;
    }

    public void addListener(WorkFlowListener listener) {
        listeners.add(listener);
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public boolean contains(BlockDescriptor dstBlock) {
        return orderedBlocks.contains(dstBlock);
    }

    /**
     * Checks whether the specified variables can be linked, and if so returns the pair of
     * {@link Block} objects to link
     *
     * @param <T>      Type of value being transported through the link.
     * @param srcBlock the {@link Block} to link from
     * @param srcVar   the {@link Var}iable to link from
     * @param dstBlock the {@link Block} to link to
     * @param dstVar   the {@link Var}iable to link to
     * @return The link instance connecting source and destination variables.
     * @throws ClassCastException if the link is not valid, meaning that the two specified variables are of
     *                            incompatible types
     */
    public <T> Link<T> checkLink(final BlockDescriptor srcBlock, Var<T> srcVar, final BlockDescriptor dstBlock, Var<T> dstVar) throws ClassCastException {
        Link<T> link = new Link<>(this, srcBlock, srcVar, dstBlock, dstVar);

        checkScope(link);

        checkLoop(link);

        checkType(link);

        return link;
    }

    protected <T> void checkScope(Link<T> link) throws ScopeException {
        if (contains(link.srcBlock) != contains(link.dstBlock))
            throw new ScopeException();

        // special case: inner loop variables should not link to outside the loop
        if (link.srcBlock.isLoop() && ((Loop) link.srcBlock.getBlock()).isLoopVariable(link.srcVar))
            throw new ScopeException();
    }

    protected <T> void checkLoop(Link<T> link) throws LoopException {
        if (link.srcBlock.equals(link.dstBlock) || depends(link.srcBlock, link.dstBlock))
            throw new LoopException();
    }

    protected <T> void checkType(Link<T> link) throws ClassCastException {
        // filter all valid cases

        if (link.dstVar.isAssignableFrom(link.srcVar))
            return;

        if (link.srcVar instanceof VarObject)
            return;

        if (link.dstVar instanceof VarMutable)
            return;
        // {
        // VarMutable dst = (VarMutable) link.dstVar;
        // dst.setType(link.srcVar.getType());
        // return;
        // }

        throw new ClassCastException("Variables \"" + link.dstVar.getName() + "\" and \""
                + link.srcVar.getName() + "\" are of different type and cannot be linked");
    }

    @Override
    public void declareInput(VarList inputMap) {
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }

    private boolean depends(BlockDescriptor srcBlock, BlockDescriptor dstBlock) {
        boolean srcDependsOnDst = false;

        for (Link<?> link : links) {
            if (link.dstBlock == srcBlock) {
                // link points to srcBlock.
                if (link.srcBlock == dstBlock)
                    return true;

                // else
                srcDependsOnDst |= depends(link.srcBlock, dstBlock);

                // exit ASAP
                if (srcDependsOnDst)
                    return true;
            }
        }

        return srcDependsOnDst;
    }

    /**
     * @param recursive if we also want blocks from sub workflow
     * @return all blocks contained in current workflow
     */
    public List<BlockDescriptor> getBlockDescriptors(boolean recursive) {
        final List<BlockDescriptor> result = new ArrayList<>();

        synchronized (orderedBlocks) {
            for (BlockDescriptor block : orderedBlocks) {
                if (recursive && block.isWorkFlow())
                    result.addAll(((WorkFlow) block.getBlock()).getBlockDescriptors(true));

                result.add(block);
            }
        }

        return result;
    }

    /**
     * @param recursive if we also want blocks from sub workflow
     * @return all Input type blocks ({@link InputBlock} contained in current workflow
     */
    public List<BlockDescriptor> getInputBlockDescriptors(boolean recursive) {
        final List<BlockDescriptor> result = new ArrayList<>();

        for (BlockDescriptor block : getBlockDescriptors(recursive))
            if (block.isInput())
                result.add(block);

        return result;
    }

    /**
     * @param recursive if we also want blocks from sub workflow
     * @return all Output type blocks ({@link OutputBlock} contained in current workflow
     */
    public List<BlockDescriptor> getOutputBlockDescriptors(boolean recursive) {
        final List<BlockDescriptor> result = new ArrayList<>();

        for (BlockDescriptor block : getBlockDescriptors(recursive))
            if (block.isOutput())
                result.add(block);

        return result;
    }

    public BlockDescriptor getBlock(int index) {
        return orderedBlocks.get(index);
    }

    public BlockDescriptor getBlockByID(int blockID) throws NoSuchBlockException {
        if (blockID == descriptor.getID())
            return descriptor;

        for (BlockDescriptor bd : orderedBlocks)
            if (bd.getID() == blockID)
                return bd;

        throw new NoSuchBlockException(blockID, this);
    }

    /**
     * @param variable The input variable to retrieve.
     * @return A unique ID for the variable within this work flow (used for XML loading/saving).
     */
    public String getInputVarID(Var<?> variable) {
        BlockDescriptor owner = getInputOwner(variable);

        return owner.getID() + ":" + owner.getVarID(variable);
    }

    /**
     * @param variable The input variable to retrieve.
     * @return A unique ID for the variable within this work flow (used for XML loading/saving).
     */
    public String getOutputVarID(Var<?> variable) {
        BlockDescriptor owner = getOutputOwner(variable);

        return owner.getID() + ":" + owner.getVarID(variable);
    }

    public BlockDescriptor getBlockDescriptor() {
        return descriptor;
    }

    public BlockDescriptor getInputOwner(Var<?> var) {
        for (BlockDescriptor blockInfo : orderedBlocks)
            for (Var<?> inputVar : blockInfo.inputVars)
                if (inputVar.equals(var))
                    return blockInfo;

        return null;
    }

    public BlockDescriptor getOutputOwner(Var<?> var) {
        for (BlockDescriptor blockInfo : orderedBlocks)
            for (Var<?> outputVar : blockInfo.outputVars)
                if (outputVar.equals(var))
                    return blockInfo;

        return null;
    }

    public Iterable<Link<?>> getLinksIterator() {
        return links;
    }

    public int indexOf(BlockDescriptor blockInfo) {
        if (blockInfo == null)
            return -1;

        return orderedBlocks.indexOf(blockInfo);
    }

    @Override
    public Iterator<BlockDescriptor> iterator() {
        // return orderedBlocks.iterator();
        return new Iterator<BlockDescriptor>() {
            private final Iterator<BlockDescriptor> orderedBlocksIt = orderedBlocks.iterator();

            @Override
            public boolean hasNext() {
                return orderedBlocksIt.hasNext();
            }

            @Override
            public BlockDescriptor next() {
                return orderedBlocksIt.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    /**
     * Give the highest priority to the specified block, by placing giving it the smallest possible
     * index in the list (i.e. after its direct dependencies). This method reorganizes the block execution order.
     *
     * @param blockDesc Descriptor of the block to be prioritized.
     */
    public void prioritize(BlockDescriptor blockDesc) {
        // retrieve the current order
        int currentOrder = indexOf(blockDesc);
        // assume we can give the block maximum priority
        int targetOrder = 0;

        // find its dependencies
        HashSet<BlockDescriptor> dependencies = new HashSet<>();

        for (Link<?> link : links)
            if (link.dstBlock == blockDesc && contains(link.srcBlock))
                dependencies.add(link.srcBlock);

        // give piority to the dependencies first
        for (BlockDescriptor dependency : dependencies)
            prioritize(dependency);

        // calculate the final order
        for (BlockDescriptor dependency : dependencies) {
            int order = indexOf(dependency) + 1;
            if (order > targetOrder)
                targetOrder = order;
        }

        // assign the final order

        if (targetOrder != currentOrder) {
            orderedBlocks.add(targetOrder, orderedBlocks.remove(currentOrder));
            for (WorkFlowListener listener : listeners)
                listener.workFlowReordered(this);
        }
    }

    /**
     * Re-orders the specified blocks to ensure that prevBlock will execute before nextBlock.<br>
     * NOTE: this method assumes that the specified blocks belong to the current work flow. If this
     * is not the case, see the {@link #reOrder(Link)} method instead.
     *
     * @param prevBlock The block that should be executed right before this instance.
     * @param nextBlock The block that should be executed right after this instance.
     */
    private void reOrder(BlockDescriptor prevBlock, BlockDescriptor nextBlock) {
        orderedBlocks.remove(prevBlock);
        orderedBlocks.add(orderedBlocks.indexOf(nextBlock), prevBlock);

        for (Link<?> link : links)
            if (link.dstBlock == prevBlock && orderedBlocks.indexOf(link.srcBlock) > orderedBlocks.indexOf(prevBlock)) {
                reOrder(link.srcBlock, prevBlock);
                break; // only do it once, even if multiple links exist
            }

        for (WorkFlowListener listener : listeners)
            listener.workFlowReordered(this);
    }

    public void removeBlock(BlockDescriptor blockInfo, boolean checkSelection) {
        // pops out workflows' selections
        if (blockInfo.getBlock() instanceof WorkFlow) {
            WorkFlow wf = ((WorkFlow) blockInfo.getBlock());
            if (checkSelection && !wf.getBlockSelection().isEmpty())
                try {
                    // use location in main container instead of in the inner workflow
                    // avoids jumping toward the top left
                    for (BlockDescriptor bd : wf) {
                        Point contLoc = bd.getContainer().getBlockDescriptor().getLocation();
                        Point loc = bd.getLocation();
                        bd.setLocation(contLoc.x + loc.x, contLoc.y + loc.y);
                    }
                    MainFrame.copySelection(wf, true);
                    MainFrame.pasteSelection(this, true);
                }
                catch (LinkCutException e) {
                    if (ConfirmDialog.confirm("Warning", e.getMessage(), ConfirmDialog.OK_CANCEL_OPTION)) {
                        MainFrame.pasteSelection(this, true);
                    }
                    else
                        return;
                }
            else {
                checkSelection = false;
            }
        }

        // 1) remove all links to/from this block
        // this must be done first for self-adjusting blocks
        // example: Accumulator may remove some variables as soon as they get unlinked

        // 1.a) links from this block elsewhere
        for (int i = 0; i < links.size(); i++) {
            Link<?> link = links.get(i);

            if (blockInfo == link.srcBlock) {
                link.dstVar.setReference(null);
                links.remove(i--);

                for (WorkFlowListener listener : listeners)
                    listener.linkRemoved(this, link);
            }
        }

        // 1.b) links from elsewhere to this block
        for (int i = 0; i < links.size(); i++) {
            Link<?> link = links.get(i);

            if (blockInfo == link.dstBlock) {
                link.dstVar.setReference(null);
                links.remove(i--);

                for (WorkFlowListener listener : listeners)
                    listener.linkRemoved(this, link);
            }
        }

        // 2) remove the block variables (this removes their exposure as well)

        // a work flow does not have variables of its own => nothing to remove
        if (!blockInfo.isWorkFlow()) {
            // Remove input variables
            for (Var<?> inputVar : blockInfo.inputVars)
                this.descriptor.removeInput(inputVar);

            // Remove output variables
            for (Var<?> outputVar : blockInfo.outputVars)
                this.descriptor.removeOutput(outputVar);
        }

        // 3) if the block is a work flow, remove its inner blocks

        // if (blockInfo.isWorkFlow())
        // {
        // WorkFlow innerFlow = (WorkFlow) blockInfo.getBlock();
        // while (innerFlow.size() > 0)
        // innerFlow.removeBlock(innerFlow.getBlock(0), checkSelection);
        // }

        // 4) remove the block itself

        orderedBlocks.remove(blockInfo);

        blockInfo.removeBlockListener(this);

        // 5) Notify listeners...

        for (WorkFlowListener listener : listeners) {
            listener.blockRemoved(this, blockInfo);
            listener.workFlowReordered(this);
        }

        blockInfo.inputVars.removeVarListListener(inputVarListener);
        blockInfo.outputVars.removeVarListListener(outputVarListener);

        blockSelection.remove(blockInfo);
    }

    /**
     * @param var the variable (input or output) to look for
     * @return true if the specified variable is linked to another block, false otherwise
     */
    public boolean isLinked(Var<?> var) {
        for (Link<?> link : links)
            if (link.dstVar == var || link.srcVar == var)
                return true;
        return false;
    }

    /**
     * Removes the link to the specified variable, and returns false if this variable wasn't linked
     *
     * @param dstVar the destination variable of the link to remove
     */
    public void removeLink(Var<?> dstVar) {
        for (Link<?> link : links)
            if (link.dstVar == dstVar && links.remove(link)) {
                link.dstVar.setReference(null);

                for (WorkFlowListener listener : listeners)
                    listener.linkRemoved(this, link);

                return;
            }

        // if code runs here, no link was found
        // => check in the parent work flow for links to an "exposed" variable

        if (descriptor.getContainer() != null) {
            descriptor.getContainer().removeLink(dstVar);
        }
        else {
            throw new NoSuchLinkException("In method WorkFlow.removeLink():\nNo link points to "
                    + getInputOwner(dstVar).getName() + " > " + dstVar.getName());
        }
    }

    public void removeListener(WorkFlowListener listener) {
        listeners.remove(listener);
    }

    /**
     * Resets all output variables of each block in this work flow to their default value
     */
    public void reset() {
        for (BlockDescriptor block : orderedBlocks)
            block.reset();
    }

    /**
     * Resets and runs this work flow in an independent thread.
     */
    public void runWorkFlow() {
        runWorkFlow(false);
    }

    /**
     * Resets and runs this work flow in an independent thread.
     *
     * @param waitForCompletion <code>true</code> if this method should wait for the work flow to finish before
     *                          returning, or <code>false</code> if the method should return immediately (in this
     *                          case, the status of the work flow can be accessed via {@link #getDescriptor()}
     *                          {@link BlockDescriptor#getStatus() .getStatus()})
     */
    public void runWorkFlow(boolean waitForCompletion) {
        // this is the top-level work flow
        // force it "dirty" to run it
        this.descriptor.setStatus(BlockStatus.DIRTY);
        executionThread = new Thread(this.descriptor, "Workflow");
        executionThread.start();

        if (waitForCompletion) {
            while (descriptor.getStatus() == BlockStatus.RUNNING)
                ThreadUtil.sleep(100);
        }
    }

    public void interrupt() {
        if (executionThread != null) {
            executionThread.interrupt();
            userInterruption = true;
        }
        else {
            // this is not the top-level workflow
            // => look for it
            descriptor.getContainer().interrupt();
        }
    }

    private boolean isInterrupted() {
        if (Icy.getMainInterface().isHeadLess())
            return false;

        if (descriptor.getContainer() != null)
            return descriptor.getContainer().isInterrupted();

        return userInterruption || executionThread.isInterrupted();
    }

    /**
     * DO NOT CALL DIRECTLY FROM CLIENT CODE: this method assumes running in its own thread, and may
     * behave inconsistently if interrupted. Use the {@link #runWorkFlow()} method instead
     */
    public void run() {
        if (orderedBlocks.size() == 0)
            return;

        descriptor.setStatus(BlockStatus.RUNNING);

        BlockDescriptor runningBlock = null;

        String finalStatus = "";
        userInterruption = false;

        long startTime = 0, endTime = 0;
        try {
            startTime = System.nanoTime();

            String statusPrefix = getBlockDescriptor().getContainer() == null ? ""
                    : "\"" + getBlockDescriptor().getContainer().descriptor.getName() + "\"" + " => ";

            // get the log stream recursively to ensure it is always the top-level one
            PrintStream log = getLogStream();

            for (int blockIndex = 0; blockIndex < orderedBlocks.size(); blockIndex++) {
                if (isInterrupted())
                    throw new StopException();

                BlockDescriptor blockDescriptor = orderedBlocks.get(blockIndex);

                if (blockDescriptor.getStatus() != BlockStatus.READY) {
                    runningBlock = blockDescriptor;

                    String blockLog = "block #" + (blockIndex + 1) + " [" + blockDescriptor.getDefinedName() + "]";

                    // Adjust status
                    statusChanged(this, "Running block " + statusPrefix + "\"" + blockDescriptor.getDefinedName()
                            + "\" (#" + (blockIndex + 1) + ")...");

                    // Log should be indented
                    String indentation = "";
                    // indent as many times as we go deep in the tree
                    WorkFlow container = this.descriptor.getContainer();
                    while (container != null) {
                        indentation += "   ";
                        container = container.descriptor.getContainer();
                    }
                    log.print(indentation + "Running " + blockLog);

                    if (blockDescriptor.inputVars.size() > 0) {
                        log.println(" with the following parameters:");
                        for (Var<?> var : blockDescriptor.inputVars) {
                            if (!blockDescriptor.inputVars.isVisible(var))
                                continue;

                            log.print(indentation + "- " + var.getName() + ": " + var.getValueAsString(true));

                            // If this variable points to another one, say so
                            Var<?> reference = var.getReference();
                            if (reference != null) {
                                // The owner of the reference is either...

                                // 1) an output variable...
                                BlockDescriptor owner = getOutputOwner(reference);
                                // 2) a pass-through input...
                                if (owner == null)
                                    owner = getInputOwner(reference);
                                // 3) a variable outside the work flow
                                if (owner == null) {
                                    log.println(" (from variable \"" + reference.getName()
                                            + "\" defined outside this workflow)");
                                }
                                else if (owner == this.descriptor) {
                                    log.println(" (from local loop or batch variable \"" + reference.getName() + "\")");
                                }
                                else {
                                    log.println(" (from variable \"" + reference.getName() + "\" of block #"
                                            + (indexOf(owner) + 1) + " [" + owner.getDefinedName() + "])");
                                }
                            }
                            else
                                log.println();
                        }
                    }

                    // THIS IS WHERE IT HAPPENS
                    long tic = System.nanoTime();
                    blockDescriptor.run();
                    long tac = System.nanoTime();

                    String time = UnitUtil.displayTimeAsStringWithUnits((tac - tic) / 1000000f, false);
                    log.println(indentation + "Finished " + blockLog + " in " + (time.isEmpty() ? "0 ms" : time));

                    runningBlock = null;

                    if (blockDescriptor.isFinalBlock()) {
                        blockDescriptor.setFinalBlock(false);
                        interrupt();
                    }
                }
            }

            if (descriptor.getContainer() != null)
                log.println();

            finalStatus = "The workflow executed successfully";
        }
        catch (StopException e) {
            // push the exception upstairs
            if (descriptor.getContainer() != null)
                throw e;

            finalStatus = "The workflow was interrupted";
        }
        catch (RuntimeException e) {
            finalStatus = "The workflow did not execute properly";

            boolean catchException = false;

            if (e instanceof IcyHandledException || e instanceof VarException) {
                catchException = true;
            }
            else if (e instanceof BlocksException) {
                catchException = ((BlocksException) e).catchException;
            }

            if (catchException && runningBlock != null) {
                String blockName = runningBlock.getDefinedName();
                int blockID = indexOf(runningBlock) + 1;
                throw new IcyHandledException(
                        "While running block \"" + blockName + "\" (" + blockID + "):\n" + e.getMessage());
            }

            // it's probably a real problem, re-throw
            throw e;
        }
        finally {
            descriptor.setStatus(BlockStatus.READY);
            endTime = System.nanoTime();
            double time = endTime - startTime;
            finalStatus += " (total running time: " + StringUtil.toString(time / 1e9, 2) + " seconds)";
            statusChanged(this, finalStatus);
        }
    }

    public void setLocation(BlockDescriptor blockInfo, Point point) {
        blockInfo.setLocation(point.x, point.y);
    }

    /**
     * @return the number of blocks in this work flow
     */
    public int size() {
        return orderedBlocks.size();
    }

    @Override
    public void blockStatusChanged(BlockDescriptor blockInfo, BlockStatus status) {
        if (status == BlockStatus.DIRTY) {
            // "dirty-fy" following blocks
            for (Link<?> link : links)
                if (link.srcBlock == blockInfo)
                    link.dstBlock.setStatus(BlockStatus.DIRTY);
        }
    }

    @Override
    public void blockVariableAdded(BlockDescriptor block, Var<?> variable) {

    }

    @Override
    public <T> void blockVariableChanged(BlockDescriptor block, Var<T> variable, T newValue) {
        blockVariableChanged(this, block, variable, newValue);
    }

    @Override
    public void blockCollapsed(BlockDescriptor block, boolean collapsed) {

    }

    @Override
    public void blockDimensionChanged(BlockDescriptor block, int newWidth, int newHeight) {
        blockDimensionChanged(this, block, newWidth, newHeight);
    }

    @Override
    public void blockLocationChanged(BlockDescriptor block, int newX, int newY) {
        blockLocationChanged(this, block, newX, newY);
    }

    @Override
    public void blockAdded(WorkFlow source, BlockDescriptor addedBlock) {
        for (WorkFlowListener listener : listeners)
            listener.blockAdded(source, addedBlock);
    }

    @Override
    public void blockRemoved(WorkFlow source, BlockDescriptor removedBlock) {
        for (WorkFlowListener listener : listeners)
            listener.blockRemoved(source, removedBlock);
    }

    @Override
    public void linkAdded(WorkFlow source, Link<?> addedLink) {
        for (WorkFlowListener listener : listeners)
            listener.linkAdded(source, addedLink);
    }

    @Override
    public void linkRemoved(WorkFlow source, Link<?> removedLink) {
        for (WorkFlowListener listener : listeners)
            listener.linkRemoved(source, removedLink);
    }

    @Override
    public void workFlowReordered(WorkFlow source) {
        for (WorkFlowListener listener : listeners)
            listener.workFlowReordered(source);
    }

    @Override
    public void blockCollapsed(WorkFlow source, BlockDescriptor block, boolean collapsed) {
        for (WorkFlowListener listener : listeners)
            listener.blockCollapsed(source, block, collapsed);
    }

    @Override
    public void blockDimensionChanged(WorkFlow source, BlockDescriptor block, int newWidth, int newHeight) {
        for (WorkFlowListener listener : listeners)
            listener.blockDimensionChanged(this, block, newWidth, newHeight);
    }

    @Override
    public void blockLocationChanged(WorkFlow source, BlockDescriptor block, int newX, int newY) {
        for (WorkFlowListener listener : listeners)
            listener.blockLocationChanged(this, block, newX, newY);
    }

    @Override
    public void blockStatusChanged(WorkFlow source, BlockDescriptor block, BlockStatus status) {
        // no need to propagate this event
    }

    @Override
    public void blockVariableAdded(WorkFlow source, BlockDescriptor block, Var<?> variable) {
        for (WorkFlowListener listener : listeners)
            listener.blockVariableAdded(this, block, variable);
    }

    @Override
    public <T> void blockVariableChanged(WorkFlow source, BlockDescriptor block, Var<T> variable, T newValue) {
        for (WorkFlowListener listener : listeners)
            listener.blockVariableChanged(this, block, variable, newValue);
    }

    @Override
    public void statusChanged(WorkFlow source, String message) {
        for (WorkFlowListener listener : listeners)
            listener.statusChanged(source, message);
    }

    public void newSelection() {
        blockSelection = new ArrayList<>();
        linkSelection = new ArrayList<>();
    }

    public boolean isBlockSelected(BlockDescriptor bd) {
        return blockSelection.contains(bd);
    }

    public boolean isLinkSelected(Link<?> l) {
        return linkSelection.contains(l);
    }

    public void selectBlock(BlockDescriptor bd) {
        if (!isBlockSelected(bd))
            blockSelection.add(bd);
    }

    public void selectLink(Link<?> l) {
        if (!isLinkSelected(l))
            linkSelection.add(l);
    }

    public void unselectBlock(BlockDescriptor bd) {
        if (isBlockSelected(bd))
            blockSelection.remove(bd);
    }

    public void unselectLink(Link<?> l) {
        if (isLinkSelected(l))
            linkSelection.remove(l);
    }

    public ArrayList<BlockDescriptor> getBlockSelection() {
        return blockSelection;
    }

    public ArrayList<Link<?>> getLinkSelection() {
        return linkSelection;
    }
}
