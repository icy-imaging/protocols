package plugins.adufour.protocols.gui.link;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.vars.lang.Var;

public class TransferableVar<T> implements Transferable
{
    public static final DataFlavor varFlavor = new DataFlavor(Var.class, "Var");

    private BlockDescriptor        srcBlock;

    private Var<T>                 srcVar;

    public TransferableVar(BlockDescriptor owner, Var<T> variable)
    {
        this.srcBlock = owner;
        this.srcVar = variable;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return new DataFlavor[] { varFlavor };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return flavor.equals(varFlavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
    {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);

        return this;
    }

    public BlockDescriptor getSrcBlock()
    {
        return srcBlock;
    }

    public Var<T> getSrcVar()
    {
        return srcVar;
    }

    /**
     * This method is necessary since this instance of Transferable is never garbage collected (a
     * global JNI reference is kept by the OS due to previous drag'n'drop operations.
     */
    public void dispose()
    {
        srcBlock = null;
        srcVar = null;
    }
}
