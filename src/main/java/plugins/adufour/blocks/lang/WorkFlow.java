/*
 * Copyright (c) 2010-2026. Institut Pasteur.
 *
 * This file is part of Icy.
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <https://www.gnu.org/licenses/>.
 */

package plugins.adufour.blocks.lang;

import fr.icy.Icy;
import fr.icy.common.math.UnitUtil;
import fr.icy.common.string.StringUtil;
import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.gui.dialog.ConfirmDialog;
import fr.icy.system.IcyHandledException;
import fr.icy.system.thread.ThreadUtil;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.tools.input.InputBlock;
import plugins.adufour.blocks.tools.output.OutputBlock;
import plugins.adufour.blocks.util.*;
import plugins.adufour.protocols.gui.MainFrame;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarObject;
import plugins.adufour.vars.util.VarException;

import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class WorkFlow extends Plugin implements Block, Iterable<BlockDescriptor>, BlockListener, WorkFlowListener {
    private final BlockDescriptor descriptor = new BlockDescriptor(-1, this);

    private final HashSet<WorkFlowListener> listeners = new HashSet<>();

    private final ArrayList<BlockDescriptor> orderedBlocks = new ArrayList<>();

    private final boolean topLevel;

    private final ArrayList<Link<?>> links = new ArrayList<>();

    private final VarListListener inputVarListener = new VarListListener() {
        @Override
        public void variableAdded(final VarList list, final Var<?> variable) {
            descriptor.addInput(getInputVarID(variable), variable);
        }

        @Override
        public void variableRemoved(final VarList list, final Var<?> variable) {
            descriptor.removeInput(variable);
        }
    };

    private final VarListListener outputVarListener = new VarListListener() {
        @Override
        public void variableAdded(final VarList list, final Var<?> variable) {
            descriptor.addOutput(getOutputVarID(variable), variable);
        }

        @Override
        public void variableRemoved(final VarList list, final Var<?> variable) {
            descriptor.removeOutput(variable);
        }
    };

    private Thread executionThread;

    private boolean userInterruption = false;

    private ArrayList<BlockDescriptor> blockSelection = new ArrayList<>();
    private ArrayList<Link<?>> linkSelection = new ArrayList<>();

    private static final OutputStream nullStream = new OutputStream() {
        @Override
        public void write(final int b) {
        }
    };

    /**
     * The stream where the execution log is sent.
     */
    private PrintStream logStream = new PrintStream(nullStream);

    public void setLogStream(final PrintStream logStream) {
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

    public WorkFlow(final boolean topLevel) {
        super();

        this.topLevel = topLevel;
    }

    public void addBlock(final @NotNull BlockDescriptor blockInfo) {
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
            for (final Var<?> inputVar : blockInfo.inputVars) {
                // FIX: avoid to duplicate variable (Stephane)
                if (!descriptor.inputVars.contains(inputVar))
                    descriptor.addInput(getInputVarID(inputVar), inputVar);
            }
            // Retrieve output variables
            for (final Var<?> outputVar : blockInfo.outputVars) {
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
    public <T> Link<T> addLink(final BlockDescriptor srcBlock, final String srcArgID, final BlockDescriptor dstBlock, final String dstArgID) throws NoSuchBlockException, NoSuchVariableException, ClassCastException {
        if (!orderedBlocks.contains(srcBlock))
            throw new NoSuchBlockException(srcBlock);
        if (!orderedBlocks.contains(dstBlock))
            throw new NoSuchBlockException(dstBlock);

        final Var<T> srcVar = srcBlock.outputVars.get(srcArgID);
        if (srcVar == null)
            throw new NoSuchVariableException(srcBlock, srcArgID);

        final Var<T> dstVar = dstBlock.inputVars.get(dstArgID);
        if (dstVar == null)
            throw new NoSuchVariableException(dstBlock, dstArgID);

        return addLink(srcBlock, srcVar, dstBlock, dstVar);
    }

    /**
     * Creates a link between the given variables and re-schedules the blocks accordingly
     *
     * @param <T>      Value type of the link that is generated.
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
    public <T> Link<T> addLink(final BlockDescriptor srcBlock, final Var<T> srcVar, final BlockDescriptor dstBlock, final Var<T> dstVar) throws LoopException, ClassCastException {
        final Link<T> link = checkLink(srcBlock, srcVar, dstBlock, dstVar);

        dstVar.setReference(srcVar);

        // re-order the boxes to make sure srcBlock runs before dstBlock
        if (orderedBlocks.indexOf(link.srcBlock) > orderedBlocks.indexOf(link.dstBlock))
            reOrder(link.srcBlock, link.dstBlock);

        links.add(link);

        for (final WorkFlowListener listener : listeners) {
            listener.linkAdded(this, link);
        }

        return link;
    }

    public void addListener(final WorkFlowListener listener) {
        listeners.add(listener);
    }

    public boolean isTopLevel() {
        return topLevel;
    }

    public boolean contains(final BlockDescriptor dstBlock) {
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
    public <T> Link<T> checkLink(final BlockDescriptor srcBlock, final Var<T> srcVar, final BlockDescriptor dstBlock, final Var<T> dstVar) throws ClassCastException {
        final Link<T> link = new Link<>(this, srcBlock, srcVar, dstBlock, dstVar);

        checkScope(link);

        checkLoop(link);

        checkType(link);

        return link;
    }

    protected <T> void checkScope(final @NotNull Link<T> link) throws ScopeException {
        if (contains(link.srcBlock) != contains(link.dstBlock))
            throw new ScopeException();

        // special case: inner loop variables should not link to outside the loop
        if (link.srcBlock.isLoop() && ((Loop) link.srcBlock.getBlock()).isLoopVariable(link.srcVar))
            throw new ScopeException();
    }

    protected <T> void checkLoop(final @NotNull Link<T> link) throws LoopException {
        if (link.srcBlock.equals(link.dstBlock) || depends(link.srcBlock, link.dstBlock))
            throw new LoopException();
    }

    protected <T> void checkType(final @NotNull Link<T> link) throws ClassCastException {
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
    public void declareInput(final VarList inputMap) {
    }

    @Override
    public void declareOutput(final VarList outputMap) {
    }

    private boolean depends(final BlockDescriptor srcBlock, final BlockDescriptor dstBlock) {
        boolean srcDependsOnDst = false;

        for (final Link<?> link : links) {
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
     * @return all blocks contained in the current workflow
     */
    public List<BlockDescriptor> getBlockDescriptors(final boolean recursive) {
        final List<BlockDescriptor> result = new ArrayList<>();

        synchronized (orderedBlocks) {
            for (final BlockDescriptor block : orderedBlocks) {
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
    public List<BlockDescriptor> getInputBlockDescriptors(final boolean recursive) {
        final List<BlockDescriptor> result = new ArrayList<>();

        for (final BlockDescriptor block : getBlockDescriptors(recursive))
            if (block.isInput())
                result.add(block);

        return result;
    }

    /**
     * @param recursive if we also want blocks from sub workflow
     * @return all Output type blocks ({@link OutputBlock} contained in current workflow
     */
    public List<BlockDescriptor> getOutputBlockDescriptors(final boolean recursive) {
        final List<BlockDescriptor> result = new ArrayList<>();

        for (final BlockDescriptor block : getBlockDescriptors(recursive))
            if (block.isOutput())
                result.add(block);

        return result;
    }

    public BlockDescriptor getBlock(final int index) {
        return orderedBlocks.get(index);
    }

    public BlockDescriptor getBlockByID(final int blockID) throws NoSuchBlockException {
        if (blockID == descriptor.getID())
            return descriptor;

        for (final BlockDescriptor bd : orderedBlocks)
            if (bd.getID() == blockID)
                return bd;

        throw new NoSuchBlockException(blockID, this);
    }

    /**
     * @param variable The input variable to retrieve.
     * @return A unique ID for the variable within this work flow (used for XML loading/saving).
     */
    public String getInputVarID(final Var<?> variable) {
        final BlockDescriptor owner = getInputOwner(variable);

        return owner.getID() + ":" + owner.getVarID(variable);
    }

    /**
     * @param variable The input variable to retrieve.
     * @return A unique ID for the variable within this work flow (used for XML loading/saving).
     */
    public String getOutputVarID(final Var<?> variable) {
        final BlockDescriptor owner = getOutputOwner(variable);

        return owner.getID() + ":" + owner.getVarID(variable);
    }

    public BlockDescriptor getBlockDescriptor() {
        return descriptor;
    }

    public BlockDescriptor getInputOwner(final Var<?> var) {
        for (final BlockDescriptor blockInfo : orderedBlocks)
            for (final Var<?> inputVar : blockInfo.inputVars)
                if (inputVar.equals(var))
                    return blockInfo;

        return null;
    }

    public BlockDescriptor getOutputOwner(final Var<?> var) {
        for (final BlockDescriptor blockInfo : orderedBlocks)
            for (final Var<?> outputVar : blockInfo.outputVars)
                if (outputVar.equals(var))
                    return blockInfo;

        return null;
    }

    public Iterable<Link<?>> getLinksIterator() {
        return links;
    }

    public int indexOf(final BlockDescriptor blockInfo) {
        if (blockInfo == null)
            return -1;

        return orderedBlocks.indexOf(blockInfo);
    }

    @Override
    public @NotNull Iterator<BlockDescriptor> iterator() {
        // return orderedBlocks.iterator();
        return new Iterator<>() {
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
     * index in the list (i.e., after its direct dependencies).
     * This method reorganizes the block execution order.
     *
     * @param blockDesc Descriptor of the block to be prioritized.
     */
    public void prioritize(final BlockDescriptor blockDesc) {
        // retrieve the current order
        final int currentOrder = indexOf(blockDesc);
        // assume we can give the block maximum priority
        int targetOrder = 0;

        // find its dependencies
        final HashSet<BlockDescriptor> dependencies = new HashSet<>();

        for (final Link<?> link : links)
            if (link.dstBlock == blockDesc && contains(link.srcBlock))
                dependencies.add(link.srcBlock);

        // give priority to the dependencies first
        for (final BlockDescriptor dependency : dependencies)
            prioritize(dependency);

        // calculate the final order
        for (final BlockDescriptor dependency : dependencies) {
            final int order = indexOf(dependency) + 1;
            if (order > targetOrder)
                targetOrder = order;
        }

        // assign the final order

        if (targetOrder != currentOrder) {
            orderedBlocks.add(targetOrder, orderedBlocks.remove(currentOrder));
            for (final WorkFlowListener listener : listeners)
                listener.workFlowReordered(this);
        }
    }

    /**
     * Re-orders the specified blocks to ensure that prevBlock will execute before nextBlock.<br>
     * NOTE: this method assumes that the specified blocks belong to the current workflow.
     *
     * @param prevBlock The block that should be executed right before this instance.
     * @param nextBlock The block that should be executed right after this instance.
     */
    private void reOrder(final BlockDescriptor prevBlock, final BlockDescriptor nextBlock) {
        orderedBlocks.remove(prevBlock);
        orderedBlocks.add(orderedBlocks.indexOf(nextBlock), prevBlock);

        for (final Link<?> link : links)
            if (link.dstBlock == prevBlock && orderedBlocks.indexOf(link.srcBlock) > orderedBlocks.indexOf(prevBlock)) {
                reOrder(link.srcBlock, prevBlock);
                break; // only do it once, even if multiple links exist
            }

        for (final WorkFlowListener listener : listeners)
            listener.workFlowReordered(this);
    }

    public void removeBlock(final @NotNull BlockDescriptor blockInfo, boolean checkSelection) {
        // pops out workflows' selections
        if (blockInfo.getBlock() instanceof final WorkFlow wf) {
            if (checkSelection && !wf.getBlockSelection().isEmpty())
                try {
                    // use location in the main container instead of in the inner workflow
                    // avoids jumping toward the top left
                    for (final BlockDescriptor bd : wf) {
                        final Point contLoc = bd.getContainer().getBlockDescriptor().getLocation();
                        final Point loc = bd.getLocation();
                        bd.setLocation(contLoc.x + loc.x, contLoc.y + loc.y);
                    }
                    MainFrame.copySelection(wf, true);
                    MainFrame.pasteSelection(this, true);
                }
                catch (final LinkCutException e) {
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
            final Link<?> link = links.get(i);

            if (blockInfo == link.srcBlock) {
                link.dstVar.setReference(null);
                links.remove(i--);

                for (final WorkFlowListener listener : listeners)
                    listener.linkRemoved(this, link);
            }
        }

        // 1.b) links from elsewhere to this block
        for (int i = 0; i < links.size(); i++) {
            final Link<?> link = links.get(i);

            if (blockInfo == link.dstBlock) {
                link.dstVar.setReference(null);
                links.remove(i--);

                for (final WorkFlowListener listener : listeners)
                    listener.linkRemoved(this, link);
            }
        }

        // 2) remove the block variables (this removes their exposure as well)

        // a work flow does not have variables of its own => nothing to remove
        if (!blockInfo.isWorkFlow()) {
            // Remove input variables
            for (final Var<?> inputVar : blockInfo.inputVars)
                this.descriptor.removeInput(inputVar);

            // Remove output variables
            for (final Var<?> outputVar : blockInfo.outputVars)
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

        for (final WorkFlowListener listener : listeners) {
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
    public boolean isLinked(final Var<?> var) {
        for (final Link<?> link : links)
            if (link.dstVar == var || link.srcVar == var)
                return true;
        return false;
    }

    /**
     * Removes the link to the specified variable, and returns false if this variable wasn't linked
     *
     * @param dstVar the destination variable of the link to remove
     */
    public void removeLink(final Var<?> dstVar) {
        for (final Link<?> link : links)
            if (link.dstVar == dstVar && links.remove(link)) {
                link.dstVar.setReference(null);

                for (final WorkFlowListener listener : listeners)
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

    public void removeListener(final WorkFlowListener listener) {
        listeners.remove(listener);
    }

    /**
     * Resets all output variables of each block in this work flow to their default value
     */
    public void reset() {
        for (final BlockDescriptor block : orderedBlocks)
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
    public void runWorkFlow(final boolean waitForCompletion) {
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
    @Override
    public void run() {
        if (orderedBlocks.isEmpty())
            return;

        descriptor.setStatus(BlockStatus.RUNNING);

        BlockDescriptor runningBlock = null;

        String finalStatus = "";
        userInterruption = false;

        long startTime = 0, endTime = 0;
        try {
            startTime = System.nanoTime();

            final String statusPrefix = getBlockDescriptor().getContainer() == null ? ""
                    : "\"" + getBlockDescriptor().getContainer().descriptor.getName() + "\"" + " => ";

            // get the log stream recursively to ensure it is always the top-level one
            final PrintStream log = getLogStream();

            for (int blockIndex = 0; blockIndex < orderedBlocks.size(); blockIndex++) {
                if (isInterrupted())
                    throw new StopException();

                final BlockDescriptor blockDescriptor = orderedBlocks.get(blockIndex);

                if (blockDescriptor.getStatus() != BlockStatus.READY) {
                    runningBlock = blockDescriptor;

                    final String blockLog = "block #" + (blockIndex + 1) + " [" + blockDescriptor.getDefinedName() + "]";

                    // Adjust status
                    statusChanged(this, "Running block " + statusPrefix + "\"" + blockDescriptor.getDefinedName()
                            + "\" (#" + (blockIndex + 1) + ")...");

                    // Log should be indented
                    final StringBuilder indentation = new StringBuilder();
                    // indent as many times as we go deep in the tree
                    WorkFlow container = this.descriptor.getContainer();
                    while (container != null) {
                        indentation.append("   ");
                        container = container.descriptor.getContainer();
                    }
                    log.print(indentation + "Running " + blockLog);

                    if (blockDescriptor.inputVars.size() > 0) {
                        log.println(" with the following parameters:");
                        for (final Var<?> var : blockDescriptor.inputVars) {
                            if (!blockDescriptor.inputVars.isVisible(var))
                                continue;

                            log.print(indentation + "- " + var.getName() + ": " + var.getValueAsString(true));

                            // If this variable points to another one, say so
                            final Var<?> reference = var.getReference();
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
                    final long tic = System.nanoTime();
                    blockDescriptor.run();
                    final long tac = System.nanoTime();

                    final String time = UnitUtil.displayTimeAsStringWithUnits((tac - tic) / 1000000f, false);
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
        catch (final StopException e) {
            // push the exception upstairs
            if (descriptor.getContainer() != null)
                throw e;

            finalStatus = "The workflow was interrupted";
        }
        catch (final RuntimeException e) {
            finalStatus = "The workflow did not execute properly";

            boolean catchException = false;

            if (e instanceof IcyHandledException || e instanceof VarException) {
                catchException = true;
            }
            else if (e instanceof BlocksException) {
                catchException = ((BlocksException) e).catchException;
            }

            if (catchException && runningBlock != null) {
                final String blockName = runningBlock.getDefinedName();
                final int blockID = indexOf(runningBlock) + 1;
                throw new IcyHandledException(
                        "While running block \"" + blockName + "\" (" + blockID + "):\n" + e.getMessage());
            }

            // it's probably a real problem, re-throw
            throw e;
        }
        finally {
            descriptor.setStatus(BlockStatus.READY);
            endTime = System.nanoTime();
            final double time = endTime - startTime;
            finalStatus += " (total running time: " + StringUtil.toString(time / 1e9, 2) + " seconds)";
            statusChanged(this, finalStatus);
        }
    }

    public void setLocation(final @NotNull BlockDescriptor blockInfo, final @NotNull Point point) {
        blockInfo.setLocation(point.x, point.y);
    }

    /**
     * @return the number of blocks in this work flow
     */
    public int size() {
        return orderedBlocks.size();
    }

    @Override
    public void blockStatusChanged(final BlockDescriptor blockInfo, final BlockStatus status) {
        if (status == BlockStatus.DIRTY) {
            // "dirty-fy" following blocks
            for (final Link<?> link : links)
                if (link.srcBlock == blockInfo)
                    link.dstBlock.setStatus(BlockStatus.DIRTY);
        }
    }

    @Override
    public void blockVariableAdded(final BlockDescriptor block, final Var<?> variable) {

    }

    @Override
    public <T> void blockVariableChanged(final BlockDescriptor block, final Var<T> variable, final T newValue) {
        blockVariableChanged(this, block, variable, newValue);
    }

    @Override
    public void blockCollapsed(final BlockDescriptor block, final boolean collapsed) {

    }

    @Override
    public void blockDimensionChanged(final BlockDescriptor block, final int newWidth, final int newHeight) {
        blockDimensionChanged(this, block, newWidth, newHeight);
    }

    @Override
    public void blockLocationChanged(final BlockDescriptor block, final int newX, final int newY) {
        blockLocationChanged(this, block, newX, newY);
    }

    @Override
    public void blockAdded(final WorkFlow source, final BlockDescriptor addedBlock) {
        for (final WorkFlowListener listener : listeners)
            listener.blockAdded(source, addedBlock);
    }

    @Override
    public void blockRemoved(final WorkFlow source, final BlockDescriptor removedBlock) {
        for (final WorkFlowListener listener : listeners)
            listener.blockRemoved(source, removedBlock);
    }

    @Override
    public void linkAdded(final WorkFlow source, final Link<?> addedLink) {
        for (final WorkFlowListener listener : listeners)
            listener.linkAdded(source, addedLink);
    }

    @Override
    public void linkRemoved(final WorkFlow source, final Link<?> removedLink) {
        for (final WorkFlowListener listener : listeners)
            listener.linkRemoved(source, removedLink);
    }

    @Override
    public void workFlowReordered(final WorkFlow source) {
        for (final WorkFlowListener listener : listeners)
            listener.workFlowReordered(source);
    }

    @Override
    public void blockCollapsed(final WorkFlow source, final BlockDescriptor block, final boolean collapsed) {
        for (final WorkFlowListener listener : listeners)
            listener.blockCollapsed(source, block, collapsed);
    }

    @Override
    public void blockDimensionChanged(final WorkFlow source, final BlockDescriptor block, final int newWidth, final int newHeight) {
        for (final WorkFlowListener listener : listeners)
            listener.blockDimensionChanged(this, block, newWidth, newHeight);
    }

    @Override
    public void blockLocationChanged(final WorkFlow source, final BlockDescriptor block, final int newX, final int newY) {
        for (final WorkFlowListener listener : listeners)
            listener.blockLocationChanged(this, block, newX, newY);
    }

    @Override
    public void blockStatusChanged(final WorkFlow source, final BlockDescriptor block, final BlockStatus status) {
        // no need to propagate this event
    }

    @Override
    public void blockVariableAdded(final WorkFlow source, final BlockDescriptor block, final Var<?> variable) {
        for (final WorkFlowListener listener : listeners)
            listener.blockVariableAdded(this, block, variable);
    }

    @Override
    public <T> void blockVariableChanged(final WorkFlow source, final BlockDescriptor block, final Var<T> variable, final T newValue) {
        for (final WorkFlowListener listener : listeners)
            listener.blockVariableChanged(this, block, variable, newValue);
    }

    @Override
    public void statusChanged(final WorkFlow source, final String message) {
        for (final WorkFlowListener listener : listeners)
            listener.statusChanged(source, message);
    }

    public void newSelection() {
        blockSelection = new ArrayList<>();
        linkSelection = new ArrayList<>();
    }

    public boolean isBlockSelected(final BlockDescriptor bd) {
        return blockSelection.contains(bd);
    }

    public boolean isLinkSelected(final Link<?> l) {
        return linkSelection.contains(l);
    }

    public void selectBlock(final BlockDescriptor bd) {
        if (!isBlockSelected(bd))
            blockSelection.add(bd);
    }

    public void selectLink(final Link<?> l) {
        if (!isLinkSelected(l))
            linkSelection.add(l);
    }

    public void unselectBlock(final BlockDescriptor bd) {
        if (isBlockSelected(bd))
            blockSelection.remove(bd);
    }

    public void unselectLink(final Link<?> l) {
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
