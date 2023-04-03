package plugins.adufour.blocks.lang;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import plugins.adufour.blocks.util.BlocksException;
import plugins.adufour.blocks.util.BlocksML;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarMutableArray;
import plugins.adufour.vars.util.MutableType;

/**
 * Class describing a link between two variables within a work flow
 *
 * @author Alexandre Dufour
 */
public class Link<T> implements XMLPersistent {
    private final WorkFlow workFlow;

    public BlockDescriptor srcBlock;

    public Var<T> srcVar;

    public BlockDescriptor dstBlock;

    public Var<T> dstVar;

    /**
     * Creates a new (empty) link in the specified work flow. This constructor is generally followed
     * by a call to {@link #loadFromXML(Node)} to restore the link status from a previously saved
     * state (e.g. XML file)
     *
     * @param workFlow Workflow containing the new link.
     */
    public Link(WorkFlow workFlow) {
        this.workFlow = workFlow;
    }

    public Link(WorkFlow workFlow, final BlockDescriptor srcBlock, final Var<T> output, final BlockDescriptor dstBlock,
                final Var<T> input) {
        this(workFlow);

        this.srcBlock = srcBlock;
        this.srcVar = output;
        this.dstBlock = dstBlock;
        this.dstVar = input;
    }

    public Class<?> getType() {
        return srcVar.getType();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean loadFromXML(Node node) {
        Element linkNode = (Element) node;

        int srcBlockID = XMLUtil.getAttributeIntValue(linkNode, "srcBlockID", -1);
        String srcVarID = XMLUtil.getAttributeValue(linkNode, "srcVarID", null);
        int dstBlockID = XMLUtil.getAttributeIntValue(linkNode, "dstBlockID", -1);
        String dstVarID = XMLUtil.getAttributeValue(linkNode, "dstVarID", null);

        // load the source variable
        BlockDescriptor theSrcBlock = workFlow.getBlockByID(srcBlockID);
        Var theSrcVar = theSrcBlock.outputVars.get(srcVarID);
        if (theSrcVar == null)
            theSrcVar = theSrcBlock.inputVars.get(srcVarID);

        if (theSrcVar == null) {
            System.err.println("Cannot create a link from variable " + srcVarID + " (from block " + theSrcBlock + ")");
            return false;
        }

        if (theSrcVar instanceof MutableType) {
            String type = XMLUtil.getAttributeValue(linkNode, "srcVarType", null);
            if (type != null) {
                try {
                    if (theSrcVar instanceof VarMutable) {
                        Class<?> mutableType = BlocksML.getPrimitiveType(type);
                        ((MutableType) theSrcVar).setType(mutableType != null ? mutableType : Class.forName(type));
                    }
                    else if (theSrcVar instanceof VarMutableArray) {
                        type = "[L" + type + ";";
                        ((MutableType) theSrcVar).setType(Class.forName(type));
                    }
                }
                catch (ClassNotFoundException e1) {
                    throw new BlocksException("Cannot create link: unknown type " + type, true);
                }
            }
        }

        // load the destination variable
        BlockDescriptor theDstBlock = workFlow.getBlockByID(dstBlockID);

        Var theDstVar = theDstBlock.inputVars.get(dstVarID);

        workFlow.addLink(theSrcBlock, theSrcVar, theDstBlock, theDstVar);

        return true;
    }

    @Override
    public boolean saveToXML(Node node) {
        // TODO Auto-generated method stub
        return false;
    }
}
