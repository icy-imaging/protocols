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

package plugins.adufour.blocks.util;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.network.NetworkUtil;
import icy.plugin.*;
import icy.plugin.interface_.PluginBundled;
import icy.system.IcyExceptionHandler;
import icy.system.IcyHandledException;
import icy.system.thread.ThreadUtil;
import icy.util.ClassUtil;
import icy.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.tools.input.InputBlock;
import plugins.adufour.blocks.tools.output.OutputBlock;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.vars.gui.model.TypeSelectionModel;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarMutableArray;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.MutableType;
import plugins.adufour.vars.util.VarListener;

import javax.swing.filechooser.FileFilter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlocksML {
    private static final String RUNTIME = "runtime";

    private static final BlocksML instance = new BlocksML();

    public static BlocksML getInstance() {
        return instance;
    }

    public static final int CURRENT_VERSION = 4;

    public static final FileFilter XML_FILE_FILTER = new FileFilter() {
        @Override
        public String getDescription() {
            return "Icy protocols (.xml | .protocol)";
        }

        @Override
        public boolean accept(final File f) {
            return f.isDirectory() || f.getPath().toLowerCase().endsWith(".xml")
                    || f.getPath().toLowerCase().endsWith(".protocol");
        }
    };

    private Transformer transformer;

    private final VarString status = new VarString("status", "");

    private BlocksML() {
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        }
        catch (final Exception e) {
            // Ignore it
        }
    }

    public void addStatusListener(final VarListener<String> listener) {
        status.addListener(listener);
    }

    public void removeStatusListener(final VarListener<String> listener) {
        status.removeListener(listener);
    }

    /**
     * Generates the XML representation of this work flow as a String object.
     *
     * @param workFlow Target workflow instance.
     * @return An XML representation of this workflow as a String object
     * @throws TransformerException If an unrecoverable error occurs during the course of the transformation.
     */
    public synchronized String toString(final WorkFlow workFlow) throws TransformerException {
        return toString(toXML(workFlow));
    }

    /**
     * Converts an XML document to its string representation.
     *
     * @param xml The target xml document to transform into string.
     * @return An XML representation of this workflow as a String object
     * @throws TransformerException If an unrecoverable error occurs during the course of the transformation.
     */
    public synchronized String toString(final Document xml) throws TransformerException {
        xml.normalizeDocument();

        final DocumentType doctype = xml.getDoctype();
        final DOMSource domSource = new DOMSource(xml);
        final StringWriter string = new StringWriter();
        final StreamResult streamResult = new StreamResult(string);

        if (doctype != null) {
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
        }

        transformer.transform(domSource, streamResult);

        return string.toString();
    }

    /**
     * Creates an XML document representing the specified work flow.
     *
     * @param workFlow The work flow to save.
     * @return The XML document representing the target workflow.
     */
    // TODO: 08/06/2023 Remove old XML save mechanics
    public synchronized Document toXML(final WorkFlow workFlow) {
        final Document xml = XMLUtil.createDocument(false);
        final Element workSpaceRoot = XMLUtil.createRootElement(xml, "protocol");

        XMLUtil.setAttributeIntValue(workSpaceRoot, "VERSION", CURRENT_VERSION);

        switch (CURRENT_VERSION) {
            /*case 1:
                saveWorkFlow_V1(workFlow, workSpaceRoot);
                break;
            case 2:
                saveWorkFlow_V2(workFlow, workSpaceRoot);
                break;
            case 3:
                saveWorkFlow_V3(workFlow, workSpaceRoot);
                break;*/
            case 4:
                saveWorkFlow_V4(workFlow, workSpaceRoot);
                break;
            default:
                throw new UnsupportedOperationException("Cannot save Blocks ML version " + CURRENT_VERSION);
        }

        return xml;
    }

    /**
     * Saves the specified work flow into the specified file in XML format.
     *
     * @param workFlow The work flow to save.
     * @param f        The file to write (or overwrite).
     * @throws BlocksException If the file could not be saved.
     * @throws IOException     If an error occurs when saving the workflow in the file.
     */
    public synchronized void saveWorkFlow(final WorkFlow workFlow, final File f) throws BlocksException, IOException {
        final Document xml = toXML(workFlow);

        if (!XMLUtil.saveDocument(xml, f))
            throw new IOException("Unable to save the protocol.\nDo you have write permissions to the destination folder.");
    }

    /**
     * @param workFlow     The workflow to save.
     * @param workFlowRoot Root element where elements are to be inserted.
     */
    public synchronized void saveWorkFlow_V4(final WorkFlow workFlow, final Element workFlowRoot) {
        final Element blocksNode = XMLUtil.addElement(workFlowRoot, "blocks");

        for (final BlockDescriptor blockData : workFlow) {
            final Element blockNode;
            final Block block = blockData.getBlock();

            if (block instanceof WorkFlow) {
                blockNode = XMLUtil.addElement(blocksNode, "workflow");
                XMLUtil.setAttributeBooleanValue(blockNode, "collapsed", blockData.isCollapsed());
            }
            else {
                blockNode = XMLUtil.addElement(blocksNode, "block");
            }

            final String blockClass = block.getClass().getName();
            final String mainClass = block instanceof PluginBundled ? ((PluginBundled) block).getMainPluginClassName() : blockClass;

            XMLUtil.setAttributeValue(blockNode, "className", mainClass);
            XMLUtil.setAttributeValue(blockNode, "blockType", blockClass);
            XMLUtil.setAttributeIntValue(blockNode, "ID", blockData.getID());
            XMLUtil.setAttributeIntValue(blockNode, "xLocation", blockData.getLocation().x);
            XMLUtil.setAttributeIntValue(blockNode, "yLocation", blockData.getLocation().y);
            XMLUtil.setAttributeIntValue(blockNode, "width", blockData.getDimension().width);
            XMLUtil.setAttributeIntValue(blockNode, "height", blockData.getDimension().height);
            XMLUtil.setAttributeBooleanValue(blockNode, "collapsed", blockData.isCollapsed());
            XMLUtil.setAttributeValue(blockNode, "definedName", blockData.getDefinedName());
            XMLUtil.setAttributeBooleanValue(blockNode, "keepsResults", blockData.keepsResults());

            if ((block instanceof InputBlock) || (block instanceof OutputBlock))
                XMLUtil.setAttributeValue(blockNode, "CommandLineID", blockData.getCommandLineID());

            if (blockData.isWorkFlow() && block instanceof WorkFlow) {
                final WorkFlow innerWorkFlow = (WorkFlow) block;

                saveWorkFlow_V4(innerWorkFlow, blockNode);

                final Element varRoot = XMLUtil.addElement(blockNode, "variables");

                final Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (final Var<?> var : blockData.inputVars) {
                    final BlockDescriptor owner = innerWorkFlow.getInputOwner(var);
                    if (owner == null) {
                        System.err.println("Warning: could not find owner for input variable: \"" + var + "\", skipping");
                        continue;
                    }
                    final Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, "ID", blockData.getVarID(var));
                    XMLUtil.setAttributeIntValue(varNode, "blockID", owner.getID());
                    var.saveToXML(varNode);
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                }

                final Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (final Var<?> var : blockData.outputVars) {
                    final BlockDescriptor owner = innerWorkFlow.getOutputOwner(var);
                    if (owner == null) {
                        System.err.println(
                                "Warning: could not find owner for output variable: \"" + var + "\", skipping");
                        continue;
                    }
                    final Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, "ID", blockData.getVarID(var));
                    XMLUtil.setAttributeIntValue(varNode, "blockID", owner.getID());
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                }
            }
            else {
                final Element varRoot = XMLUtil.addElement(blockNode, "variables");

                final Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (final Var<?> var : blockData.inputVars) {
                    final Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, Var.XML_KEY_ID, blockData.getVarID(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());

                    // only save user values (linked values are stored or generated elsewhere)
                    if (var.getReference() == null)
                        var.saveToXML(varNode);

                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                    if (var instanceof MutableType) {
                        if (var instanceof VarMutable && var.getType() != null) {
                            XMLUtil.setAttributeValue(varNode, "type", var.getType().getName());
                        }
                        else if (var instanceof VarMutableArray && var.getType() != null) {
                            XMLUtil.setAttributeValue(varNode, "type", var.getType().getComponentType().getName());
                        }
                    }
                    XMLUtil.setAttributeBooleanValue(varNode, RUNTIME, blockData.inputVars.isRuntimeVariable(var));
                }

                final Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (final Var<?> var : blockData.outputVars) {
                    final Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, Var.XML_KEY_ID, blockData.getVarID(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                    if (var instanceof MutableType) {
                        if (var instanceof VarMutable && var.getType() != null) {
                            XMLUtil.setAttributeValue(varNode, "type", var.getType().getName());
                        }
                        else if (var instanceof VarMutableArray && var.getType() != null) {
                            XMLUtil.setAttributeValue(varNode, "type", var.getType().getComponentType().getName());
                        }
                    }
                    XMLUtil.setAttributeBooleanValue(varNode, RUNTIME, blockData.outputVars.isRuntimeVariable(var));
                }
            }
        }

        final Element linkRoot = XMLUtil.addElement(workFlowRoot, "links");

        for (final Link<?> link : workFlow.getLinksIterator()) {
            final Element linkNode = XMLUtil.addElement(linkRoot, "link");

            XMLUtil.setAttributeIntValue(linkNode, "srcBlockID", link.srcBlock.getID());
            XMLUtil.setAttributeValue(linkNode, "srcVarID", link.srcBlock.getVarID(link.srcVar));
            if (link.srcVar instanceof MutableType) {
                if (link.srcVar instanceof VarMutable && link.srcVar.getType() != null) {
                    XMLUtil.setAttributeValue(linkNode, "srcVarType", link.srcVar.getType().getName());
                }
                else if (link.srcVar instanceof VarMutableArray && link.srcVar.getType() != null) {
                    XMLUtil.setAttributeValue(linkNode, "srcVarType",
                            link.srcVar.getType().getComponentType().getName());
                }
            }
            XMLUtil.setAttributeIntValue(linkNode, "dstBlockID", link.dstBlock.getID());
            XMLUtil.setAttributeValue(linkNode, "dstVarID", link.dstBlock.getVarID(link.dstVar));
        }
    }

    /**
     * @param xml            The xml document to load.
     * @param targetWorkFlow The workflow where elements generated from the xml document are to be inserted.
     * @return true if the loaded file is at the latest BlocksML version, false otherwise.
     */
    public synchronized boolean loadWorkFlow(final Document xml, final WorkFlow targetWorkFlow) {
        final Element xmlRoot = XMLUtil.getRootElement(xml);

        final String rootName = xmlRoot.getNodeName();

        if (!rootName.equalsIgnoreCase("protocol") && !rootName.equalsIgnoreCase("workspace")) {
            throw new IcyHandledException("The selected file is not an Icy protocol");
        }

        final int fileVersion = XMLUtil.getAttributeIntValue(xmlRoot, "VERSION", -1);

        // scan the protocol for missing plugins
        final Set<String> missingPlugins = getMissingPlugins(xmlRoot, fileVersion);

        if (missingPlugins.size() > 0) {
            // Are we connected to the internet?
            if (!NetworkUtil.hasInternetAccess())
                throw new IcyHandledException("Some plugins required by this protocol are missing, but no internet connection is available");

            // Are we using Icy's plug-in loader (and not Eclipse)?
//            if (getClass().getClassLoader() == ClassLoader.getSystemClassLoader())
//                throw new IcyHandledException(
//                        "Cannot install missing blocks while using Icy4Eclipse in \"Debug\" or \"Run\" modes");

            // we are live!
            final String message = "[Protocols] Installing required plugins...";
            System.out.println(message);
            AnnounceFrame announcement = null;

            if (!Icy.getMainInterface().isHeadLess())
                announcement = new AnnounceFrame(message);

            final Set<PluginDescriptor> descriptors = new HashSet<>(missingPlugins.size());

            for (final String className : missingPlugins) {
                final PluginDescriptor pDesc = PluginRepositoryLoader.getPlugin(className);
                if (pDesc == null) {
                    // sadly, I believe the plugin loader could be reloaded at anytime...
                    // double check that for every plugin, just to be safe
                    if (PluginLoader.getPlugins(Block.class, true, false, false).size() == 0)
                        throw new BlocksReloadedException();

                    throw new BlocksException("Couldn't find plugin " + ClassUtil.getSimpleClassName(className) + " online", true);
                }
                descriptors.add(pDesc);
            }

            for (final PluginDescriptor pDesc : descriptors)
                PluginInstaller.install(pDesc, false);

            PluginInstaller.waitInstall();
            PluginLoader.waitWhileLoading();

            System.out.println("[Protocols] Plugins installed successfully.");
            if (announcement != null)
                announcement.close();

            // reload the whole mother!
            throw new BlocksReloadedException();
        }

        switch (fileVersion) {
            /*case 1:
                loadWorkFlow_V1(xmlRoot, targetWorkFlow);
                break;
            case 2:
                loadWorkFlow_V2(xmlRoot, targetWorkFlow);
                break;
            case 3:
                loadWorkFlow_V3(xmlRoot, targetWorkFlow);
                break;*/
            case 4:
                loadWorkFlow_V4(xmlRoot, targetWorkFlow);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Protocol version: " + fileVersion);
        }

        return fileVersion == CURRENT_VERSION;
    }

    private Set<String> getMissingPlugins(final Element workFlowRoot, final int fileVersion) {
        final Set<String> missingPlugins = new HashSet<>();

        final Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        // we are not in a node that has blocks => stop the recursion
        if (blocksRoot == null)
            return missingPlugins;

        final ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (final Element blockNode : blocksNode) {
            // the class name of the block to find
            final String blockTag = (fileVersion <= 2) ? "type" : "blockType";
            final String blockType = XMLUtil.getAttributeValue(blockNode, blockTag, null);
            // the class name of the plugin providing this block (may be the same)
            final String pluginClassName = XMLUtil.getAttributeValue(blockNode, "className", blockType);

            try {
                ClassUtil.findClass(blockType);
            }
            catch (final ClassNotFoundException e) {
                // the block appears to be missing
                // => is the corresponding plug-in missing or out-dated?
                final PluginDescriptor mainPlugin = PluginLoader.getPlugin(pluginClassName);
                if (mainPlugin == null || PluginUpdater.getUpdate(mainPlugin) != null) {
                    // store the class name of the missing plug-in
                    missingPlugins.add(pluginClassName);
                }
                else {
                    // The block is definitely missing, stop everything!
                    IcyExceptionHandler.handleException(mainPlugin, e, false);
                }
            }
            finally {
                // whether the current block is missing or not,
                // perhaps it's a work flow and has more blocks within
                missingPlugins.addAll(getMissingPlugins(blockNode, fileVersion));
            }
        }

        return missingPlugins;
    }

    /**
     * @param workFlowRoot XML element of the workflow root to be loaded.
     * @param workFlow     Target workflow to add elements from the document.
     * @throws BlocksException If an error occurs while loading the workflow.
     */
    public synchronized void loadWorkFlow_V4(final Element workFlowRoot, final WorkFlow workFlow) throws BlocksException {
        boolean hasWarnings = false;

        // Prepare a list of blocks to collapse after loading
        final ArrayList<BlockDescriptor> blocksToCollapse = new ArrayList<>();

        // 1) Load blocks and inner work flows

        final Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        final ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (final Element blockNode : blocksNode) {
            final String blockType = XMLUtil.getAttributeValue(blockNode, "blockType", null);

            final Block block;

            try {
                block = ThreadUtil.invokeNow(() -> (Block) ClassUtil.findClass(blockType).getDeclaredConstructor().newInstance());
            }
            catch (final Exception e) {
                String message = "Couldn't create block from class \"" + blockType + "\".\n";
                message += "Reason: " + e.getClass().getName() + " (" + e.getMessage() + ")";

                throw new BlocksException(message, true);
            }

            final int blockID = XMLUtil.getAttributeIntValue(blockNode, "ID", -1);

            final BlockDescriptor blockDescriptor;

            if (block instanceof WorkFlow) {
                blockDescriptor = ((WorkFlow) block).getBlockDescriptor();
                blockDescriptor.setID(blockID);
            }
            else {
                blockDescriptor = new BlockDescriptor(blockID, block);
            }

            final int xPos = XMLUtil.getAttributeIntValue(blockNode, "xLocation", -1);
            final int yPos = XMLUtil.getAttributeIntValue(blockNode, "yLocation", -1);
            blockDescriptor.setLocation(xPos, yPos);

            final int width = XMLUtil.getAttributeIntValue(blockNode, "width", 0);
            final int height = XMLUtil.getAttributeIntValue(blockNode, "height", 0);

            // set the block's dimension, but don't collapse just now
            // => wait for all the drawing to be done
            blockDescriptor.setDimension(width, height);

            final String definedName = XMLUtil.getAttributeValue(blockNode, "definedName", null);
            blockDescriptor.setDefinedName(definedName);

            blockDescriptor.keepResults(XMLUtil.getAttributeBooleanValue(blockNode, "keepsResults", true));

            if ((block instanceof InputBlock) || (block instanceof OutputBlock))
                blockDescriptor.setCommandLineID(XMLUtil.getAttributeValue(blockNode, "CommandLineID", ""));

            workFlow.addBlock(blockDescriptor);

            if (block instanceof WorkFlow) {
                // load the inner work flow
                loadWorkFlow_V4(blockNode, (WorkFlow) block);

                // adjust visibility triggers

                final Element varRoot = XMLUtil.getElement(blockNode, "variables");
                final Element inVarRoot = XMLUtil.getElement(varRoot, "input");
                final ArrayList<Element> inVarNodes = XMLUtil.getElements(inVarRoot);

                synchronized (blockDescriptor.inputVars) {
                    for (final Element varNode : inVarNodes) {
                        final String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        final Var<?> var = blockDescriptor.inputVars.get(uid);

                        if (var == null) {
                            System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                            continue;
                        }

                        if (var.getReference() == null) {
                            var.loadFromXML(varNode);

                            // adjust visibility

                            final boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                            try {
                                blockDescriptor.inputVars.setVisible(var, visible);
                            }
                            catch (final NoSuchVariableException e) {
                                if (!hasWarnings) {
                                    System.err.println("Error(s) while loading protocol:");
                                    hasWarnings = true;
                                }
                                System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                            }
                        }
                    }
                }

                final Element outVarRoot = XMLUtil.getElement(varRoot, "output");
                final ArrayList<Element> outVarNodes = XMLUtil.getElements(outVarRoot);

                synchronized (blockDescriptor.outputVars) {
                    for (final Element varNode : outVarNodes) {
                        final String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        final Var<?> var = blockDescriptor.outputVars.get(uid);

                        // adjust visibility

                        final boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                        try {
                            blockDescriptor.outputVars.setVisible(var, visible);
                        }
                        catch (final NoSuchVariableException e) {
                            if (!hasWarnings) {
                                System.err.println("Error(s) while loading protocol:");
                                hasWarnings = true;
                            }
                            System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                            // throw new NoSuchVariableException(blockInfo, uid);
                        }
                    }
                }
            }
            else {
                final Element varRoot = XMLUtil.getElement(blockNode, "variables");
                final Element inVarRoot = XMLUtil.getElement(varRoot, "input");
                final ArrayList<Element> inVarNodes = XMLUtil.getElements(inVarRoot);

                synchronized (blockDescriptor.inputVars) {
                    for (final Element varNode : inVarNodes) {
                        final String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockDescriptor.inputVars.get(uid);

                        final boolean isDynamicVariable = XMLUtil.getAttributeBooleanValue(varNode, RUNTIME, false);

                        if (var == null) {
                            if (isDynamicVariable) {
                                var = new VarMutable(XMLUtil.getAttributeValue(varNode, "name", "input"), null);
                                ((VarMutable) var).setDefaultEditorModel(new TypeSelectionModel());
                                blockDescriptor.inputVars.addRuntimeVariable(uid, (VarMutable) var);
                            }
                            else {
                                if (!hasWarnings) {
                                    System.err.println("Error(s) while loading protocol:");
                                    hasWarnings = true;
                                }
                                System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                                continue;
                                // throw new NoSuchVariableException(blockInfo, uid);
                            }
                        }

                        try {
                            if (var instanceof MutableType) {
                                Class<?> mutableType = null;

                                final String type = XMLUtil.getAttributeValue(varNode, "type", null);

                                if (type != null) {
                                    if (var instanceof VarMutable) {
                                        mutableType = getPrimitiveType(type);

                                        if (mutableType == null)
                                            mutableType = Class.forName(type);
                                    }
                                    else if (var instanceof VarMutableArray) {
                                        mutableType = Class.forName("[L" + type + ";");
                                    }

                                    ((MutableType) var).setType(mutableType);
                                }
                            }

                            var.loadFromXML(varNode);

                            // Input blocks can have their values set from command-line
                            if (block instanceof InputBlock) {
                                final String clID = blockDescriptor.getCommandLineID();
                                if (!clID.isEmpty()) {
                                    final Map<String, String> map = Protocols.getCommandLineArguments();
                                    if (map.containsKey(clID)) {
                                        // Set the value of the first (and only) block variable
                                        var.setValueAsString(map.get(clID));
                                    }
                                }
                            }
                        }
                        catch (final Exception e) {
                            String message = "Unable to read input variable \"" + var.getName() + "\" in block \"" + blockDescriptor.getDefinedName() + "\".\n";
                            message += "Reason: " + e.getClass().getName() + " (" + e.getMessage() + ")";
                            throw new BlocksException(message, true);
                        }
                        blockDescriptor.inputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));
                    }
                }

                final Element outVarRoot = XMLUtil.getElement(varRoot, "output");
                final ArrayList<Element> outVarNodes = XMLUtil.getElements(outVarRoot);

                synchronized (blockDescriptor.outputVars) {
                    for (final Element varNode : outVarNodes) {
                        final String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockDescriptor.outputVars.get(uid);

                        final boolean isDynamicVariable = XMLUtil.getAttributeBooleanValue(varNode, RUNTIME, false);

                        if (var == null) {
                            if (isDynamicVariable) {
                                var = new VarMutable(XMLUtil.getAttributeValue(varNode, "name", "output"), null);
                                ((VarMutable) var).setDefaultEditorModel(new TypeSelectionModel());
                                blockDescriptor.outputVars.addRuntimeVariable(uid, (VarMutable) var);
                            }
                            else {
                                if (!hasWarnings) {
                                    System.err.println("Error(s) while loading protocol:");
                                    hasWarnings = true;
                                }
                                System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                                continue;
                                // throw new NoSuchVariableException(blockInfo, uid);
                            }
                        }

                        blockDescriptor.outputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));

                        if (var instanceof MutableType)
                            try {
                                final String type = XMLUtil.getAttributeValue(varNode, "type", null);
                                if (type != null) {
                                    if (var instanceof VarMutable) {
                                        final Class<?> mutableType = getPrimitiveType(type);
                                        ((MutableType) var)
                                                .setType(mutableType != null ? mutableType : Class.forName(type));
                                    }
                                    else if (var instanceof VarMutableArray) {
                                        ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                                    }
                                }
                            }
                            catch (final ClassNotFoundException e) {
                                String message = "Unable to read output variable \"" + var.getName() + "\" in block \""
                                        + blockDescriptor.getDefinedName() + "\".\n";
                                message += "Reason: " + e.getClass().getName() + " (" + e.getMessage() + ")";
                                throw new BlocksException(message, true);
                            }
                    }
                }
            }

            // prepare to collapse the block if necessary
            if (XMLUtil.getAttributeBooleanValue(blockNode, "collapsed", false))
                blocksToCollapse.add(blockDescriptor);
        }

        // 2) Load links

        final Element linkRoot = XMLUtil.getElement(workFlowRoot, "links");

        final ArrayList<Element> links = XMLUtil.getElements(linkRoot);
        links.sort((link1, link2) -> {
            final int srcBlock1 = XMLUtil.getAttributeIntValue(link1, "srcBlockID", -1);
            final int srcBlock2 = XMLUtil.getAttributeIntValue(link2, "srcBlockID", -1);

            final int i1 = workFlow.indexOf(workFlow.getBlockByID(srcBlock1));
            final int i2 = workFlow.indexOf(workFlow.getBlockByID(srcBlock2));

            //return i1 < i2 ? -1 : i1 > i2 ? 1 : 0;
            return Integer.compare(i1, i2); // TODO: 28/02/2023 Check if same result
        });

        for (final Element linkNode : links) {
            final int srcBlockID = XMLUtil.getAttributeIntValue(linkNode, "srcBlockID", -1);
            final String srcVarID = XMLUtil.getAttributeValue(linkNode, "srcVarID", null);
            final int dstBlockID = XMLUtil.getAttributeIntValue(linkNode, "dstBlockID", -1);
            final String dstVarID = XMLUtil.getAttributeValue(linkNode, "dstVarID", null);

            // load the source variable
            final BlockDescriptor srcBlock = workFlow.getBlockByID(srcBlockID);
            Var<Object> srcVar = srcBlock.outputVars.get(srcVarID);
            if (srcVar == null)
                srcVar = srcBlock.inputVars.get(srcVarID);

            if (srcVar == null) {
                System.err.println("Cannot create a link from variable " + srcVarID + " (from block " + srcBlock
                        + "). It may have been removed or renamed.");
                continue;
            }

            if (srcVar instanceof MutableType) {
                final String type = XMLUtil.getAttributeValue(linkNode, "srcVarType", null);
                if (type != null) {
                    try {
                        if (srcVar instanceof VarMutable) {
                            final Class<?> mutableType = getPrimitiveType(type);
                            ((MutableType) srcVar).setType(mutableType != null ? mutableType : Class.forName(type));
                        }
                        else if (srcVar instanceof VarMutableArray) {
                            ((MutableType) srcVar).setType(Class.forName("[L" + type + ";"));
                        }
                    }
                    catch (final ClassNotFoundException e1) {
                        throw new BlocksException("Cannot create block (" + e1.getMessage() + ") => class not found",
                                true);
                    }
                }
            }

            // load the destination variable
            final BlockDescriptor dstBlock = workFlow.getBlockByID(dstBlockID);

            final Var<Object> dstVar = dstBlock.inputVars.get(dstVarID);

            if (dstVar == null) {
                System.err.println("Cannot link to variable " + dstVarID + " (from block " + dstBlock
                        + "). It may have been removed or renamed.");
                continue;
            }

            workFlow.addLink(srcBlock, srcVar, dstBlock, dstVar);
        }

        // 3) Collapse necessary blocks
        for (final BlockDescriptor block : blocksToCollapse)
            block.setCollapsed(true);

        if (hasWarnings)
            System.err.println("--");
    }

    public synchronized void loadWorkFlow_V5(final Element workFlowRoot, final WorkFlow workFlow, final WorkFlow parentFlow) throws BlocksException {
        boolean noWarnings = true;

        final Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        final ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (final Element blockNode : blocksNode) {
            final BlockDescriptor blockDesc = new BlockDescriptor();
            noWarnings &= blockDesc.loadFromXML(blockNode);
            workFlow.addBlock(blockDesc);
        }

        final Element linkRoot = XMLUtil.getElement(workFlowRoot, "links");

        for (final Element linkNode : XMLUtil.getElements(linkRoot)) {
            final Link<?> link = new Link<>(workFlow);
            link.loadFromXML(linkNode);
        }

        if (!noWarnings)
            System.err.println("--");
    }

    /**
     * @param primitiveName The string representation of the primitive type.
     * @return The Java primitive type represented by the given name, or null if the given name does
     * not represent a primitive type
     */
    public static Class<?> getPrimitiveType(final String primitiveName) {
        if (primitiveName.equals("byte"))
            return byte.class;
        if (primitiveName.equals("short"))
            return short.class;
        if (primitiveName.equals("int"))
            return int.class;
        if (primitiveName.equals("long"))
            return long.class;
        if (primitiveName.equals("char"))
            return char.class;
        if (primitiveName.equals("float"))
            return float.class;
        if (primitiveName.equals("double"))
            return double.class;
        if (primitiveName.equals("boolean"))
            return boolean.class;
        if (primitiveName.equals("void"))
            return void.class;

        return null;
    }
}
