package plugins.adufour.blocks.lang;

import java.awt.Dimension;
import java.awt.Point;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.gui.plugin.PluginErrorReport;
import icy.network.NetworkUtil;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginInstaller;
import icy.plugin.PluginLoader;
import icy.plugin.PluginRepositoryLoader;
import icy.plugin.abstract_.Plugin;
import icy.util.ClassUtil;
import icy.util.StringUtil;
import icy.util.XMLUtil;
import plugins.adufour.blocks.tools.Display;
import plugins.adufour.blocks.tools.input.InputBlock;
import plugins.adufour.blocks.util.*;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarMutableArray;
import plugins.adufour.vars.util.MutableType;
import plugins.adufour.vars.util.VarListener;

/**
 * Class defining all the metadata associated to a {@link Block}.
 * 
 * @author Alexandre Dufour
 */
@SuppressWarnings("rawtypes")
public class BlockDescriptor implements Runnable, VarListener, VarListListener, XMLPersistent
{
    public static enum BlockStatus
    {
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

        private BlockStatus(String message)
        {
            defaultErrorMessage = message;
        }

        public String getUserMessage()
        {
            return optionalUserMessage;
        }

        public void setUserMessage(String message)
        {
            optionalUserMessage = (message != null ? message : "");
        }

        @Override
        public String toString()
        {
            return defaultErrorMessage + (optionalUserMessage.isEmpty() ? "" : ":\n" + optionalUserMessage);
        }
    }

    private Integer id = hashCode();

    private Block block;

    private WorkFlow container;

    private final HashSet<BlockListener> listeners = new HashSet<BlockListener>();

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

    /** Command-line ID (used only for input blocks) */
    private String commandLineID = "";

    public BlockDescriptor()
    {
        this.inputVars = new VarList();
        this.inputVars.addVarListListener(this);
        this.outputVars = new VarList();
        this.outputVars.addVarListListener(this);
    }

