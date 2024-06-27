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

package plugins.adufour.protocols.gui.link;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.vars.lang.Var;

import java.awt.dnd.*;

/**
 * Button defining an area on a panel from where an output variable can be dragged to another panel
 *
 * @param <T> the type of the underlying variable to drag
 * @author Alexandre Dufour
 */
public class DragZone<T> extends DragDropZone implements DragGestureListener {
    private final DragGestureRecognizer dragGestureRecognizer;
    private final TransferableVar<T> transferable;

    // TODO: 21/03/2023  Why scope is unused ?
    @SuppressWarnings("unused")
    DragZone(final WorkFlow scope, final BlockDescriptor owner, final Var<T> variable) {
        super("Drag this arrow to create a link to another variable", variable);

        // component, action, listener
        this.dragGestureRecognizer = DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DRAGnDROP_ACTION, this);

        // prepare the data to transfer
        this.transferable = new TransferableVar<>(owner, variable);
    }

    @Override
    public void dispose() {
        super.dispose();

        dragGestureRecognizer.removeDragGestureListener(this);
        dragGestureRecognizer.setComponent(null);
        dragGestureRecognizer.resetRecognizer();
        transferable.dispose();
    }

    @Override
    public void dragGestureRecognized(final DragGestureEvent dge) {
        try {
            DragSource.getDefaultDragSource().startDrag(dge, DragSource.DefaultLinkDrop, transferable, null);
        }
        catch (final InvalidDnDOperationException idoe) {
            dge.startDrag(DragSource.DefaultLinkDrop, transferable, null);
        }
    }
}