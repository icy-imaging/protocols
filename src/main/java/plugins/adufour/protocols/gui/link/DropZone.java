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

package plugins.adufour.protocols.gui.link;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.vars.lang.Var;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Button defining an area on a panel to where an variable can be dropped from another panel
 *
 * @param <T> the type of the underlying variable to drop
 * @author Alexandre Dufour
 */
class DropZone<T> extends DragDropZone implements ActionListener, DropTargetListener {
    private final DropTarget dropTarget;
    private final JPopupMenu errorPopup = new JPopupMenu();

    private final WorkFlow workFlow;
    private final BlockDescriptor dstBlock;
    private final Var<T> dstVar;

    DropZone(final WorkFlow workFlow, final BlockDescriptor dstBlock, final Var<T> dstVar) {
        super("Drop a variable here to create a link\nLeft-click: remove link (if any)", dstVar);

        addActionListener(this);

        this.dropTarget = new DropTarget(this, DRAGnDROP_ACTION, this, true);

        this.dstBlock = dstBlock;
        this.workFlow = workFlow;
        this.dstVar = dstVar;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (dstVar.getReference() == null) return;

        workFlow.removeLink(dstVar);

        setDefaultLinkIcon();
    }

    @Override
    public void dragEnter(final DropTargetDragEvent e) {
        checkDrag(e);
    }

    @Override
    public void dragOver(final DropTargetDragEvent e) {
        checkDrag(e);
    }

    @Override
    public void dropActionChanged(final DropTargetDragEvent e) {
        checkDrag(e);
    }

    @Override
    public void dragExit(final DropTargetEvent e) {
        errorPopup.setVisible(false);
        errorPopup.removeAll();
        setDefaultLinkIcon();
    }

    @SuppressWarnings("unchecked")
    private void checkDrag(final DropTargetDragEvent e) {
        try {
            final TransferableVar<T> transfer = (TransferableVar<T>) e.getTransferable().getTransferData(TransferableVar.varFlavor);

            // create the link without actually storing it (to check for errors)
            workFlow.checkLink(transfer.getSrcBlock(), transfer.getSrcVar(), dstBlock, dstVar);

            e.acceptDrag(DRAGnDROP_ACTION);
        }
        catch (final Exception exception) {
            setErrorLinkIcon();
            e.rejectDrag();

            if (!errorPopup.isVisible()) {
                errorPopup.add(new JLabel("<html><h4>" + exception.getLocalizedMessage().replace("\n", "<br/>") + "</h4></html>"));
                final Point location = getLocationOnScreen();
                errorPopup.setLocation(location.x + 10, location.y + 20);
                errorPopup.setVisible(true);
            }
        }
        catch (final Error error) {
            setErrorLinkIcon();
            e.rejectDrag();

            if (!errorPopup.isVisible()) {
                errorPopup.add(new JLabel("<html><h4>" + error.getClass().getSimpleName() + ": " + error.getLocalizedMessage().replace("\n", "<br/>") + "</h4></html>"));
                final Point location = getLocationOnScreen();
                errorPopup.setLocation(location.x + 10, location.y + 20);
                errorPopup.setVisible(true);

                error.printStackTrace();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void drop(final DropTargetDropEvent e) {
        try {
            final TransferableVar<T> transfer = (TransferableVar<T>) e.getTransferable().getTransferData(TransferableVar.varFlavor);

            if (dstVar.getReference() != null) workFlow.removeLink(dstVar);

            workFlow.addLink(transfer.getSrcBlock(), transfer.getSrcVar(), dstBlock, dstVar);

            e.dropComplete(true);
        }
        catch (final Exception e1) {
            JOptionPane.showMessageDialog(null, e1.getMessage() + "\n(stack trace will be sent to the console)", "Error", JOptionPane.ERROR_MESSAGE);
            e1.printStackTrace();
            e.dropComplete(false);
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        removeActionListener(this);
        dropTarget.removeDropTargetListener(this);
        dropTarget.setComponent(null);
    }
}
