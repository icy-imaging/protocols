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

package plugins.adufour.blocks.util;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.vars.lang.Var;

public interface BlockListener {
    void blockCollapsed(BlockDescriptor block, boolean collapsed);

    void blockDimensionChanged(BlockDescriptor block, int newWidth, int newHeight);

    void blockLocationChanged(BlockDescriptor block, int newX, int newY);

    void blockStatusChanged(BlockDescriptor block, BlockStatus status);

    <T> void blockVariableChanged(BlockDescriptor block, Var<T> variable, T newValue);

    void blockVariableAdded(BlockDescriptor block, Var<?> variable);
}