    public BlockDescriptor(int ID, Block block)
    {
        this();

        this.block = block;

        try
        {
            block.declareInput(inputVars);

            block.declareOutput(outputVars);
        }
        catch (RuntimeException e)
        {
            String blockName = block.getClass().getName();

            String devId = blockName.substring("plugins.".length());
            devId = devId.substring(0, devId.indexOf('.'));

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
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
     * @deprecated use {@link #BlockDescriptor(int, Block)} instead
     * @param ID
     *        Identifier
     * @param block
     *        Block template instance.
     * @param owner
     *        Workflow containing the new block.
     * @param location
     *        Position of the new block.
     */
    @Deprecated
    public BlockDescriptor(int ID, Block block, WorkFlow owner, Point location)
    {
        this(ID, block);

        this.container = owner;
        setLocation(location.x, location.y);
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
     * @param uid
     *        a unique identifier for the variable
     * @param variable
     *        the variable to add
     * @throws IllegalArgumentException
     *         if a variable with same unique ID exists
     */
    public void addInput(String uid, Var<?> variable) throws IllegalArgumentException
    {
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
     * @param uid
     *        a unique identifier for the variable
     * @param variable
     *        the variable to add
     * @throws IllegalArgumentException
     *         if a variable with same unique ID exists
     */
    public void addOutput(String uid, Var<?> variable) throws IllegalArgumentException
    {
        outputVars.add(uid, variable);
    }

    public void addBlockListener(BlockListener listener)
    {
        listeners.add(listener);
    }

    public void addBlockPanelListener(BlockListener listener)
    {
        listeners.add(listener);
    }

    public void removeBlockListener(BlockListener listener)
    {
        listeners.remove(listener);
    }

    public void removeBlockPanelListener(BlockListener listener)
    {
        listeners.remove(listener);
    }

    public Block getBlock()
    {
        return block;
    }

    public WorkFlow getContainer()
    {
        return container;
    }

    public Dimension getDimension()
    {
        return dimension;
    }

    public Integer getID()
    {
        return id;
    }

    /**
     * @return the location of the block in the work flow (use only in graphical mode)
     */
    public Point getLocation()
    {
        return location;
    }

    /**
     * @return The name of the block
     */
    public String getName()
    {
        String blockName = block.getClass().getSimpleName();

        if (block instanceof Plugin && ((Plugin) block).getDescriptor() != null)
        {
            String pluginName = ((Plugin) block).getDescriptor().getName();

            if (!pluginName.equalsIgnoreCase(blockName))
                return pluginName;
        }

        if (blockName.endsWith("block"))
        {
            blockName = blockName.substring(0, blockName.lastIndexOf("block"));
        }
        else if (blockName.endsWith("Block"))
        {
            blockName = blockName.substring(0, blockName.lastIndexOf("Block"));
        }

        return BlocksFinder.getFlattened(blockName);
    }

    public BlockStatus getStatus()
    {
        return status;
    }

    /**
     * Retrieves the unique ID of this variable. This method first searches in the list of input
     * variables, and then in the list of output variables if necessary.
     * 
     * @param variable
     *        The variable to retrieve.
     * @return Identifier of the given variable.
     * @throws NoSuchVariableException
     *         If the variable cannot be found in this block's input or output.
     */
    public String getVarID(Var<?> variable) throws NoSuchVariableException
    {
        if (inputVars.contains(variable))
        {
            String varID = inputVars.getID(variable);

            if (!isWorkFlow() || varID.contains(":"))
                return varID;

            return ((WorkFlow) block).getInputVarID(variable);
        }

        String varID = outputVars.getID(variable);

        if (!isWorkFlow() || varID.contains(":"))
            return varID;

        return ((WorkFlow) block).getOutputVarID(variable);
    }

    public boolean isCollapsed()
    {
        return collapsed;
    }

    /**
     * @return true if this block is the last block to execute in the work flow. If true, then the
     *         execution of the top-level work flow will stop after execution of this block.
     */
    public boolean isFinalBlock()
    {
        return finalBlock;
    }

    public boolean isLoop()
    {
        return block instanceof Loop;
    }

    public boolean isSingleBlock()
    {
        return block instanceof Display || block instanceof InputBlock;
    }

    public boolean isWorkFlow()
    {
        return block instanceof WorkFlow;
    }

    public boolean isTopLevelWorkFlow()
    {
        // Stephane FIX: properly detect top level workflow
        return isWorkFlow() && ((WorkFlow) block).isTopLevel();
        // return isWorkFlow() && container == null;
    }

    public void setCollapsed(boolean collapsed)
    {
        if (this.collapsed == collapsed)
            return;

        this.collapsed = collapsed;

        for (BlockListener l : listeners)
            l.blockCollapsed(this, collapsed);
    }

    public void setContainer(WorkFlow container)
    {
        this.container = container;
    }

    /**
     * Sets whether this block is the last block to execute in the work flow. If true, then the
     * execution of the top-level work flow will stop after execution of this block. Note that if
     * multiple blocks are marked as final, the execution will stop after the first block marked
     * final, in order of execution.
     * 
     * @param finalBlock
     *        true to stop the work flow after running this block, false otherwise.
     */
    public void setFinalBlock(boolean finalBlock)
    {
        this.finalBlock = finalBlock;
    }

    /**
     * Sets the dimension of this block.
     * 
     * @param width
     *        Width of the block in pixels.
     * @param height
     *        Height of the block in pixels.
     */
    public void setDimension(int width, int height)
    {
        if (dimension.width == width && dimension.height == height)
            return;

        dimension.setSize(width, height);

        for (BlockListener l : listeners)
            l.blockDimensionChanged(this, width, height);
    }

    /**
     * Sets the ID of this block.
     * 
     * @param id
     *        Block unique identifier.
     */
    public void setID(int id)
    {
        this.id = id;
    }

    /**
     * Sets the value of the specified input variable
     * 
     * @param <T>
     *        Type of the value of the variable being set.
     * @param varID
     *        The unique ID of the input variable to set.
     * @param value
     *        The new variable value.
     * @throws NoSuchVariableException
     *         If the variable is not in this block.
     */
    public <T> void setInput(String varID, T value) throws NoSuchVariableException
    {
        Var<T> input = inputVars.get(varID);
        if (input == null)
            throw new NoSuchVariableException(this, varID);

        input.setValue(value);
    }

    /**
     * Stores the location of the block and notifies listeners.
     * 
     * @param x
     *        X-axis position in pixels.
     * @param y
     *        Y-axis position in pixels.
     */
    public void setLocation(int x, int y)
    {
        if (location.x == x && location.y == y)
            return;

        location.move(x, y);
        for (BlockListener l : listeners)
            l.blockLocationChanged(this, x, y);
    }

    /**
     * Sets the value of the specified output variable
     * 
     * @param <T>
     *        Type of the value in the variable being set.
     * @param varID
     *        the unique ID of the output variable to set
     * @param value
     *        the new variable value
     * @throws NoSuchVariableException
     *         if the variable is not in this block
     */
    public <T> void setOutput(String varID, T value) throws NoSuchVariableException
    {
        Var<T> output = outputVars.get(varID);
        if (output == null)
            throw new NoSuchVariableException(this, varID);

        output.setValue(value);
    }

    public void setStatus(BlockStatus newStatus)
    {
        if (this.status == newStatus)
            return;

        this.status = newStatus;
        for (BlockListener listener : listeners)
            listener.blockStatusChanged(this, status);

        if (status == BlockStatus.DIRTY && container != null)
        {
            // propagate dirty status to the container (if it is not running)
            BlockDescriptor wfDescriptor = container.getBlockDescriptor();

            if (wfDescriptor.getStatus() != BlockStatus.RUNNING)
            {
                wfDescriptor.setStatus(BlockStatus.DIRTY);
            }
        }
    }

    /**
     * Removes the specified variable from the list of inputs
     * 
     * @param inputVar
     *        Variable to be removed from the input variable list.
     */
    public void removeInput(Var<?> inputVar)
    {
        if (!inputVars.contains(inputVar))
            return;

        inputVars.setVisible(inputVar, false);
        inputVars.remove(inputVar);
    }

    /**
     * Removes the specified variable from the list of outputs
     * 
     * @param outputVar
     *        Variable to be removed from the output variable list.
     */
    public void removeOutput(Var<?> outputVar)
    {
        if (!outputVars.contains(outputVar))
            return;

        outputVars.setVisible(outputVar, false);
        outputVars.remove(outputVar);
    }

    /**
     * Resets all output variables in this block to their default value
     */
    @SuppressWarnings("unchecked")
    public void reset()
    {
        setStatus(BlockStatus.DIRTY);

        for (Var var : outputVars)
            var.setValue(var.getDefaultValue());

        if (isWorkFlow())
            ((WorkFlow) block).reset();
    }

    public void run()
    {
        if (keepResults && status == BlockStatus.READY)
            return;

        setStatus(BlockStatus.RUNNING);

        try
        {
            block.run();
            setStatus(keepResults ? BlockStatus.READY : BlockStatus.DIRTY);
        }
        catch (RuntimeException e)
        {
            BlockStatus error = BlockStatus.ERROR;

            error.setUserMessage(e.getMessage());

            setStatus(error);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void valueChanged(Var source, Object oldValue, Object newValue)
    {
        if (inputVars.contains(source))
            setStatus(BlockStatus.DIRTY);

        for (BlockListener listener : listeners)
            listener.blockVariableChanged(this, source, newValue);
    }

    @Override
    public void referenceChanged(Var source, Var oldReference, Var newReference)
    {
        setStatus(BlockStatus.DIRTY);
    }

    @Override
    public String toString()
    {
        return getContainer().getBlockDescriptor().getName() + "." + getName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void variableAdded(VarList list, Var<?> variable)
    {
        variable.addListener(this);

        for (BlockListener listener : listeners)
            listener.blockVariableAdded(this, variable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void variableRemoved(VarList list, Var<?> variable)
    {
        variable.removeListener(this);

        // although we removed a variable, there is no "removed" notifier
        // use the "added" notifier just for the sake of refreshing the GUI
        for (BlockListener listener : listeners)
            listener.blockVariableAdded(this, variable);
    }

    /**
     * Goes online, downloads and installs the plug-in containing the specified block
     * 
     * @param blockType
     * @return
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Block> installRequiredBlock(String pluginClassName, String blockType)
            throws ClassNotFoundException
    {
        Class<?> clazz = null;

        // check if the class exists, otherwise try downloading it (if online)
        try
        {
            clazz = ClassUtil.findClass(blockType);
        }
        catch (ClassNotFoundException e)
        {
            if (!NetworkUtil.hasInternetAccess() || getClass().getClassLoader() == ClassLoader.getSystemClassLoader())
            {
                throw new BlocksException("Plugin " + blockType
                        + " is missing, but no internet connection is available.\nTry again later", true);
            }

            // String simpleName =
            // ClassUtil.getSimpleClassName(ClassUtil.getBaseClassName(blockType));

            // status.setValue("Downloading " + simpleName + "...");

            PluginDescriptor pd = PluginRepositoryLoader.getPlugin(pluginClassName);

            if (pd == null)
            {
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
    public boolean loadFromXML(Node node)
    {
        boolean noWarnings = true;

        Element blockNode = (Element) node;

        String blockType = XMLUtil.getAttributeValue(blockNode, "blockType", null);

        try
        {
            Class<? extends Block> blockClass = installRequiredBlock(
                    XMLUtil.getAttributeValue(blockNode, "className", null), blockType);

            block = blockClass.newInstance();

            if (block == null)
                throw new BlocksException("Couldn't create block from class " + blockClass.getName(), true);

            block.declareInput(inputVars);
            block.declareOutput(outputVars);

            setID(XMLUtil.getAttributeIntValue(blockNode, "ID", -1));

            int width = XMLUtil.getAttributeIntValue(blockNode, "width", 500);
            int height = XMLUtil.getAttributeIntValue(blockNode, "height", 500);
            setDimension(width, height);

            int xPos = XMLUtil.getAttributeIntValue(blockNode, "xLocation", -1);
            int yPos = XMLUtil.getAttributeIntValue(blockNode, "yLocation", -1);
            setLocation(xPos, yPos);

            Element varRoot = XMLUtil.getElement(blockNode, "variables");
            Element inVarRoot = XMLUtil.getElement(varRoot, "input");
            for (Element varNode : XMLUtil.getElements(inVarRoot))
            {
                String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                Var<?> var = inputVars.get(uid);

                if (var == null)
                {
                    if (noWarnings)
                    {
                        System.err.println("Error(s) while loading protocol:");
                        noWarnings = false;
                    }
                    System.err.println(new NoSuchVariableException(this, uid).getMessage());
                    continue;
                }

                if (var instanceof MutableType)
                {
                    String type = XMLUtil.getAttributeValue(varNode, "type", null);

                    if (type != null)
                    {
                        if (var instanceof VarMutable)
                        {
                            Class<?> mutableType = BlocksML.getPrimitiveType(type);

                            if (mutableType == null)
                                mutableType = Class.forName(type);

                            ((MutableType) var).setType(mutableType);
                        }
                        else if (var instanceof VarMutableArray)
                        {
                            ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                        }
                    }
                }

                var.loadFromXML(varNode);

                inputVars.setVisible(var, XMLUtil.getAttributeBooleanValue(varNode, "visible", false));
            }

            Element outVarRoot = XMLUtil.getElement(varRoot, "output");
            for (Element varNode : XMLUtil.getElements(outVarRoot))
            {
                String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                Var<?> var = outputVars.get(uid);

                if (var == null)
                {
                    if (noWarnings)
                    {
                        System.err.println("Error(s) while loading protocol:");
                        noWarnings = false;
                    }
                    System.err.println(new NoSuchVariableException(this, uid).getMessage());
                    continue;
                }

                outputVars.setVisible(var, XMLUtil.getAttributeBooleanValue(varNode, "visible", false));

                if (var instanceof MutableType)
                {
                    String type = XMLUtil.getAttributeValue(varNode, "type", null);
                    if (type != null)
                    {
                        if (var instanceof VarMutable)
                        {
                            Class<?> mutableType = BlocksML.getPrimitiveType(type);
                            ((MutableType) var).setType(mutableType != null ? mutableType : Class.forName(type));
                        }
                        else if (var instanceof VarMutableArray)
                        {
                            ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                        }
                    }
                }
            }

        }
        catch (ClassNotFoundException e1)
        {
            throw new BlocksException("Cannot create block (" + e1.getMessage() + ") => class not found", true);
        }
        catch (InstantiationException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        return noWarnings;
    }

    @Override
    public boolean saveToXML(Node node)
    {
        // TODO Auto-generated method stub
        return false;
    }

    public void setDefinedName(String s)
    {
        definedName = s;
    }

    public String getDefinedName()
    {
        if (definedName == null || definedName.isEmpty())
            return getName();

        return definedName;
    }

    private BlockDescriptor getBlockDescriptor(int blockId, Set<BlockDescriptor> bds)
    {
        for (BlockDescriptor bd : bds)
            if (bd.getID().intValue() == blockId)
                return bd;

        return null;
    }

    private String getNewVarId(String oldVarId, Map<BlockDescriptor, BlockDescriptor> copies)
    {
        // search for workflow shadow exposed variable
        final int index = oldVarId.indexOf(":");

        // direct variable id ? --> return it
        if (index == -1)
            return oldVarId;

        String result = "";

        // get block Id
        final int blockId = StringUtil.parseInt(oldVarId.substring(0, index), 0);
        // get block descriptor from id
        final BlockDescriptor bd = getBlockDescriptor(blockId, copies.keySet());
        // get corresponding new block
        final BlockDescriptor nbd = copies.get(bd);

        if (nbd != null)
            result = nbd.getID().toString();

        // remaining var id
        return result + ":" + getNewVarId(oldVarId.substring(index + 1), copies);
    }

    public BlockDescriptor clone(boolean embedding)
    {
        return clone(embedding, new HashMap<BlockDescriptor, BlockDescriptor>());
    }

    @SuppressWarnings("unchecked")
    public BlockDescriptor clone(boolean embedding, Map<BlockDescriptor, BlockDescriptor> copies)
    {
        Class<? extends Block> blockClass = PluginLoader.getPlugin(getBlock().getClass().getName()).getPluginClass()
                .asSubclass(Block.class);
        WorkFlow wf = null;
        WorkFlow wfCpy = null;
        BlockDescriptor cpy = null;
        Block newBlock = null;

        try
        {
            newBlock = blockClass.newInstance();
        }
        catch (InstantiationException e1)
        {
            e1.printStackTrace();
        }
        catch (IllegalAccessException e1)
        {
            e1.printStackTrace();
        }

        cpy = (newBlock instanceof WorkFlow ? ((WorkFlow) newBlock).getBlockDescriptor()
                : new BlockDescriptor(-1, newBlock));

        cpy.setDefinedName(getDefinedName());
        cpy.setLocation(getLocation().x + 12, getLocation().y + 12);
        cpy.setDimension(getDimension().width, getDimension().height);
        cpy.setCollapsed(isCollapsed());

        // Clone input variables
        for (Var oldVar : inputVars)
        {
            final String oldID = inputVars.getID(oldVar);

            // FIX: shadow exposed variable from workflow ? --> don't copy it for now (Stephane)
            if (oldID.contains(":") && (getBlock() instanceof WorkFlow))
                continue;

            Var newVar = cpy.inputVars.get(oldID);
            // If newVar is null, then oldVar was a runtime variable
            if (newVar == null)
            {
                newVar = new VarMutable(oldVar.getName(), oldVar.getType());
                cpy.inputVars.addRuntimeVariable(oldID, (VarMutable) newVar);
            }
            else
            {
                if (newVar instanceof VarMutable)
                    ((VarMutable) newVar).setType(oldVar.getType());
                newVar.setValue(oldVar.getValue());
            }

            // FIX: preserve visibility information (Stephane)
            cpy.inputVars.setVisible(newVar, inputVars.isVisible(oldVar));
        }

        // Clone output variables
        for (Var oldVar : outputVars)
        {
            final String oldID = outputVars.getID(oldVar);

            // FIX: shadow exposed variable from workflow ? --> don't copy it for now (Stephane)
            if (oldID.contains(":") && (getBlock() instanceof WorkFlow))
                continue;

            Var newVar = cpy.outputVars.get(oldID);
            // If newVar is null, then oldVar was a runtime variable
            if (newVar == null)
            {
                newVar = new VarMutable(oldVar.getName(), oldVar.getType());
                cpy.outputVars.addRuntimeVariable(oldID, (VarMutable) newVar);
            }
            else
            {
                if (newVar instanceof VarMutable)
                    ((VarMutable) newVar).setType(oldVar.getType());
                newVar.setValue(oldVar.getValue());
            }

            // FIX: preserve visibility information (Stephane)
            cpy.outputVars.setVisible(newVar, outputVars.isVisible(oldVar));
        }

        if (getBlock() instanceof WorkFlow)
        {
            wf = (WorkFlow) getBlock();
            wfCpy = (WorkFlow) cpy.getBlock();

            BlockDescriptor tmp;
            if (wf.getBlockSelection().isEmpty() || embedding)
            {
                for (BlockDescriptor bd : wf)
                {
                    tmp = bd.clone(true, copies);
                    copies.put(bd, tmp);
                    wfCpy.addBlock(tmp);
                    wfCpy.selectBlock(tmp);
                }
                cloneLinks(wf.getLinksIterator(), copies, wfCpy);
            }
            else
            {
                for (BlockDescriptor bd : wf.getBlockSelection())
                {
                    tmp = bd.clone(true, copies);
                    copies.put(bd, tmp);
                    wfCpy.addBlock(tmp);
                    wfCpy.selectBlock(tmp);
                }
                cloneLinks(wf.getLinkSelection(), copies, wfCpy);
            }

            for (Var oldVar : inputVars)
            {
                final String oldID = inputVars.getID(oldVar);
                final int index = oldID.indexOf(":");

                // shadow exposed variable ? --> try to recover it
                if (index != -1)
                {
                    // get block ID
                    final int blockID = StringUtil.parseInt(oldID.substring(0, index), 0);
                    // get var ID
                    final String varID = oldID.substring(index + 1);
                    // get new var ID
                    final String newVarID = getNewVarId(varID, copies);
                    // get block descriptor from ID
                    final BlockDescriptor bd = wf.getBlockByID(blockID);
                    // get corresponding new block
                    final BlockDescriptor nbd = copies.get(bd);
                    // get corresponding var for this block
                    final Var<Object> inputVar = nbd.inputVars.get(newVarID);

                    // finally add the shadow exposed variable if not already done (can be done in WorkFlow.addBlock(..) method) !
                    if (inputVar != null)
                    {
                        if (!cpy.inputVars.contains(inputVar))
                            cpy.addInput(wfCpy.getInputVarID(inputVar), inputVar);
                        // and preserve visibility (very important)
                        cpy.inputVars.setVisible(inputVar, inputVars.isVisible(oldVar));
                    }
                }
            }

            for (Var oldVar : outputVars)
            {
                final String oldID = outputVars.getID(oldVar);
                final int index = oldID.indexOf(":");

                // shadow exposed variable ? --> try to recover it
                if (index != -1)
                {
                    // get block ID
                    final int blockID = StringUtil.parseInt(oldID.substring(0, index), 0);
                    // get var ID
                    final String varID = oldID.substring(index + 1);
                    // get new var ID
                    final String newVarID = getNewVarId(varID, copies);
                    // get block descriptor from ID
                    final BlockDescriptor bd = wf.getBlockByID(blockID);
                    // get corresponding new block
                    final BlockDescriptor nbd = copies.get(bd);
                    // get corresponding var for this block
                    final Var<Object> outputVar = nbd.outputVars.get(newVarID);

                    // finally add the shadow exposed variable if not already done (can be done in WorkFlow.addBlock(..) method) !
                    if (outputVar != null)
                    {
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

    private static void cloneLinks(Iterable<Link<?>> iterable, Map<BlockDescriptor, BlockDescriptor> copies,
            WorkFlow dest)
    {
        for (Link<?> l : iterable)
        {
            if (l.srcBlock.getBlock() instanceof Loop || l.dstBlock.getBlock() instanceof Loop)
            {
                System.err.println("Warning : cannot copy a link to a loop variable");
                continue;
            }
            if (l.srcBlock.getBlock() instanceof WorkFlow || l.dstBlock.getBlock() instanceof WorkFlow)
            {
                System.err.println("Warning : cannot copy a link to an exposed variable");
                continue;
            }
            try
            {
                dest.addLink(copies.get(l.srcBlock),
                        copies.get(l.srcBlock).inputVars.get(l.srcBlock.inputVars.getID(l.srcVar)),
                        copies.get(l.dstBlock),
                        copies.get(l.dstBlock).inputVars.get(l.dstBlock.inputVars.getID(l.dstVar)));
            }
            catch (NoSuchVariableException nsve)
            {
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
    public boolean keepsResults()
    {
        return keepResults;
    }

    /**
     * Sets whether the block should keep results in memory (and not need to recalculate it unless a
     * parameter has changed)
     * 
     * @param keep
     *        Flag to keep results in memory.
     */
    public void keepResults(boolean keep)
    {
        this.keepResults = keep;
    }

    /**
     * (This method is used only if the block implements {@link InputBlock})
     * 
     * @return the command-line identifier for the input block's variable
     */
    public String getCommandLineID()
    {
        return commandLineID;
    }

    /**
     * (This method is used only if the block implements {@link InputBlock})
     * Sets the command line identifier for this input block's variable
     * 
     * @param id
     *        Block identifier when using the command line.
     */
    public void setCommandLineID(String id)
    {
        commandLineID = id;
    }
}
