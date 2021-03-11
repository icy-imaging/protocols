package plugins.adufour.blocks.util;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.filechooser.FileFilter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.network.NetworkUtil;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginInstaller;
import icy.plugin.PluginLoader;
import icy.plugin.PluginRepositoryLoader;
import icy.plugin.PluginUpdater;
import icy.plugin.interface_.PluginBundled;
import icy.system.IcyExceptionHandler;
import icy.system.IcyHandledException;
import icy.system.thread.ThreadUtil;
import icy.util.ClassUtil;
import icy.util.XMLUtil;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.tools.input.InputBlock;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.vars.gui.model.TypeSelectionModel;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarMutableArray;
import plugins.adufour.vars.lang.VarString;
import plugins.adufour.vars.util.MutableType;
import plugins.adufour.vars.util.VarListener;

public class BlocksML
{
    private static final String RUNTIME = "runtime";

    private static BlocksML instance = new BlocksML();

    public static BlocksML getInstance()
    {
        return instance;
    }

    public static final int CURRENT_VERSION = 4;

    public static final FileFilter XML_FILE_FILTER = new FileFilter()
    {
        @Override
        public String getDescription()
        {
            return "Icy protocols (.xml | .protocol)";
        }

        @Override
        public boolean accept(File f)
        {
            return f.isDirectory() || f.getPath().toLowerCase().endsWith(".xml")
                    || f.getPath().toLowerCase().endsWith(".protocol");
        }
    };

    private Transformer transformer;

    private VarString status = new VarString("status", "");

    private BlocksML()
    {
        try
        {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        }
        catch (Exception e)
        {
        }
    }

    public void addStatusListener(VarListener<String> listener)
    {
        status.addListener(listener);
    }

    public void removeStatusListener(VarListener<String> listener)
    {
        status.removeListener(listener);
    }

    /**
     * Generates the XML representation of this work flow as a String object.
     * 
     * @param workFlow
     *        Target workflow instance.
     * @return A XML representation of this work flow as a String object
     * @throws TransformerException
     *         If an unrecoverable error occurs during the course of the transformation.
     */
    public synchronized String toString(WorkFlow workFlow) throws TransformerException
    {
        return toString(toXML(workFlow));
    }

    /**
     * Converts an XML document to its string representation.
     * 
     * @param xml
     *        The target xml document to transform into string.
     * @return A XML representation of this work flow as a String object
     * @throws TransformerException
     *         If an unrecoverable error occurs during the course of the transformation.
     */
    public synchronized String toString(Document xml) throws TransformerException
    {
        xml.normalizeDocument();

        DocumentType doctype = xml.getDoctype();
        DOMSource domSource = new DOMSource(xml);
        StringWriter string = new StringWriter();
        StreamResult streamResult = new StreamResult(string);

        if (doctype != null)
        {
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
        }

        transformer.transform(domSource, streamResult);

        return string.toString();
    }

