/*
 * Copyright (c) 2010-2023. Institut Pasteur.
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

import icy.file.xml.XMLPersistent;
import icy.gui.plugin.PluginErrorReport;
import icy.network.NetworkUtil;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginInstaller;
import icy.plugin.PluginRepositoryLoader;
import icy.plugin.abstract_.Plugin;
import icy.util.ClassUtil;
import icy.util.StringUtil;
import icy.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import plugins.adufour.blocks.tools.Display;
import plugins.adufour.blocks.tools.input.InputBlock;
import plugins.adufour.blocks.tools.output.OutputBlock;
import plugins.adufour.blocks.util.*;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarMutableArray;
import plugins.adufour.vars.util.MutableType;
import plugins.adufour.vars.util.VarListener;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

/**
 * Class defining all the metadata associated to a {@link Block}.
 *
 * @author Alexandre Dufour
 */
@SuppressWarnings("rawtypes")
public class BlockDescriptor implements Runnable, VarListener, VarListListener, XMLPersistent {
    public enum BlockStatus {
        /**
         * The result is not up-to-date. This status is active at startup and whenever a variable is
         * changed
         */
        DIRTY("This block is ready to run"),
        /**
         * The block is currently running
         */
        RUNNING("This block is currently running..."),
        /**
         * The result is up-to-date and need not be recomputed (unless explicitly required)
         */
        READY("This block is up to date"),
        /**
         * There was an error while running this block
         */
        ERROR("This block did not run properly");

        public final String defaultErrorMessage;

        private String optionalUserMessage = "";

        BlockStatus(final String message) {
            defaultErrorMessage = message;
        }

        public String getUserMessage() {
            return optionalUserMessage;
        }

        public void setUserMessage(final String message) {
            optionalUserMessage = (message != null ? message : "");
        }

        @Override
        public String toString() {
            return defaultErrorMessage + (optionalUserMessage.isEmpty() ? "" : ":\n" + optionalUserMessage);
        }
    }

    private Integer id = hashCode();

    private Block block;

    private WorkFlow container;

    private final HashSet<BlockListener> listeners = new HashSet<>();

    /**
     * The input variables of this descriptor's block
     */
    public final VarList inputVars;

    /**
     * The output variables of this descriptor's block
     */
    public final VarList outputVars;

    private final Point location = new Point();

    private final Dimension dimension = new Dimension(0, 0);

    private boolean collapsed = false;

    private BlockStatus status = BlockStatus.DIRTY;

    private boolean finalBlock;

    private String definedName = null;

    private boolean keepResults = true;

    /**
     * Command-line ID (used only for input blocks)
     */
    private String commandLineID = "";

    public BlockDescriptor() {
        this.inputVars = new VarList();
        this.inputVars.addVarListListener(this);
        this.outputVars = new VarList();
        this.outputVars.addVarListListener(this);
    }

    /**
     * @param ID    Identifier
     * @param block Block template instance
     */
    public BlockDescriptor(final int ID, final Block block) {
        this();

        this.block = block;

        try {
            block.declareInput(inputVars);

            block.declareOutput(outputVars);
        }
        catch (final RuntimeException e) {
            final String blockName = block.getClass().getName();

            String devId = blockName.substring("plugins.".length());
            devId = devId.substring(0, devId.indexOf('.'));

            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            pw.write("Unable to insert block: " + block.getClass().getName() + "\n");
            pw.write("Reason: " + e.getClass().getName() + ": " + e.getMessage() + "\n");
            pw.write("Stack trace:\n");
            e.printStackTrace(pw);

            PluginErrorReport.report(null, devId, sw.toString());
        }

        if (ID != -1)
            id = ID;
    }

    /**
     * Adds a new input variable to this block <u>after</u> it has been initialized. This method is
     * called when the block is embedded inside another work flow, such that the block variables can
     * be later exposed (and linked) outside the work flow.
     * <p>
     * NB: default (startup) block variables are not added here, they are automatically created in
     * {@link #BlockDescriptor(int, Block)} via {@link Block#declareInput(VarList)} and
     * {@link Block#declareOutput(VarList)}.
     *
     * @param uid      a unique identifier for the variable
     * @param variable the variable to add
     * @throws IllegalArgumentException if a variable with same unique ID exists
     */
    public void addInput(final String uid, final Var<?> variable) throws IllegalArgumentException {
        inputVars.add(uid, variable);
    }

