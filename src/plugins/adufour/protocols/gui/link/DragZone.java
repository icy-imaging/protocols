package plugins.adufour.protocols.gui.link;

import java.awt.Image;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.InvalidDnDOperationException;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.vars.lang.Var;
import sun.awt.dnd.SunDragSourceContextPeer;

/**
 * Button defining an area on a panel from where an output variable can be dragged to another panel
 * 
 * @author Alexandre Dufour
 * @param <T>
 *        the type of the underlying variable to drag
 */
@SuppressWarnings("serial")
public class DragZone<T> extends DragDropZone implements DragGestureListener
{
    private final DragGestureRecognizer dragGestureRecognizer;
    private final TransferableVar<T> transferable;

    DragZone(Image image, WorkFlow scope, BlockDescriptor owner, Var<T> variable)
    {
        super("Drag this arrow to create a link to another variable", image, variable);

        // component, action, listener
        this.dragGestureRecognizer = DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this,
                DRAGnDROP_ACTION, this);

        // prepare the data to transfer
        this.transferable = new TransferableVar<T>(owner, variable);
    }

    public void dispose()
    {
        super.dispose();

        dragGestureRecognizer.removeDragGestureListener(this);
        dragGestureRecognizer.setComponent(null);
        dragGestureRecognizer.resetRecognizer();
        transferable.dispose();
    }

    @Override
    public void dragGestureRecognized(DragGestureEvent dge)
    {
        try
        {
            DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultLinkDrop, transferable, null);
        }
        catch (final InvalidDnDOperationException idoe)
        {
            try
            {
                SunDragSourceContextPeer.setDragDropInProgress(false);
            }
            catch (Throwable t)
            {

            }

            DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultLinkDrop, transferable, null);
        }
    }
}