    /**
     * Creates a XML document representing the specified work flow.
     * 
     * @param workFlow
     *        The work flow to save.
     * @return The XML document representing the target workflow.
     */
    public synchronized Document toXML(WorkFlow workFlow)
    {
        Document xml = XMLUtil.createDocument(false);
        Element workSpaceRoot = XMLUtil.createRootElement(xml, "protocol");

        XMLUtil.setAttributeIntValue(workSpaceRoot, "VERSION", CURRENT_VERSION);

        switch (CURRENT_VERSION)
        {
            case 1:
                saveWorkFlow_V1(workFlow, workSpaceRoot);
                break;
            case 2:
                saveWorkFlow_V2(workFlow, workSpaceRoot);
                break;
            case 3:
                saveWorkFlow_V3(workFlow, workSpaceRoot);
                break;
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
     * @param workFlow
     *        The work flow to save.
     * @param f
     *        The file to write (or overwrite).
     * @throws BlocksException
     *         If the file could not be saved.
     * @throws IOException
     *         If an error occurs when saving the workflow in the file.
     */
    public synchronized void saveWorkFlow(WorkFlow workFlow, File f) throws BlocksException, IOException
    {
        Document xml = toXML(workFlow);

        if (!XMLUtil.saveDocument(xml, f))
            throw new IOException(
                    "Unable to save the protocol.\nDo you have write permissions to the destination folder.");
    }

    /**
     * @deprecated Legacy method, use {@link #saveWorkFlow_V4(WorkFlow, Element)} instead.
     * @param workFlow
     *        The workflow to save.
     * @param workFlowRoot
     *        Root element where elements are to be inserted.
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public synchronized void saveWorkFlow_V1(WorkFlow workFlow, Element workFlowRoot)
    {
        Element blocksNode = XMLUtil.addElement(workFlowRoot, "blocks");

        for (BlockDescriptor blockData : workFlow)
        {
            Element blockNode;
            Block block = blockData.getBlock();

            if (block instanceof WorkFlow)
            {
                blockNode = XMLUtil.addElement(blocksNode, "workflow");
            }
            else
            {
                blockNode = XMLUtil.addElement(blocksNode, "block");
            }

            XMLUtil.setAttributeValue(blockNode, "type", block.getClass().getCanonicalName());
            XMLUtil.setAttributeIntValue(blockNode, "ID", workFlow.indexOf(blockData));
            XMLUtil.setAttributeIntValue(blockNode, "xLocation", blockData.getLocation().x);
            XMLUtil.setAttributeIntValue(blockNode, "yLocation", blockData.getLocation().y);

            if (block instanceof WorkFlow)
            {
                WorkFlow innerWorkFlow = (WorkFlow) block;

                saveWorkFlow_V1(innerWorkFlow, blockNode);

                Element varRoot = XMLUtil.addElement(blockNode, "variables");

                Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (Var<?> var : blockData.inputVars)
                {
                    BlockDescriptor owner = innerWorkFlow.getInputOwner(var);
                    Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeIntValue(varNode, "ID", blockData.inputVars.indexOf(var));
                    XMLUtil.setAttributeIntValue(varNode, "blockID", innerWorkFlow.indexOf(owner));
                    XMLUtil.setAttributeIntValue(varNode, "varID", owner.inputVars.indexOf(var));
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                }

                Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (Var<?> var : blockData.outputVars)
                {
                    BlockDescriptor owner = innerWorkFlow.getOutputOwner(var);
                    Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeIntValue(varNode, "ID", blockData.outputVars.indexOf(var));
                    XMLUtil.setAttributeIntValue(varNode, "blockID", innerWorkFlow.indexOf(owner));
                    XMLUtil.setAttributeIntValue(varNode, "varID", owner.outputVars.indexOf(var));
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                }
            }
            else
            {
                Element varRoot = XMLUtil.addElement(blockNode, "variables");

                Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (Var<?> var : blockData.inputVars)
                {
                    Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeIntValue(varNode, Var.XML_KEY_ID, blockData.inputVars.indexOf(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());
                    var.saveToXML(varNode);
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                }

                Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (Var<?> var : blockData.outputVars)
                {
                    Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeIntValue(varNode, Var.XML_KEY_ID, blockData.outputVars.indexOf(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());
                    var.saveToXML(varNode);
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                }
            }
        }

        Element linkRoot = XMLUtil.addElement(workFlowRoot, "links");

        for (Link<?> link : workFlow.getLinksIterator())
        {
            Element linkNode = XMLUtil.addElement(linkRoot, "link");

            XMLUtil.setAttributeIntValue(linkNode, "srcBlockID", workFlow.indexOf(link.srcBlock));
            XMLUtil.setAttributeIntValue(linkNode, "srcVarID", link.srcBlock.outputVars.indexOf(link.srcVar));
            XMLUtil.setAttributeIntValue(linkNode, "dstBlockID", workFlow.indexOf(link.dstBlock));
            XMLUtil.setAttributeIntValue(linkNode, "dstVarID", link.dstBlock.inputVars.indexOf(link.dstVar));
        }
    }

    /**
     * @deprecated Legacy method, use {@link #saveWorkFlow_V4(WorkFlow, Element)} instead.
     * @param workFlow
     *        The workflow to save.
     * @param workFlowRoot
     *        Root element where elements are to be inserted.
     */
    @Deprecated
    public synchronized void saveWorkFlow_V2(WorkFlow workFlow, Element workFlowRoot)
    {
        Element blocksNode = XMLUtil.addElement(workFlowRoot, "blocks");

        for (BlockDescriptor blockData : workFlow)
        {
            Element blockNode;
            Block block = blockData.getBlock();

            if (block instanceof WorkFlow)
            {
                blockNode = XMLUtil.addElement(blocksNode, "workflow");
                XMLUtil.setAttributeBooleanValue(blockNode, "collapsed", blockData.isCollapsed());
            }
            else
            {
                blockNode = XMLUtil.addElement(blocksNode, "block");
            }

            XMLUtil.setAttributeValue(blockNode, "type", block.getClass().getCanonicalName());
            XMLUtil.setAttributeIntValue(blockNode, "ID", workFlow.indexOf(blockData));
            XMLUtil.setAttributeIntValue(blockNode, "xLocation", blockData.getLocation().x);
            XMLUtil.setAttributeIntValue(blockNode, "yLocation", blockData.getLocation().y);

            if (blockData.isWorkFlow())
            {
                WorkFlow innerWorkFlow = (WorkFlow) block;

                saveWorkFlow_V2(innerWorkFlow, blockNode);

                Element varRoot = XMLUtil.addElement(blockNode, "variables");

                Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (Var<?> var : blockData.inputVars)
                {
                    BlockDescriptor owner = innerWorkFlow.getInputOwner(var);
                    Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, "ID", blockData.inputVars.getID(var));
                    XMLUtil.setAttributeIntValue(varNode, "blockID", innerWorkFlow.indexOf(owner));
                    XMLUtil.setAttributeValue(varNode, "varID", owner.inputVars.getID(var));
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                }

                Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (Var<?> var : blockData.outputVars)
                {
                    BlockDescriptor owner = innerWorkFlow.getOutputOwner(var);
                    Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, "ID", blockData.outputVars.getID(var));
                    XMLUtil.setAttributeIntValue(varNode, "blockID", innerWorkFlow.indexOf(owner));
                    XMLUtil.setAttributeValue(varNode, "varID", owner.outputVars.getID(var));
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                }
            }
            else
            {
                Element varRoot = XMLUtil.addElement(blockNode, "variables");

                Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (Var<?> var : blockData.inputVars)
                {
                    Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, Var.XML_KEY_ID, blockData.inputVars.getID(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());
                    var.saveToXML(varNode);
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                    if (var instanceof VarMutable && var.getType() != null)
                    {
                        XMLUtil.setAttributeValue(varNode, "type", var.getType().getCanonicalName());
                    }
                    else if (var instanceof VarMutableArray && var.getType() != null)
                    {
                        XMLUtil.setAttributeValue(varNode, "type", var.getType().getComponentType().getCanonicalName());
                    }
                }

                Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (Var<?> var : blockData.outputVars)
                {
                    Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, Var.XML_KEY_ID, blockData.outputVars.getID(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());
                    var.saveToXML(varNode);
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                    if (var instanceof VarMutable && var.getType() != null)
                    {
                        XMLUtil.setAttributeValue(varNode, "type", var.getType().getCanonicalName());
                    }
                    else if (var instanceof VarMutableArray && var.getType() != null)
                    {
                        XMLUtil.setAttributeValue(varNode, "type", var.getType().getComponentType().getCanonicalName());
                    }
                }
            }
        }

        Element linkRoot = XMLUtil.addElement(workFlowRoot, "links");

        for (Link<?> link : workFlow.getLinksIterator())
        {
            Element linkNode = XMLUtil.addElement(linkRoot, "link");

            XMLUtil.setAttributeIntValue(linkNode, "srcBlockID", workFlow.indexOf(link.srcBlock));
            XMLUtil.setAttributeValue(linkNode, "srcVarID", link.srcBlock.getVarID(link.srcVar));
            XMLUtil.setAttributeIntValue(linkNode, "dstBlockID", workFlow.indexOf(link.dstBlock));
            XMLUtil.setAttributeValue(linkNode, "dstVarID", link.dstBlock.getVarID(link.dstVar));
        }
    }