    /**
     * Adds a new input variable to this block <u>after</u> it has been initialized. This method is
     * called when the block is embedded inside another work flow, such that the block variables can
     * be later exposed (and linked) outside the work flow.
     * <p>
     * NB: default (startup) block variables are not added here, they are automatically created in
     * {@link #BlockDescriptor(int, Block)} via {@link Block#declareInput(VarList)} and
     * {@link Block#declareOutput(VarList)}.
     *
     * @param uid      a unique identifier for the variable
     * @param variable the variable to add
     * @throws IllegalArgumentException if a variable with same unique ID exists
     */
    public void addOutput(final String uid, final Var<?> variable) throws IllegalArgumentException {
        outputVars.add(uid, variable);
    }

    public void addBlockListener(final BlockListener listener) {
        listeners.add(listener);
    }

    public void addBlockPanelListener(final BlockListener listener) {
        listeners.add(listener);
    }

    public void removeBlockListener(final BlockListener listener) {
        listeners.remove(listener);
    }

    public void removeBlockPanelListener(final BlockListener listener) {
        listeners.remove(listener);
    }

    public Block getBlock() {
        return block;
    }

    public WorkFlow getContainer() {
        return container;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public Integer getID() {
        return id;
    }

    /**
     * @return the location of the block in the work flow (use only in graphical mode)
     */
    public Point getLocation() {
        return location;
    }

    /**
     * @return The name of the block
     */
    public String getName() {
        String blockName = block.getClass().getSimpleName();

        if (block instanceof Plugin && ((Plugin) block).getDescriptor() != null) {
            final String pluginName = ((Plugin) block).getDescriptor().getName();

            if (!pluginName.equalsIgnoreCase(blockName))
                return pluginName;
        }

        if (blockName.endsWith("block")) {
            blockName = blockName.substring(0, blockName.lastIndexOf("block"));
        }
        else if (blockName.endsWith("Block")) {
            blockName = blockName.substring(0, blockName.lastIndexOf("Block"));
        }

        return BlocksFinder.getFlattened(blockName);
    }

    public BlockStatus getStatus() {
        return status;
    }

    /**
     * Retrieves the unique ID of this variable. This method first searches in the list of input
     * variables, and then in the list of output variables if necessary.
     *
     * @param variable The variable to retrieve.
     * @return Identifier of the given variable.
     * @throws NoSuchVariableException If the variable cannot be found in this block's input or output.
     */
    public String getVarID(final Var<?> variable) throws NoSuchVariableException {
        if (inputVars.contains(variable)) {
            final String varID = inputVars.getID(variable);

            if (!isWorkFlow() || varID.contains(":"))
                return varID;

            return ((WorkFlow) block).getInputVarID(variable);
        }

        final String varID = outputVars.getID(variable);

        if (!isWorkFlow() || varID.contains(":"))
            return varID;

        return ((WorkFlow) block).getOutputVarID(variable);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * @return true if this block is the last block to execute in the work flow. If true, then the
     * execution of the top-level work flow will stop after execution of this block.
     */
    public boolean isFinalBlock() {
        return finalBlock;
    }

    public boolean isLoop() {
        return block instanceof Loop;
    }

    public boolean isInput() {
        return block instanceof InputBlock;
    }

    public boolean isOutput() {
        return block instanceof OutputBlock;
    }

    public boolean isSingleBlock() {
        return block instanceof Display || block instanceof InputBlock || block instanceof OutputBlock;
    }

    public boolean isWorkFlow() {
        return block instanceof WorkFlow;
    }

    public boolean isTopLevelWorkFlow() {
        // Stephane FIX: properly detect top level workflow
        return isWorkFlow() && ((WorkFlow) block).isTopLevel();
        // return isWorkFlow() && container == null;
    }

    public void setCollapsed(final boolean collapsed) {
        if (this.collapsed == collapsed)
            return;

        this.collapsed = collapsed;

        for (final BlockListener l : listeners)
            l.blockCollapsed(this, collapsed);
    }

    public void setContainer(final WorkFlow container) {
        this.container = container;
    }

    /**
     * Sets whether this block is the last block to execute in the work flow. If true, then the
     * execution of the top-level work flow will stop after execution of this block. Note that if
     * multiple blocks are marked as final, the execution will stop after the first block marked
     * final, in order of execution.
     *
     * @param finalBlock true to stop the work flow after running this block, false otherwise.
     */
    public void setFinalBlock(final boolean finalBlock) {
        this.finalBlock = finalBlock;
    }

    /**
     * Sets the dimension of this block.
     *
     * @param width  Width of the block in pixels.
     * @param height Height of the block in pixels.
     */
    public void setDimension(final int width, final int height) {
        if (dimension.width == width && dimension.height == height)
            return;

        dimension.setSize(width, height);

        for (final BlockListener l : listeners)
            l.blockDimensionChanged(this, width, height);
    }

    /**
     * Sets the ID of this block.
     *
     * @param id Unique block identifier.
     */
    public void setID(final int id) {
        this.id = id;
    }

    /**
     * Sets the value of the specified input variable
     *
     * @param <T>   Value type of the variable being set.
     * @param varID The unique ID of the input variable to set.
     * @param value The new variable value.
     * @throws NoSuchVariableException If the variable is not in this block.
     */
    public <T> void setInput(final String varID, final T value) throws NoSuchVariableException {
        final Var<T> input = inputVars.get(varID);
        if (input == null)
            throw new NoSuchVariableException(this, varID);

        input.setValue(value);
    }

    /**
     * Stores the location of the block and notifies listeners.
     *
     * @param x X-axis position in pixels.
     * @param y Y-axis position in pixels.
     */
    public void setLocation(final int x, final int y) {
        if (location.x == x && location.y == y)
            return;

        location.move(x, y);
        for (final BlockListener l : listeners)
            l.blockLocationChanged(this, x, y);
    }

    /**
     * Sets the value of the specified output variable
     *
     * @param <T>   Value type of the variable being set.
     * @param varID the unique ID of the output variable to set
     * @param value the new variable value
     * @throws NoSuchVariableException if the variable is not in this block
     */
    public <T> void setOutput(final String varID, final T value) throws NoSuchVariableException {
        final Var<T> output = outputVars.get(varID);
        if (output == null)
            throw new NoSuchVariableException(this, varID);

        output.setValue(value);
    }

    public void setStatus(final BlockStatus newStatus) {
        if (this.status == newStatus)
            return;

        this.status = newStatus;
        for (final BlockListener listener : listeners)
            listener.blockStatusChanged(this, status);

        if (status == BlockStatus.DIRTY && container != null) {
            // propagate dirty status to the container (if it is not running)
            final BlockDescriptor wfDescriptor = container.getBlockDescriptor();

            if (wfDescriptor.getStatus() != BlockStatus.RUNNING) {
                wfDescriptor.setStatus(BlockStatus.DIRTY);
            }
        }
    }

    /**
     * Removes the specified variable from the list of inputs
     *
     * @param inputVar Variable to be removed from the input variable list.
     */
    public void removeInput(final Var<?> inputVar) {
        if (!inputVars.contains(inputVar))
            return;

        inputVars.setVisible(inputVar, false);
        inputVars.remove(inputVar);
    }

    /**
     * Removes the specified variable from the list of outputs
     *
     * @param outputVar Variable to be removed from the output variable list.
     */
    public void removeOutput(final Var<?> outputVar) {
        if (!outputVars.contains(outputVar))
            return;

        outputVars.setVisible(outputVar, false);
        outputVars.remove(outputVar);
    }

    /**
     * Resets all output variables in this block to their default value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void reset() {
        setStatus(BlockStatus.DIRTY);

        for (final Var var : outputVars)
            var.setValue(var.getDefaultValue());

        if (isWorkFlow())
            ((WorkFlow) block).reset();
    }

    @Override
    public void run() {
        if (keepResults && status == BlockStatus.READY)
            return;

        setStatus(BlockStatus.RUNNING);

        try {
            block.run();
            setStatus(keepResults ? BlockStatus.READY : BlockStatus.DIRTY);
        }
        catch (final RuntimeException e) {
            final BlockStatus error = BlockStatus.ERROR;

            error.setUserMessage(e.getMessage());

            setStatus(error);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void valueChanged(final Var source, final Object oldValue, final Object newValue) {
        if (inputVars.contains(source))
            setStatus(BlockStatus.DIRTY);

        for (final BlockListener listener : listeners)
            listener.blockVariableChanged(this, source, newValue);
    }

    @Override
    public void referenceChanged(final Var source, final Var oldReference, final Var newReference) {
        setStatus(BlockStatus.DIRTY);
    }

    @Override
    public String toString() {
        return getContainer().getBlockDescriptor().getName() + "." + getName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void variableAdded(final VarList list, final Var<?> variable) {
        variable.addListener(this);

        for (final BlockListener listener : listeners)
            listener.blockVariableAdded(this, variable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void variableRemoved(final VarList list, final Var<?> variable) {
        variable.removeListener(this);

        // although we removed a variable, there is no "removed" notifier.
        // use the "added" notifier just for the sake of refreshing the GUI.
        for (final BlockListener listener : listeners)
            listener.blockVariableAdded(this, variable);
    }

    /**
     * Goes online, downloads and installs the plug-in containing the specified block
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Block> installRequiredBlock(final String pluginClassName, final String blockType) throws ClassNotFoundException {
        final Class<?> clazz;

        // check if the class exists, otherwise try downloading it (if online)
        try {
            clazz = ClassUtil.findClass(blockType);
        }
        catch (final ClassNotFoundException e) {
            if (!NetworkUtil.hasInternetAccess() || getClass().getClassLoader() == ClassLoader.getSystemClassLoader()) {
                throw new BlocksException(String.format(
                        "Plugin %s is missing, but no internet connection is available.\nTry again later",
                        blockType
                ), true);
            }

            // String simpleName =
            // ClassUtil.getSimpleClassName(ClassUtil.getBaseClassName(blockType));

            // status.setValue("Downloading " + simpleName + "...");

            final PluginDescriptor pd = PluginRepositoryLoader.getPlugin(pluginClassName);

            if (pd == null) {
                // status.setValue("Couldn't find plugin online !");
                throw e;
            }

            // status.setValue("Installing " + simpleName + "...");

            PluginInstaller.install(pd, false);

            throw new BlocksReloadedException();
        }

        return (Class<? extends Block>) clazz;
    }

    @Override
    public boolean loadFromXML(final Node node) {
        boolean noWarnings = true;

        final Element blockNode = (Element) node;

        final String blockType = XMLUtil.getAttributeValue(blockNode, "blockType", null);

        try {
            final Class<? extends Block> blockClass = installRequiredBlock(
                    XMLUtil.getAttributeValue(blockNode, "className", null), blockType);

            block = blockClass.getDeclaredConstructor().newInstance();

            if (block == null)
                throw new BlocksException("Couldn't create block from class " + blockClass.getName(), true);

            block.declareInput(inputVars);
            block.declareOutput(outputVars);

            setID(XMLUtil.getAttributeIntValue(blockNode, "ID", -1));

            final int width = XMLUtil.getAttributeIntValue(blockNode, "width", 500);
            final int height = XMLUtil.getAttributeIntValue(blockNode, "height", 500);
            setDimension(width, height);

            final int xPos = XMLUtil.getAttributeIntValue(blockNode, "xLocation", -1);
            final int yPos = XMLUtil.getAttributeIntValue(blockNode, "yLocation", -1);
            setLocation(xPos, yPos);

            final Element varRoot = XMLUtil.getElement(blockNode, "variables");
            final Element inVarRoot = XMLUtil.getElement(varRoot, "input");
            for (final Element varNode : XMLUtil.getElements(inVarRoot)) {
                final String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                final Var<?> var = inputVars.get(uid);

                if (var == null) {
                    if (noWarnings) {
                        System.err.println("Error(s) while loading protocol:");
                        noWarnings = false;
                    }
                    System.err.println(new NoSuchVariableException(this, uid).getMessage());
                    continue;
                }

                if (var instanceof MutableType) {
                    final String type = XMLUtil.getAttributeValue(varNode, "type", null);

                    if (type != null) {
                        if (var instanceof VarMutable) {
                            Class<?> mutableType = BlocksML.getPrimitiveType(type);

                            if (mutableType == null)
                                mutableType = Class.forName(type);

                            ((MutableType) var).setType(mutableType);
                        }
                        else if (var instanceof VarMutableArray) {
                            ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                        }
                    }
                }

                var.loadFromXML(varNode);

                inputVars.setVisible(var, XMLUtil.getAttributeBooleanValue(varNode, "visible", false));
            }

            final Element outVarRoot = XMLUtil.getElement(varRoot, "output");
            for (final Element varNode : XMLUtil.getElements(outVarRoot)) {
                final String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                final Var<?> var = outputVars.get(uid);

                if (var == null) {
                    if (noWarnings) {
                        System.err.println("Error(s) while loading protocol:");
                        noWarnings = false;
                    }
                    System.err.println(new NoSuchVariableException(this, uid).getMessage());
                    continue;
                }

                outputVars.setVisible(var, XMLUtil.getAttributeBooleanValue(varNode, "visible", false));

                if (var instanceof MutableType) {
                    final String type = XMLUtil.getAttributeValue(varNode, "type", null);
                    if (type != null) {
                        if (var instanceof VarMutable) {
                            final Class<?> mutableType = BlocksML.getPrimitiveType(type);
                            ((MutableType) var).setType(mutableType != null ? mutableType : Class.forName(type));
                        }
                        else if (var instanceof VarMutableArray) {
                            ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                        }
                    }
                }
            }

        }
        catch (final ClassNotFoundException e1) {
            throw new BlocksException("Cannot create block (" + e1.getMessage() + ") => class not found", true);
        }
        catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return noWarnings;
    }

    @Override
    public boolean saveToXML(final Node node) {
        return false;
    }

    public void setDefinedName(final String s) {
        definedName = s;
    }

    public String getDefinedName() {
        if (definedName == null || definedName.isEmpty())
            return getName();

        return definedName;
    }

    public static BlockDescriptor findBlockDescriptor(final int blockId, final List<BlockDescriptor> blocks) {
        for (final BlockDescriptor bd : blocks)
            if (bd.getID().equals(blockId))
                return bd;

        return null;
    }

    /**
     * @param commandLineId commandLineId (see {@link BlockDescriptor#getCommandLineID()}
     * @param blocks        list of {@link BlockDescriptor}
     * @return Input block {@link InputBlock} with given commandLineId or <i>null</i> if it doesn't exist
     */
    public static BlockDescriptor findInputBlockDescriptor(final String commandLineId, final List<BlockDescriptor> blocks) {
        for (final BlockDescriptor block : blocks)
            if (block.isInput() && StringUtil.equals(block.getCommandLineID(), commandLineId))
                return block;

        return null;
    }

    /**
     * @param commandLineId commandLineId (see {@link BlockDescriptor#getCommandLineID()}
     * @param blocks        list of {@link BlockDescriptor}
     * @return Output block {@link OutputBlock} with given commandLineId or <i>null</i> if it doesn't exist
     */
    public static BlockDescriptor findOutputBlockDescriptor(final String commandLineId, final List<BlockDescriptor> blocks) {
        for (final BlockDescriptor block : blocks)
            if (block.isOutput() && StringUtil.equals(block.getCommandLineID(), commandLineId))
                return block;

        return null;
    }

    private static BlockDescriptor getBlockDescriptor(final int blockId, final Set<BlockDescriptor> bds) {
        for (final BlockDescriptor bd : bds)
            if (bd.getID().equals(blockId))
                return bd;

        return null;
    }

    private String getNewVarId(final String oldVarId, final Map<BlockDescriptor, BlockDescriptor> copies) {
        // search for workflow shadow exposed variable
        final int index = oldVarId.indexOf(":");

        // direct variable id ? --> return it
        if (index == -1)
            return oldVarId;

        String result = "";

        // get block ID
        final int blockId = StringUtil.parseInt(oldVarId.substring(0, index), 0);
        // get block descriptor from id
        final BlockDescriptor bd = getBlockDescriptor(blockId, copies.keySet());
        // get the corresponding new block
        final BlockDescriptor nbd = copies.get(bd);

        if (nbd != null)
            result = nbd.getID().toString();

        // remaining var id
        return result + ":" + getNewVarId(oldVarId.substring(index + 1), copies);
    }

    public BlockDescriptor clone(final boolean embedding) {
        return clone(embedding, new HashMap<>());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public BlockDescriptor clone(final boolean embedding, final Map<BlockDescriptor, BlockDescriptor> copies) {
        final Class<? extends Block> blockClass = getBlock().getClass().asSubclass(Block.class);
        final WorkFlow wf;
        final WorkFlow wfCpy;
        final BlockDescriptor cpy;
        Block newBlock = null;

        try {
            newBlock = blockClass.getDeclaredConstructor().newInstance();
        }
        catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e1) {
            e1.printStackTrace();
        }

        cpy = (newBlock instanceof WorkFlow ? ((WorkFlow) newBlock).getBlockDescriptor()
                : new BlockDescriptor(-1, newBlock));

        cpy.setDefinedName(getDefinedName());
        cpy.setLocation(getLocation().x + 12, getLocation().y + 12);
        cpy.setDimension(getDimension().width, getDimension().height);
        cpy.setCollapsed(isCollapsed());

        // Clone input variables
        for (final Var oldVar : inputVars) {
            final String oldID = inputVars.getID(oldVar);

            // FIX: shadow exposed variable from workflow? --> don't copy it for now (Stephane)
            if (oldID.contains(":") && (getBlock() instanceof WorkFlow))
                continue;

            Var newVar = cpy.inputVars.get(oldID);
            // If newVar is null, then oldVar was a runtime variable
            if (newVar == null) {
                newVar = new VarMutable(oldVar.getName(), oldVar.getType());
                cpy.inputVars.addRuntimeVariable(oldID, (VarMutable) newVar);
            }
            else {
                if (newVar instanceof VarMutable)
                    ((VarMutable) newVar).setType(oldVar.getType());
                newVar.setValue(oldVar.getValue());
            }

            // FIX: preserve visibility information (Stephane)
            cpy.inputVars.setVisible(newVar, inputVars.isVisible(oldVar));
        }

        // Clone output variables
        for (final Var oldVar : outputVars) {
            final String oldID = outputVars.getID(oldVar);

            // FIX: shadow exposed variable from workflow? --> don't copy it for now (Stephane)
            if (oldID.contains(":") && (getBlock() instanceof WorkFlow))
                continue;

            Var newVar = cpy.outputVars.get(oldID);
            // If newVar is null, then oldVar was a runtime variable
            if (newVar == null) {
                newVar = new VarMutable(oldVar.getName(), oldVar.getType());
                cpy.outputVars.addRuntimeVariable(oldID, (VarMutable) newVar);
            }
            else {
                if (newVar instanceof VarMutable)
                    ((VarMutable) newVar).setType(oldVar.getType());
                newVar.setValue(oldVar.getValue());
            }

            // FIX: preserve visibility information (Stephane)
            cpy.outputVars.setVisible(newVar, outputVars.isVisible(oldVar));
        }

        if (getBlock() instanceof WorkFlow) {
            wf = (WorkFlow) getBlock();
            wfCpy = (WorkFlow) cpy.getBlock();

            BlockDescriptor tmp;
            if (wf.getBlockSelection().isEmpty() || embedding) {
                for (final BlockDescriptor bd : wf) {
                    tmp = bd.clone(true, copies);
                    copies.put(bd, tmp);
                    wfCpy.addBlock(tmp);
                    wfCpy.selectBlock(tmp);
                }
                cloneLinks(wf.getLinksIterator(), copies, wfCpy);
            }
            else {
                for (final BlockDescriptor bd : wf.getBlockSelection()) {
                    tmp = bd.clone(true, copies);
                    copies.put(bd, tmp);
                    wfCpy.addBlock(tmp);
                    wfCpy.selectBlock(tmp);
                }
                cloneLinks(wf.getLinkSelection(), copies, wfCpy);
            }

            for (final Var oldVar : inputVars) {
                final String oldID = inputVars.getID(oldVar);
                final int index = oldID.indexOf(":");

                // shadow exposed variable? --> try to recover it
                if (index != -1) {
                    // get block ID
                    final int blockID = StringUtil.parseInt(oldID.substring(0, index), 0);
                    // get var ID
                    final String varID = oldID.substring(index + 1);
                    // get new var ID
                    final String newVarID = getNewVarId(varID, copies);
                    // get block descriptor from ID
                    final BlockDescriptor bd = wf.getBlockByID(blockID);
                    // get the corresponding new block
                    final BlockDescriptor nbd = copies.get(bd);
                    // get the corresponding var for this block
                    final Var<Object> inputVar = nbd.inputVars.get(newVarID);

                    // finally, add the shadow exposed variable if not already done (can be done in WorkFlow.addBlock(..) method) !
                    if (inputVar != null) {
                        if (!cpy.inputVars.contains(inputVar))
                            cpy.addInput(wfCpy.getInputVarID(inputVar), inputVar);
                        // and preserve visibility (very important)
                        cpy.inputVars.setVisible(inputVar, inputVars.isVisible(oldVar));
                    }
                }
            }

            for (final Var oldVar : outputVars) {
                final String oldID = outputVars.getID(oldVar);
                final int index = oldID.indexOf(":");

                // shadow exposed variable? --> try to recover it
                if (index != -1) {
                    // get block ID
                    final int blockID = StringUtil.parseInt(oldID.substring(0, index), 0);
                    // get var ID
                    final String varID = oldID.substring(index + 1);
                    // get new var ID
                    final String newVarID = getNewVarId(varID, copies);
                    // get block descriptor from ID
                    final BlockDescriptor bd = wf.getBlockByID(blockID);
                    // get the corresponding new block
                    final BlockDescriptor nbd = copies.get(bd);
                    // get the corresponding var for this block
                    final Var<Object> outputVar = nbd.outputVars.get(newVarID);

                    // finally, add the shadow exposed variable if not already done (can be done in WorkFlow.addBlock(..) method) !
                    if (outputVar != null) {
                        if (!cpy.outputVars.contains(outputVar))
                            cpy.addOutput(wfCpy.getInputVarID(outputVar), outputVar);
                        // and preserve visibility (very important)
                        cpy.outputVars.setVisible(outputVar, outputVars.isVisible(oldVar));
                    }
                }
            }
        }

        return cpy;
    }

    private static void cloneLinks(final Iterable<Link<?>> iterable, final Map<BlockDescriptor, BlockDescriptor> copies, final WorkFlow dest) {
        for (final Link<?> l : iterable) {
            if (l.srcBlock.getBlock() instanceof Loop || l.dstBlock.getBlock() instanceof Loop) {
                System.err.println("Warning : cannot copy a link to a loop variable");
                continue;
            }
            if (l.srcBlock.getBlock() instanceof WorkFlow || l.dstBlock.getBlock() instanceof WorkFlow) {
                System.err.println("Warning : cannot copy a link to an exposed variable");
                continue;
            }
            try {
                dest.addLink(copies.get(l.srcBlock),
                        copies.get(l.srcBlock).inputVars.get(l.srcBlock.inputVars.getID(l.srcVar)),
                        copies.get(l.dstBlock),
                        copies.get(l.dstBlock).inputVars.get(l.dstBlock.inputVars.getID(l.dstVar)));
            }
            catch (final NoSuchVariableException nsve) {
                dest.addLink(copies.get(l.srcBlock),
                        copies.get(l.srcBlock).outputVars.get(l.srcBlock.outputVars.getID(l.srcVar)),
                        copies.get(l.dstBlock),
                        copies.get(l.dstBlock).inputVars.get(l.dstBlock.inputVars.getID(l.dstVar)));
            }
        }
    }

    /**
     * @return <code>true</code> if the block keeps results in memory, <code>false</code> otherwise
     */
    public boolean keepsResults() {
        return keepResults;
    }

    /**
     * Sets whether the block should keep results in memory (and not need to recalculate it unless a
     * parameter has changed)
     *
     * @param keep Flag to keep results in memory.
     */
    public void keepResults(final boolean keep) {
        this.keepResults = keep;
    }

    /**
     * (This method is used only if the block implements {@link InputBlock} or {@link OutputBlock})
     *
     * @return the command-line identifier for the input/output block's variable
     */
    public String getCommandLineID() {
        return commandLineID;
    }

    /**
     * (This method is used only if the block implements {@link InputBlock} or {@link OutputBlock})
     * Sets the command line identifier for this input/output block's variable
     *
     * @param id Block identifier when using the command line.
     */
    public void setCommandLineID(final String id) {
        commandLineID = id;
    }

    /**
     * @return the single variable of this block, useful for single input block as {@link InputBlock} or {@link OutputBlock}
     */
    public Var<?> getVariable() {
        // OutputBlock actually store the variable in the input list only
        return inputVars.first();
    }
}
