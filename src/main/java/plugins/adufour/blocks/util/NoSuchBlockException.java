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

package plugins.adufour.blocks.util;

import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;

public class NoSuchBlockException extends BlocksException {
    public NoSuchBlockException(final BlockDescriptor block) {
        super("Cannot find block '" + (block == null ? "null" : block.getName()) + "'", false);
    }

    public NoSuchBlockException(final int ID) {
        super("Cannot find block with ID '" + ID + "'", false);
    }

    public NoSuchBlockException(final int ID, final @NotNull WorkFlow container) {
        super("Cannot find block with ID '" + ID + "' in workflow '" + container.getBlockDescriptor().getName() + "'", false);
    }
}