    /**
     * @deprecated Legacy method, use {@link #saveWorkFlow_V4(WorkFlow, Element)} instead.
     * @param workFlow
     *        The workflow to save.
     * @param workFlowRoot
     *        Root element where elements are to be inserted.
     */
    @Deprecated
    public synchronized void saveWorkFlow_V3(WorkFlow workFlow, Element workFlowRoot)
    {
        Element blocksNode = XMLUtil.addElement(workFlowRoot, "blocks");

        for (BlockDescriptor blockData : workFlow)
        {
            Element blockNode;
            Block block = blockData.getBlock();

            if (block instanceof WorkFlow)
            {
                blockNode = XMLUtil.addElement(blocksNode, "workflow");
                XMLUtil.setAttributeBooleanValue(blockNode, "collapsed", blockData.isCollapsed());
                XMLUtil.setAttributeIntValue(blockNode, "width", blockData.getDimension().width);
                XMLUtil.setAttributeIntValue(blockNode, "height", blockData.getDimension().height);
            }
            else
            {
                blockNode = XMLUtil.addElement(blocksNode, "block");
            }

            String blockClass = block.getClass().getCanonicalName();
            String mainClass = block instanceof PluginBundled ? ((PluginBundled) block).getMainPluginClassName()
                    : blockClass;

            XMLUtil.setAttributeValue(blockNode, "className", mainClass);
            XMLUtil.setAttributeValue(blockNode, "blockType", blockClass);
            XMLUtil.setAttributeIntValue(blockNode, "ID", workFlow.indexOf(blockData));
            XMLUtil.setAttributeIntValue(blockNode, "xLocation", blockData.getLocation().x);
            XMLUtil.setAttributeIntValue(blockNode, "yLocation", blockData.getLocation().y);

            if (blockData.isWorkFlow())
            {
                WorkFlow innerWorkFlow = (WorkFlow) block;

                saveWorkFlow_V3(innerWorkFlow, blockNode);

                Element varRoot = XMLUtil.addElement(blockNode, "variables");

                Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (Var<?> var : blockData.inputVars)
                {
                    Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, "ID", innerWorkFlow.getInputVarID(var));
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                }

                Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (Var<?> var : blockData.outputVars)
                {
                    Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, "ID", innerWorkFlow.getOutputVarID(var));
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                }
            }
            else
            {
                Element varRoot = XMLUtil.addElement(blockNode, "variables");

                Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (Var<?> var : blockData.inputVars)
                {
                    Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, Var.XML_KEY_ID, blockData.inputVars.getID(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());
                    var.saveToXML(varNode);
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                    if (var instanceof VarMutable && var.getType() != null)
                    {
                        XMLUtil.setAttributeValue(varNode, "type", var.getType().getCanonicalName());
                    }
                    else if (var instanceof VarMutableArray && var.getType() != null)
                    {
                        XMLUtil.setAttributeValue(varNode, "type", var.getType().getComponentType().getCanonicalName());
                    }
                }

                Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (Var<?> var : blockData.outputVars)
                {
                    Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, Var.XML_KEY_ID, blockData.outputVars.getID(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());
                    var.saveToXML(varNode);
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                    if (var instanceof VarMutable && var.getType() != null)
                    {
                        XMLUtil.setAttributeValue(varNode, "type", var.getType().getCanonicalName());
                    }
                    else if (var instanceof VarMutableArray && var.getType() != null)
                    {
                        XMLUtil.setAttributeValue(varNode, "type", var.getType().getComponentType().getCanonicalName());
                    }
                }
            }
        }

        Element linkRoot = XMLUtil.addElement(workFlowRoot, "links");

        for (Link<?> link : workFlow.getLinksIterator())
        {
            Element linkNode = XMLUtil.addElement(linkRoot, "link");

            XMLUtil.setAttributeIntValue(linkNode, "srcBlockID", workFlow.indexOf(link.srcBlock));
            XMLUtil.setAttributeValue(linkNode, "srcVarID", link.srcBlock.getVarID(link.srcVar));
            XMLUtil.setAttributeIntValue(linkNode, "dstBlockID", workFlow.indexOf(link.dstBlock));
            XMLUtil.setAttributeValue(linkNode, "dstVarID", link.dstBlock.getVarID(link.dstVar));
        }
    }

    /**
     * @param workFlow
     *        The workflow to save.
     * @param workFlowRoot
     *        Root element where elements are to be inserted.
     */
    public synchronized void saveWorkFlow_V4(WorkFlow workFlow, Element workFlowRoot)
    {
        Element blocksNode = XMLUtil.addElement(workFlowRoot, "blocks");

        for (BlockDescriptor blockData : workFlow)
        {
            Element blockNode;
            Block block = blockData.getBlock();

            if (block instanceof WorkFlow)
            {
                blockNode = XMLUtil.addElement(blocksNode, "workflow");
                XMLUtil.setAttributeBooleanValue(blockNode, "collapsed", blockData.isCollapsed());
            }
            else
            {
                blockNode = XMLUtil.addElement(blocksNode, "block");
            }

            String blockClass = block.getClass().getName();
            String mainClass = block instanceof PluginBundled ? ((PluginBundled) block).getMainPluginClassName()
                    : blockClass;

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

            if (block instanceof InputBlock)
                XMLUtil.setAttributeValue(blockNode, "CommandLineID", blockData.getCommandLineID());

            if (blockData.isWorkFlow())
            {
                WorkFlow innerWorkFlow = (WorkFlow) block;

                saveWorkFlow_V4(innerWorkFlow, blockNode);

                Element varRoot = XMLUtil.addElement(blockNode, "variables");

                Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (Var<?> var : blockData.inputVars)
                {
                    BlockDescriptor owner = innerWorkFlow.getInputOwner(var);
                    if (owner == null)
                    {
                        System.err
                                .println("Warning: could not find owner for input variable: \"" + var + "\", skipping");
                        continue;
                    }
                    Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, "ID", blockData.getVarID(var));
                    XMLUtil.setAttributeIntValue(varNode, "blockID", owner.getID());
                    var.saveToXML(varNode);
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                }

                Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (Var<?> var : blockData.outputVars)
                {
                    BlockDescriptor owner = innerWorkFlow.getOutputOwner(var);
                    if (owner == null)
                    {
                        System.err.println(
                                "Warning: could not find owner for output variable: \"" + var + "\", skipping");
                        continue;
                    }
                    Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, "ID", blockData.getVarID(var));
                    XMLUtil.setAttributeIntValue(varNode, "blockID", owner.getID());
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                }
            }
            else
            {
                Element varRoot = XMLUtil.addElement(blockNode, "variables");

                Element inputVarRoot = XMLUtil.addElement(varRoot, "input");

                for (Var<?> var : blockData.inputVars)
                {
                    Element varNode = XMLUtil.addElement(inputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, Var.XML_KEY_ID, blockData.getVarID(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());

                    // only save user values (linked values are stored or generated elsewhere)
                    if (var.getReference() == null)
                        var.saveToXML(varNode);

                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.inputVars.isVisible(var));
                    if (var instanceof MutableType)
                    {
                        if (var instanceof VarMutable && var.getType() != null)
                        {
                            XMLUtil.setAttributeValue(varNode, "type", var.getType().getName());
                        }
                        else if (var instanceof VarMutableArray && var.getType() != null)
                        {
                            XMLUtil.setAttributeValue(varNode, "type", var.getType().getComponentType().getName());
                        }
                    }
                    XMLUtil.setAttributeBooleanValue(varNode, RUNTIME, blockData.inputVars.isRuntimeVariable(var));
                }

                Element outputVarRoot = XMLUtil.addElement(varRoot, "output");

                for (Var<?> var : blockData.outputVars)
                {
                    Element varNode = XMLUtil.addElement(outputVarRoot, "variable");
                    XMLUtil.setAttributeValue(varNode, Var.XML_KEY_ID, blockData.getVarID(var));
                    XMLUtil.setAttributeValue(varNode, "name", var.getName());
                    XMLUtil.setAttributeBooleanValue(varNode, "visible", blockData.outputVars.isVisible(var));
                    if (var instanceof MutableType)
                    {
                        if (var instanceof VarMutable && var.getType() != null)
                        {
                            XMLUtil.setAttributeValue(varNode, "type", var.getType().getName());
                        }
                        else if (var instanceof VarMutableArray && var.getType() != null)
                        {
                            XMLUtil.setAttributeValue(varNode, "type", var.getType().getComponentType().getName());
                        }
                    }
                    XMLUtil.setAttributeBooleanValue(varNode, RUNTIME, blockData.outputVars.isRuntimeVariable(var));
                }
            }
        }

        Element linkRoot = XMLUtil.addElement(workFlowRoot, "links");

        for (Link<?> link : workFlow.getLinksIterator())
        {
            Element linkNode = XMLUtil.addElement(linkRoot, "link");

            XMLUtil.setAttributeIntValue(linkNode, "srcBlockID", link.srcBlock.getID());
            XMLUtil.setAttributeValue(linkNode, "srcVarID", link.srcBlock.getVarID(link.srcVar));
            if (link.srcVar instanceof MutableType)
            {
                if (link.srcVar instanceof VarMutable && link.srcVar.getType() != null)
                {
                    XMLUtil.setAttributeValue(linkNode, "srcVarType", link.srcVar.getType().getName());
                }
                else if (link.srcVar instanceof VarMutableArray && link.srcVar.getType() != null)
                {
                    XMLUtil.setAttributeValue(linkNode, "srcVarType",
                            link.srcVar.getType().getComponentType().getName());
                }
            }
            XMLUtil.setAttributeIntValue(linkNode, "dstBlockID", link.dstBlock.getID());
            XMLUtil.setAttributeValue(linkNode, "dstVarID", link.dstBlock.getVarID(link.dstVar));
        }
    }

    /**
     * @param xml
     *        The xml document to load.
     * @param targetWorkFlow
     *        The workflow where elements generated from the xml document are to be inserted.
     * @return true if the loaded file is at the latest BlocksML version, false otherwise.
     */
    public synchronized boolean loadWorkFlow(Document xml, WorkFlow targetWorkFlow)
    {
        Element xmlRoot = XMLUtil.getRootElement(xml);

        String rootName = xmlRoot.getNodeName();

        if (!rootName.equalsIgnoreCase("protocol") && !rootName.equalsIgnoreCase("workspace"))
        {
            throw new IcyHandledException("The selected file is not an Icy protocol");
        }

        int fileVersion = XMLUtil.getAttributeIntValue(xmlRoot, "VERSION", -1);

        // scan the protocol for missing plugins
        Set<String> missingPlugins = getMissingPlugins(xmlRoot, fileVersion);

        if (missingPlugins.size() > 0)
        {
            // Are we connected to the internet?
            if (!NetworkUtil.hasInternetAccess())
                throw new IcyHandledException(
                        "Some plugins required by this protocol are missing,  but no internet connection is available");

            // Are we using Icy's plug-in loader (and not Eclipse)?
//            if (getClass().getClassLoader() == ClassLoader.getSystemClassLoader())
//                throw new IcyHandledException(
//                        "Cannot install missing blocks while using Icy4Eclipse in \"Debug\" or \"Run\" modes");

            // we are live!
            String message = "[Protocols] Installing required plugins...";
            System.out.println(message);
            AnnounceFrame announcement = null;

            if (!Icy.getMainInterface().isHeadLess())
                announcement = new AnnounceFrame(message);

            Set<PluginDescriptor> descriptors = new HashSet<PluginDescriptor>(missingPlugins.size());

            for (String className : missingPlugins)
            {
                PluginDescriptor pDesc = PluginRepositoryLoader.getPlugin(className);
                if (pDesc == null)
                {
                    // sadly, I believe the plugin loader could be reloaded at anytime...
                    // double check that for every plugin, just to be safe
                    if (PluginLoader.getPlugins(Block.class, true, false, false).size() == 0)
                        throw new BlocksReloadedException();

                    throw new BlocksException(
                            "Couldn't find plugin " + ClassUtil.getSimpleClassName(className) + " online", true);
                }
                descriptors.add(pDesc);
            }

            for (PluginDescriptor pDesc : descriptors)
                PluginInstaller.install(pDesc, false);

            PluginInstaller.waitInstall();
            PluginLoader.waitWhileLoading();

            System.out.println("[Protocols] Plugins installed successfully.");
            if (announcement != null)
                announcement.close();

            // reload the whole mother!
            throw new BlocksReloadedException();
        }

        switch (fileVersion)
        {
            case 1:
                loadWorkFlow_V1(xmlRoot, targetWorkFlow);
                break;
            case 2:
                loadWorkFlow_V2(xmlRoot, targetWorkFlow);
                break;
            case 3:
                loadWorkFlow_V3(xmlRoot, targetWorkFlow);
                break;
            case 4:
                loadWorkFlow_V4(xmlRoot, targetWorkFlow);
                break;
            default:
                throw new UnsupportedOperationException("Unknown Protocol version: " + fileVersion);
        }

        return fileVersion == CURRENT_VERSION;
    }

    private Set<String> getMissingPlugins(Element workFlowRoot, int fileVersion)
    {
        Set<String> missingPlugins = new HashSet<String>();

        Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        // we are not in a node that has blocks => stop the recursion
        if (blocksRoot == null)
            return missingPlugins;

        ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (Element blockNode : blocksNode)
        {
            // the class name of the block to find
            String blockTag = (fileVersion <= 2) ? "type" : "blockType";
            String blockType = XMLUtil.getAttributeValue(blockNode, blockTag, null);
            // the class name of the plugin providing this block (may be the same)
            String pluginClassName = XMLUtil.getAttributeValue(blockNode, "className", blockType);

            try
            {
                ClassUtil.findClass(blockType);
            }
            catch (ClassNotFoundException e)
            {
                // the block appears to be missing
                // => is the corresponding plug-in missing or out-dated?
                PluginDescriptor mainPlugin = PluginLoader.getPlugin(pluginClassName);
                if (mainPlugin == null || PluginUpdater.getUpdate(mainPlugin) != null)
                {
                    // store the class name of the missing plug-in
                    missingPlugins.add(pluginClassName);
                }
                else
                {
                    // The block is definitely missing, stop everything!
                    IcyExceptionHandler.handleException(mainPlugin, e, false);
                }
            }
            finally
            {
                // whether the current block is missing or not,
                // perhaps it's a work flow and has more blocks within
                missingPlugins.addAll(getMissingPlugins(blockNode, fileVersion));
            }
        }

        return missingPlugins;
    }

    /**
     * @deprecated legacy method. Use {@link #loadWorkFlow_V4(Element, WorkFlow)} instead.
     * @param workFlowRoot
     *        XML element of the workflow root to be loaded.
     * @param workFlow
     *        Target workflow to add elements from document.
     */
    @Deprecated
    @SuppressWarnings({"unchecked", "deprecation"})
    public synchronized void loadWorkFlow_V1(Element workFlowRoot, WorkFlow workFlow)
    {
        Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (Element blockNode : blocksNode)
        {
            String className = XMLUtil.getAttributeValue(blockNode, "type", null);

            int xPos = XMLUtil.getAttributeIntValue(blockNode, "xLocation", 0);
            int yPos = XMLUtil.getAttributeIntValue(blockNode, "yLocation", 0);

            try
            {
                Class<? extends Block> blockClass = (Class<? extends Block>) ClassUtil.findClass(className);

                Block block = blockClass.newInstance();

                if (block == null)
                    throw new BlocksException("Couldn't create block from class " + blockClass.getName(), true);

                if (block instanceof WorkFlow)
                {
                    // load the inner work flow
                    loadWorkFlow_V1(blockNode, (WorkFlow) block);

                    BlockDescriptor blockInfo = workFlow.addBlock(-1, block, new Point(xPos, yPos));

                    // adjust visibility triggers
                    {
                        Element varRoot = XMLUtil.getElement(blockNode, "variables");

                        Element inVarRoot = XMLUtil.getElement(varRoot, "input");

                        for (Element varNode : XMLUtil.getElements(inVarRoot))
                        {
                            int id = XMLUtil.getAttributeIntValue(varNode, "ID", -1);
                            Var<?> var = blockInfo.inputVars.get(id);
                            boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                            blockInfo.inputVars.setVisible(var, visible);
                        }
                        Element outVarRoot = XMLUtil.getElement(varRoot, "output");

                        for (Element varNode : XMLUtil.getElements(outVarRoot))
                        {
                            int id = XMLUtil.getAttributeIntValue(varNode, "ID", -1);
                            Var<?> var = blockInfo.outputVars.get(id);
                            boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                            blockInfo.outputVars.setVisible(var, visible);
                        }
                    }
                }
                else
                {
                    BlockDescriptor blockInfo = workFlow.addBlock(-1, block, new Point(xPos, yPos));

                    Element varRoot = XMLUtil.getElement(blockNode, "variables");
                    Element inVarRoot = XMLUtil.getElement(varRoot, "input");
                    for (Element varNode : XMLUtil.getElements(inVarRoot))
                    {
                        int id = XMLUtil.getAttributeIntValue(varNode, Var.XML_KEY_ID, -1);
                        Var<?> var = blockInfo.inputVars.get(id);
                        var.loadFromXML(varNode);
                        blockInfo.inputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));
                    }
                    Element outVarRoot = XMLUtil.getElement(varRoot, "output");
                    for (Element varNode : XMLUtil.getElements(outVarRoot))
                    {
                        int id = XMLUtil.getAttributeIntValue(varNode, Var.XML_KEY_ID, -1);
                        Var<?> var = blockInfo.outputVars.get(id);
                        var.loadFromXML(varNode);
                        blockInfo.outputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));
                    }
                }
            }
            catch (Exception e1)
            {
                throw new BlocksException("Cannot create block (" + e1.getMessage() + ")", true);
            }
        }

        Element linkRoot = XMLUtil.getElement(workFlowRoot, "links");

        for (Element linkNode : XMLUtil.getElements(linkRoot))
        {
            int srcBlockID = XMLUtil.getAttributeIntValue(linkNode, "srcBlockID", -1);
            int srcVarID = XMLUtil.getAttributeIntValue(linkNode, "srcVarID", -1);
            int dstBlockID = XMLUtil.getAttributeIntValue(linkNode, "dstBlockID", -1);
            int dstVarID = XMLUtil.getAttributeIntValue(linkNode, "dstVarID", -1);

            BlockDescriptor srcBlock = workFlow.getBlock(srcBlockID);
            @SuppressWarnings("rawtypes")
            // this is assumed correct
            Var srcVar = srcBlock.outputVars.get(srcVarID);

            BlockDescriptor dstBlock = workFlow.getBlock(dstBlockID);
            @SuppressWarnings("rawtypes")
            // this is assumed correct
            Var dstVar = dstBlock.inputVars.get(dstVarID);

            workFlow.addLink(srcBlock, srcVar, dstBlock, dstVar);
        }
    }

    /**
     * @deprecated legacy method. Use {@link #loadWorkFlow_V4(Element, WorkFlow)} instead.
     * @param workFlowRoot
     *        XML element of the workflow root to be loaded.
     * @param workFlow
     *        Target workflow to add elements from document.
     */
    @Deprecated
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    public synchronized void loadWorkFlow_V2(Element workFlowRoot, WorkFlow workFlow)
    {
        Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (Element blockNode : blocksNode)
        {
            String className = XMLUtil.getAttributeValue(blockNode, "type", null);

            int xPos = XMLUtil.getAttributeIntValue(blockNode, "xLocation", -1);
            int yPos = XMLUtil.getAttributeIntValue(blockNode, "yLocation", -1);

            try
            {
                Class<? extends Block> blockClass = (Class<? extends Block>) ClassUtil.findClass(className);

                Block block = blockClass.newInstance();

                if (block == null)
                    throw new BlocksException("Couldn't create block from class " + blockClass.getName(), true);

                if (block instanceof WorkFlow)
                {
                    BlockDescriptor blockInfo = workFlow.addBlock(-1, block, new Point(xPos, yPos));

                    // load the inner work flow
                    loadWorkFlow_V2(blockNode, (WorkFlow) block);

                    // adjust visibility triggers
                    {
                        Element varRoot = XMLUtil.getElement(blockNode, "variables");

                        Element inVarRoot = XMLUtil.getElement(varRoot, "input");

                        for (Element varNode : XMLUtil.getElements(inVarRoot))
                        {
                            String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                            Var<?> var = blockInfo.inputVars.get(uid);
                            boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                            blockInfo.inputVars.setVisible(var, visible);
                        }
                        Element outVarRoot = XMLUtil.getElement(varRoot, "output");

                        for (Element varNode : XMLUtil.getElements(outVarRoot))
                        {
                            String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                            Var<?> var = blockInfo.outputVars.get(uid);
                            boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                            blockInfo.outputVars.setVisible(var, visible);
                        }
                    }
                }
                else
                {
                    BlockDescriptor blockInfo = workFlow.addBlock(-1, block, new Point(xPos, yPos));

                    Element varRoot = XMLUtil.getElement(blockNode, "variables");
                    Element inVarRoot = XMLUtil.getElement(varRoot, "input");
                    for (Element varNode : XMLUtil.getElements(inVarRoot))
                    {
                        String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockInfo.inputVars.get(uid);
                        var.loadFromXML(varNode);
                        blockInfo.inputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));

                        if (var instanceof MutableType)
                        {
                            String type = XMLUtil.getAttributeValue(varNode, "type", null);
                            if (type != null)
                            {
                                if (var instanceof VarMutable)
                                {
                                    ((MutableType) var).setType(Class.forName(type));
                                }
                                else if (var instanceof VarMutableArray)
                                {
                                    ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                                }
                            }
                        }
                    }
                    Element outVarRoot = XMLUtil.getElement(varRoot, "output");
                    for (Element varNode : XMLUtil.getElements(outVarRoot))
                    {
                        String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockInfo.outputVars.get(uid);
                        var.loadFromXML(varNode);
                        blockInfo.outputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));

                        if (var instanceof MutableType)
                        {
                            String type = XMLUtil.getAttributeValue(varNode, "type", null);
                            if (type != null)
                            {
                                if (var instanceof VarMutable)
                                {
                                    ((MutableType) var).setType(Class.forName(type));
                                }
                                else if (var instanceof VarMutableArray)
                                {
                                    ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e1)
            {
                throw new BlocksException("Cannot create block (" + e1.getMessage() + ")", true);
            }
        }

        Element linkRoot = XMLUtil.getElement(workFlowRoot, "links");

        for (Element linkNode : XMLUtil.getElements(linkRoot))
        {
            int srcBlockID = XMLUtil.getAttributeIntValue(linkNode, "srcBlockID", -1);
            String srcVarID = XMLUtil.getAttributeValue(linkNode, "srcVarID", null);
            int dstBlockID = XMLUtil.getAttributeIntValue(linkNode, "dstBlockID", -1);
            String dstVarID = XMLUtil.getAttributeValue(linkNode, "dstVarID", null);

            BlockDescriptor srcBlock = workFlow.getBlock(srcBlockID);
            Var srcVar = srcBlock.outputVars.get(srcVarID);
            if (srcVar == null)
                srcVar = srcBlock.inputVars.get(srcVarID);

            BlockDescriptor dstBlock = workFlow.getBlock(dstBlockID);
            Var dstVar = dstBlock.inputVars.get(dstVarID);

            workFlow.addLink(srcBlock, srcVar, dstBlock, dstVar);
        }
    }

    /**
     * @deprecated legacy method. Use {@link #loadWorkFlow_V4(Element, WorkFlow)} instead.
     * @param workFlowRoot
     *        XML element of the workflow root to be loaded.
     * @param workFlow
     *        Target workflow to add elements from document.
     */
    @Deprecated
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    public synchronized void loadWorkFlow_V3(Element workFlowRoot, WorkFlow workFlow)
    {
        Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (Element blockNode : blocksNode)
        {
            String blockType = XMLUtil.getAttributeValue(blockNode, "blockType", null);

            int xPos = XMLUtil.getAttributeIntValue(blockNode, "xLocation", -1);
            int yPos = XMLUtil.getAttributeIntValue(blockNode, "yLocation", -1);

            try
            {
                Class<? extends Block> blockClass = (Class<? extends Block>) ClassUtil.findClass(blockType);

                Block block = blockClass.newInstance();

                if (block == null)
                    throw new BlocksException("Couldn't create block from class " + blockClass.getName(), true);

                if (block instanceof WorkFlow)
                {
                    int width = XMLUtil.getAttributeIntValue(blockNode, "width", 500);
                    int height = XMLUtil.getAttributeIntValue(blockNode, "height", 500);
                    ((WorkFlow) block).getBlockDescriptor().setDimension(width, height);

                    BlockDescriptor blockInfo = workFlow.addBlock(-1, block, new Point(xPos, yPos));

                    // load the inner work flow
                    loadWorkFlow_V3(blockNode, (WorkFlow) block);

                    // adjust visibility triggers
                    {
                        Element varRoot = XMLUtil.getElement(blockNode, "variables");

                        Element inVarRoot = XMLUtil.getElement(varRoot, "input");

                        for (Element varNode : XMLUtil.getElements(inVarRoot))
                        {
                            String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                            Var<?> var = blockInfo.inputVars.get(uid);
                            boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                            blockInfo.inputVars.setVisible(var, visible);
                        }
                        Element outVarRoot = XMLUtil.getElement(varRoot, "output");

                        for (Element varNode : XMLUtil.getElements(outVarRoot))
                        {
                            String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                            Var<?> var = blockInfo.outputVars.get(uid);
                            boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                            blockInfo.outputVars.setVisible(var, visible);
                        }
                    }
                }
                else
                {
                    BlockDescriptor blockInfo = workFlow.addBlock(-1, block, new Point(xPos, yPos));

                    Element varRoot = XMLUtil.getElement(blockNode, "variables");
                    Element inVarRoot = XMLUtil.getElement(varRoot, "input");
                    for (Element varNode : XMLUtil.getElements(inVarRoot))
                    {
                        String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockInfo.inputVars.get(uid);
                        var.loadFromXML(varNode);
                        blockInfo.inputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));

                        if (var instanceof MutableType)
                        {
                            String type = XMLUtil.getAttributeValue(varNode, "type", null);
                            if (type != null)
                            {
                                if (var instanceof VarMutable)
                                {
                                    ((MutableType) var).setType(Class.forName(type));
                                }
                                else if (var instanceof VarMutableArray)
                                {
                                    ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                                }
                            }
                        }
                    }
                    Element outVarRoot = XMLUtil.getElement(varRoot, "output");
                    for (Element varNode : XMLUtil.getElements(outVarRoot))
                    {
                        String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockInfo.outputVars.get(uid);
                        var.loadFromXML(varNode);
                        blockInfo.outputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));

                        if (var instanceof MutableType)
                        {
                            String type = XMLUtil.getAttributeValue(varNode, "type", null);
                            if (type != null)
                            {
                                if (var instanceof VarMutable)
                                {
                                    ((MutableType) var).setType(Class.forName(type));
                                }
                                else if (var instanceof VarMutableArray)
                                {
                                    ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e1)
            {
                throw new BlocksException("Cannot create block (" + e1.getMessage() + ")", true);
            }
        }

        Element linkRoot = XMLUtil.getElement(workFlowRoot, "links");

        for (Element linkNode : XMLUtil.getElements(linkRoot))
        {
            int srcBlockID = XMLUtil.getAttributeIntValue(linkNode, "srcBlockID", -1);
            String srcVarID = XMLUtil.getAttributeValue(linkNode, "srcVarID", null);
            int dstBlockID = XMLUtil.getAttributeIntValue(linkNode, "dstBlockID", -1);
            String dstVarID = XMLUtil.getAttributeValue(linkNode, "dstVarID", null);

            BlockDescriptor srcBlock = workFlow.getBlock(srcBlockID);
            Var srcVar = srcBlock.outputVars.get(srcVarID);
            if (srcVar == null)
                srcVar = srcBlock.inputVars.get(srcVarID);

            BlockDescriptor dstBlock = workFlow.getBlock(dstBlockID);
            Var dstVar = dstBlock.inputVars.get(dstVarID);

            workFlow.addLink(srcBlock, srcVar, dstBlock, dstVar);
        }
    }

    /**
     * @param workFlowRoot
     *        XML element of the workflow root to be loaded.
     * @param workFlow
     *        Target workflow to add elements from document.
     * @throws BlocksException
     *         If an error occurs while loading the workflow.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void loadWorkFlow_V4(Element workFlowRoot, final WorkFlow workFlow) throws BlocksException
    {
        boolean hasWarnings = false;

        // Prepare a list of blocks to collapse after loading
        ArrayList<BlockDescriptor> blocksToCollapse = new ArrayList<BlockDescriptor>();

        // 1) Load blocks and inner work flows

        Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (final Element blockNode : blocksNode)
        {
            final String blockType = XMLUtil.getAttributeValue(blockNode, "blockType", null);

            Block block = null;

            try
            {
                block = ThreadUtil.invokeNow(new Callable<Block>()
                {
                    @Override
                    public Block call() throws Exception
                    {
                        return (Block) ClassUtil.findClass(blockType).newInstance();
                    }
                });
            }
            catch (Exception e)
            {
                String message = "Couldn't create block from class \"" + blockType + "\".\n";
                message += "Reason: " + e.getClass().getName() + " (" + e.getMessage() + ")";

                throw new BlocksException(message, true);
            }

            int blockID = XMLUtil.getAttributeIntValue(blockNode, "ID", -1);

            final BlockDescriptor blockDescriptor;

            if (block instanceof WorkFlow)
            {
                blockDescriptor = ((WorkFlow) block).getBlockDescriptor();
                blockDescriptor.setID(blockID);
            }
            else
            {
                blockDescriptor = new BlockDescriptor(blockID, block);
            }

            int xPos = XMLUtil.getAttributeIntValue(blockNode, "xLocation", -1);
            int yPos = XMLUtil.getAttributeIntValue(blockNode, "yLocation", -1);
            blockDescriptor.setLocation(xPos, yPos);

            int width = XMLUtil.getAttributeIntValue(blockNode, "width", 0);
            int height = XMLUtil.getAttributeIntValue(blockNode, "height", 0);

            // set the block's dimension, but don't collapse just now
            // => wait for all the drawing to be done
            blockDescriptor.setDimension(width, height);

            String definedName = XMLUtil.getAttributeValue(blockNode, "definedName", null);
            blockDescriptor.setDefinedName(definedName);

            blockDescriptor.keepResults(XMLUtil.getAttributeBooleanValue(blockNode, "keepsResults", true));

            if (block instanceof InputBlock)
                blockDescriptor.setCommandLineID(XMLUtil.getAttributeValue(blockNode, "CommandLineID", ""));

            workFlow.addBlock(blockDescriptor);

            if (block instanceof WorkFlow)
            {
                // load the inner work flow
                loadWorkFlow_V4(blockNode, (WorkFlow) block);

                // adjust visibility triggers

                Element varRoot = XMLUtil.getElement(blockNode, "variables");
                Element inVarRoot = XMLUtil.getElement(varRoot, "input");
                final ArrayList<Element> inVarNodes = XMLUtil.getElements(inVarRoot);

                synchronized (blockDescriptor.inputVars)
                {
                    for (Element varNode : inVarNodes)
                    {
                        String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockDescriptor.inputVars.get(uid);

                        if (var == null)
                        {
                            System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                            continue;
                        }

                        if (var.getReference() == null)
                        {
                            var.loadFromXML(varNode);

                            // adjust visibility

                            boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                            try
                            {
                                blockDescriptor.inputVars.setVisible(var, visible);
                            }
                            catch (NoSuchVariableException e)
                            {
                                if (!hasWarnings)
                                {
                                    System.err.println("Error(s) while loading protocol:");
                                    hasWarnings = true;
                                }
                                System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                            }
                        }
                    }
                }

                Element outVarRoot = XMLUtil.getElement(varRoot, "output");
                final ArrayList<Element> outVarNodes = XMLUtil.getElements(outVarRoot);

                synchronized (blockDescriptor.outputVars)
                {
                    for (Element varNode : outVarNodes)
                    {
                        String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockDescriptor.outputVars.get(uid);

                        // adjust visibility

                        boolean visible = XMLUtil.getAttributeBooleanValue(varNode, "visible", false);
                        try
                        {
                            blockDescriptor.outputVars.setVisible(var, visible);
                        }
                        catch (NoSuchVariableException e)
                        {
                            if (!hasWarnings)
                            {
                                System.err.println("Error(s) while loading protocol:");
                                hasWarnings = true;
                            }
                            System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                            // throw new NoSuchVariableException(blockInfo, uid);
                        }
                    }
                }
            }
            else
            {
                Element varRoot = XMLUtil.getElement(blockNode, "variables");
                Element inVarRoot = XMLUtil.getElement(varRoot, "input");
                final ArrayList<Element> inVarNodes = XMLUtil.getElements(inVarRoot);

                synchronized (blockDescriptor.inputVars)
                {
                    for (Element varNode : inVarNodes)
                    {
                        String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockDescriptor.inputVars.get(uid);

                        boolean isDynamicVariable = XMLUtil.getAttributeBooleanValue(varNode, RUNTIME, false);

                        if (var == null)
                        {
                            if (isDynamicVariable)
                            {
                                var = new VarMutable(XMLUtil.getAttributeValue(varNode, "name", "input"), null);
                                ((VarMutable) var).setDefaultEditorModel(new TypeSelectionModel());
                                blockDescriptor.inputVars.addRuntimeVariable(uid, (VarMutable) var);
                            }
                            else
                            {
                                if (!hasWarnings)
                                {
                                    System.err.println("Error(s) while loading protocol:");
                                    hasWarnings = true;
                                }
                                System.err.println(new NoSuchVariableException(blockDescriptor, uid).getMessage());
                                continue;
                                // throw new NoSuchVariableException(blockInfo, uid);
                            }
                        }

                        try
                        {
                            if (var instanceof MutableType)
                            {
                                Class<?> mutableType = null;

                                String type = XMLUtil.getAttributeValue(varNode, "type", null);

                                if (type != null)
                                {
                                    if (var instanceof VarMutable)
                                    {
                                        mutableType = getPrimitiveType(type);

                                        if (mutableType == null)
                                            mutableType = Class.forName(type);
                                    }
                                    else if (var instanceof VarMutableArray)
                                    {
                                        mutableType = Class.forName("[L" + type + ";");
                                    }

                                    ((MutableType) var).setType(mutableType);
                                }
                            }

                            var.loadFromXML(varNode);

                            // Input blocks can have their values set from command line
                            if (block instanceof InputBlock)
                            {
                                String clID = blockDescriptor.getCommandLineID();
                                if (!clID.isEmpty())
                                {
                                    Map<String, String> map = Protocols.getCommandLineArguments();
                                    if (map.containsKey(clID))
                                    {
                                        // Set the value of the first (and only) block variable
                                        var.setValueAsString(map.get(clID));
                                    }
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            String message = "Unable to read input variable \"" + var.getName() + "\" in block \""
                                    + blockDescriptor.getDefinedName() + "\".\n";
                            message += "Reason: " + e.getClass().getName() + " (" + e.getMessage() + ")";
                            throw new BlocksException(message, true);
                        }
                        blockDescriptor.inputVars.setVisible(var,
                                XMLUtil.getAttributeBooleanValue(varNode, "visible", false));
                    }
                }

                Element outVarRoot = XMLUtil.getElement(varRoot, "output");
                final ArrayList<Element> outVarNodes = XMLUtil.getElements(outVarRoot);

                synchronized (blockDescriptor.outputVars)
                {
                    for (Element varNode : outVarNodes)
                    {
                        String uid = XMLUtil.getAttributeValue(varNode, "ID", null);
                        Var<?> var = blockDescriptor.outputVars.get(uid);

                        boolean isDynamicVariable = XMLUtil.getAttributeBooleanValue(varNode, RUNTIME, false);

                        if (var == null)
                        {
                            if (isDynamicVariable)
                            {
                                var = new VarMutable(XMLUtil.getAttributeValue(varNode, "name", "output"), null);
                                ((VarMutable) var).setDefaultEditorModel(new TypeSelectionModel());
                                blockDescriptor.outputVars.addRuntimeVariable(uid, (VarMutable) var);
                            }
                            else
                            {
                                if (!hasWarnings)
                                {
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
                            try
                            {
                                String type = XMLUtil.getAttributeValue(varNode, "type", null);
                                if (type != null)
                                {
                                    if (var instanceof VarMutable)
                                    {
                                        Class<?> mutableType = getPrimitiveType(type);
                                        ((MutableType) var)
                                                .setType(mutableType != null ? mutableType : Class.forName(type));
                                    }
                                    else if (var instanceof VarMutableArray)
                                    {
                                        ((MutableType) var).setType(Class.forName("[L" + type + ";"));
                                    }
                                }
                            }
                            catch (ClassNotFoundException e)
                            {
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

        Element linkRoot = XMLUtil.getElement(workFlowRoot, "links");

        ArrayList<Element> links = XMLUtil.getElements(linkRoot);
        Collections.sort(links, new Comparator<Element>()
        {
            @Override
            public int compare(Element link1, Element link2)
            {
                int srcBlock1 = XMLUtil.getAttributeIntValue(link1, "srcBlockID", -1);
                int srcBlock2 = XMLUtil.getAttributeIntValue(link2, "srcBlockID", -1);

                int i1 = workFlow.indexOf(workFlow.getBlockByID(srcBlock1));
                int i2 = workFlow.indexOf(workFlow.getBlockByID(srcBlock2));

                return i1 < i2 ? -1 : i1 > i2 ? 1 : 0;
            }
        });

        for (Element linkNode : links)
        {
            int srcBlockID = XMLUtil.getAttributeIntValue(linkNode, "srcBlockID", -1);
            String srcVarID = XMLUtil.getAttributeValue(linkNode, "srcVarID", null);
            int dstBlockID = XMLUtil.getAttributeIntValue(linkNode, "dstBlockID", -1);
            String dstVarID = XMLUtil.getAttributeValue(linkNode, "dstVarID", null);

            // load the source variable
            BlockDescriptor srcBlock = workFlow.getBlockByID(srcBlockID);
            Var srcVar = srcBlock.outputVars.get(srcVarID);
            if (srcVar == null)
                srcVar = srcBlock.inputVars.get(srcVarID);

            if (srcVar == null)
            {
                System.err.println("Cannot create a link from variable " + srcVarID + " (from block " + srcBlock
                        + "). It may have been removed or renamed.");
                continue;
            }

            if (srcVar instanceof MutableType)
            {
                String type = XMLUtil.getAttributeValue(linkNode, "srcVarType", null);
                if (type != null)
                {
                    try
                    {
                        if (srcVar instanceof VarMutable)
                        {
                            Class<?> mutableType = getPrimitiveType(type);
                            ((MutableType) srcVar).setType(mutableType != null ? mutableType : Class.forName(type));
                        }
                        else if (srcVar instanceof VarMutableArray)
                        {
                            ((MutableType) srcVar).setType(Class.forName("[L" + type + ";"));
                        }
                    }
                    catch (ClassNotFoundException e1)
                    {
                        throw new BlocksException("Cannot create block (" + e1.getMessage() + ") => class not found",
                                true);
                    }
                }
            }

            // load the destination variable
            BlockDescriptor dstBlock = workFlow.getBlockByID(dstBlockID);

            Var dstVar = dstBlock.inputVars.get(dstVarID);

            if (dstVar == null)
            {
                System.err.println("Cannot link to variable " + dstVarID + " (from block " + dstBlock
                        + "). It may have been removed or renamed.");
                continue;
            }

            workFlow.addLink(srcBlock, srcVar, dstBlock, dstVar);
        }

        // 3) Collapse necessary blocks
        for (BlockDescriptor block : blocksToCollapse)
            block.setCollapsed(true);

        if (hasWarnings)
            System.err.println("--");
    }

    public synchronized void loadWorkFlow_V5(Element workFlowRoot, WorkFlow workFlow, WorkFlow parentFlow)
            throws BlocksException
    {
        boolean noWarnings = true;

        Element blocksRoot = XMLUtil.getElement(workFlowRoot, "blocks");

        ArrayList<Element> blocksNode = XMLUtil.getElements(blocksRoot);

        for (Element blockNode : blocksNode)
        {
            BlockDescriptor blockDesc = new BlockDescriptor();
            noWarnings &= blockDesc.loadFromXML(blockNode);
            workFlow.addBlock(blockDesc);
        }

        Element linkRoot = XMLUtil.getElement(workFlowRoot, "links");

        for (Element linkNode : XMLUtil.getElements(linkRoot))
        {
            @SuppressWarnings("rawtypes")
            Link link = new Link(workFlow);
            link.loadFromXML(linkNode);
        }

        if (!noWarnings)
            System.err.println("--");
    }

    /**
     * @param primitiveName
     *        The string representation of the primitive type.
     * @return The Java primitive type represented by the given name, or null if the given name does
     *         not represent a primitive type
     */
    public static Class<?> getPrimitiveType(String primitiveName)
    {
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
