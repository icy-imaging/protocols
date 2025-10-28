/*
 * Copyright (c) 2010-2025. Institut Pasteur.
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

import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.vars.lang.Var;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class TransferableVar<T> implements Transferable {
    public static final DataFlavor varFlavor = new DataFlavor(Var.class, "Var");

    private BlockDescriptor srcBlock;

    private Var<T> srcVar;

    public TransferableVar(final BlockDescriptor owner, final Var<T> variable) {
        this.srcBlock = owner;
        this.srcVar = variable;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{varFlavor};
    }

    @Override
    public boolean isDataFlavorSupported(final @NotNull DataFlavor flavor) {
        return flavor.equals(varFlavor);
    }

    @Override
    public @NotNull Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);

        return this;
    }

    public BlockDescriptor getSrcBlock() {
        return srcBlock;
    }

    public Var<T> getSrcVar() {
        return srcVar;
    }

    /**
     * This method is necessary since this instance of Transferable is never garbage collected
     * (the OS keeps a global JNI reference due to previous drag n drop operations).
     */
    public void dispose() {
        srcBlock = null;
        srcVar = null;
    }
}
