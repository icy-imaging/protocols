package plugins.adufour.protocols.gui.link;

import java.awt.Image;
import java.awt.Point;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import icy.system.SystemUtil;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.vars.lang.Var;

/**
 * Button defining an area on a panel to where an variable can be dropped from another panel
 * 
 * @author Alexandre Dufour
 * @param <T>
 *            the type of the underlying variable to drop
 */
@SuppressWarnings("serial")
class DropZone<T> extends DragDropZone implements ActionListener, DropTargetListener
{
    private final DropTarget      dropTarget;
    private final JPopupMenu      errorPopup = new JPopupMenu();
    
    private final WorkFlow        workFlow;
    private final BlockDescriptor dstBlock;
    private final Var<T>          dstVar;
    
    DropZone(Image image, final WorkFlow workFlow, BlockDescriptor dstBlock, Var<T> dstVar)
    {
        super("Drop a variable here to create a link\nLeft-click: remove link (if any)", image, dstVar);
        
        addActionListener(this);
        
        this.dropTarget = new DropTarget(this, DRAGnDROP_ACTION, this, true);
        
        this.dstBlock = dstBlock;
        this.workFlow = workFlow;
        this.dstVar = dstVar;
        
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (dstVar.getReference() == null) return;
        
        workFlow.removeLink(dstVar);
        
        setDefaultLinkIcon();
    }
    
    public void dragEnter(DropTargetDragEvent e)
    {
        checkDrag(e);
    }
    
    public void dragOver(DropTargetDragEvent e)
    {
        checkDrag(e);
    }
    
    public void dropActionChanged(DropTargetDragEvent e)
    {
        checkDrag(e);
    }
    
    public void dragExit(DropTargetEvent e)
    {
        errorPopup.setVisible(false);
        errorPopup.removeAll();
        setDefaultLinkIcon();
    }
    
    @SuppressWarnings("unchecked")
    private void checkDrag(DropTargetDragEvent e)
    {
        try
        {
            TransferableVar<T> transfer = (TransferableVar<T>) e.getTransferable().getTransferData(TransferableVar.varFlavor);
            
            // create the link without actually storing it (to check for errors)
            workFlow.checkLink(transfer.getSrcBlock(), transfer.getSrcVar(), dstBlock, dstVar);
            
            e.acceptDrag(DRAGnDROP_ACTION);
        }
        catch (Exception exception)
        {
            setErrorLinkIcon();
            e.rejectDrag();
            
            if (!errorPopup.isVisible())
            {
                errorPopup.add(new JLabel("<html><h4>" + exception.getLocalizedMessage().replace("\n", "<br/>") + "</h4></html>"));
                Point location = getLocationOnScreen();
                errorPopup.setLocation(location.x + 10, location.y + 20);
                errorPopup.setVisible(true);
            }
        }
        catch (final Error error)
        {
            setErrorLinkIcon();
            e.rejectDrag();
            
            if (!errorPopup.isVisible())
            {
                errorPopup.add(new JLabel("<html><h4>" + error.getClass().getSimpleName() + ": " + error.getLocalizedMessage().replace("\n", "<br/>") + "</h4></html>"));
                Point location = getLocationOnScreen();
                errorPopup.setLocation(location.x + 10, location.y + 20);
                errorPopup.setVisible(true);
                
                error.printStackTrace();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent e)
    {
        try
        {
            TransferableVar<T> transfer = (TransferableVar<T>) e.getTransferable().getTransferData(TransferableVar.varFlavor);
            
            if (dstVar.getReference() != null) workFlow.removeLink(dstVar);
            
            workFlow.addLink(transfer.getSrcBlock(), transfer.getSrcVar(), dstBlock, dstVar);
            
            e.dropComplete(true);
        }
        catch (Exception e1)
        {
            JOptionPane.showMessageDialog(null, e1.getMessage() + "\n(stack trace will be sent to the console)", "Error", JOptionPane.ERROR_MESSAGE);
            e1.printStackTrace();
            e.dropComplete(false);
        }
    }
    
    public void dispose()
    {
        super.dispose();
        
        removeActionListener(this);
        dropTarget.removeDropTargetListener(this);
        dropTarget.setComponent(null);
        
        // only available on java < 10
        if (SystemUtil.getJavaVersionAsNumber() < 10d)
            dropTarget.getDropTargetContext().removeNotify();
    }
}
