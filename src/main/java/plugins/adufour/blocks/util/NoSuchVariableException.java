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
import plugins.adufour.vars.lang.Var;

public class NoSuchVariableException extends BlocksException {
    public NoSuchVariableException(final String varName) {
        super("Variable not found: '" + varName + "'", false);
    }

    public NoSuchVariableException(final BlockDescriptor blockInfo, final String varName) {
        super("Variable '" + varName + "' not found in block '" + blockInfo.getName() + "'. It may have been removed or renamed.", false);
    }

    public NoSuchVariableException(final Var<?> src) {
        this(src == null ? null : src.getName());
    }
}
