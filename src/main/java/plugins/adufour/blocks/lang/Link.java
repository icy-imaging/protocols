/*
 * Copyright (c) 2010-2024. Institut Pasteur.
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

import org.bioimageanalysis.icy.io.xml.XMLPersistent;
import org.bioimageanalysis.icy.io.xml.XMLUtil;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
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
    public Link(final WorkFlow workFlow) {
        this.workFlow = workFlow;
    }

    public Link(final WorkFlow workFlow, final BlockDescriptor srcBlock, final Var<T> output, final BlockDescriptor dstBlock, final Var<T> input) {
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
    public boolean loadFromXML(final Node node) {
        final Element linkNode = (Element) node;

        final int srcBlockID = XMLUtil.getAttributeIntValue(linkNode, "srcBlockID", -1);
        final String srcVarID = XMLUtil.getAttributeValue(linkNode, "srcVarID", null);
        final int dstBlockID = XMLUtil.getAttributeIntValue(linkNode, "dstBlockID", -1);
        final String dstVarID = XMLUtil.getAttributeValue(linkNode, "dstVarID", null);

        // load the source variable
        final BlockDescriptor theSrcBlock = workFlow.getBlockByID(srcBlockID);
        Var theSrcVar = theSrcBlock.outputVars.get(srcVarID);
        if (theSrcVar == null)
            theSrcVar = theSrcBlock.inputVars.get(srcVarID);

        if (theSrcVar == null) {
            IcyLogger.error(Link.class, "Cannot create a link from variable " + srcVarID + " (from block " + theSrcBlock + ")");
            return false;
        }

        if (theSrcVar instanceof MutableType) {
            String type = XMLUtil.getAttributeValue(linkNode, "srcVarType", null);
            if (type != null) {
                try {
                    if (theSrcVar instanceof VarMutable) {
                        final Class<?> mutableType = BlocksML.getPrimitiveType(type);
                        ((MutableType) theSrcVar).setType(mutableType != null ? mutableType : Class.forName(type));
                    }
                    else if (theSrcVar instanceof VarMutableArray) {
                        type = "[L" + type + ";";
                        ((MutableType) theSrcVar).setType(Class.forName(type));
                    }
                }
                catch (final ClassNotFoundException e1) {
                    throw new BlocksException("Cannot create link: unknown type " + type, true);
                }
            }
        }

        // load the destination variable
        final BlockDescriptor theDstBlock = workFlow.getBlockByID(dstBlockID);

        final Var theDstVar = theDstBlock.inputVars.get(dstVarID);

        workFlow.addLink(theSrcBlock, theSrcVar, theDstBlock, theDstVar);

        return true;
    }

    @Override
    public boolean saveToXML(final Node node) {
        return false;
    }
}
