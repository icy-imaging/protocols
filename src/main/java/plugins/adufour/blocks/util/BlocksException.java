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

/**
 * Convenience class for the Blocks framework that allows optimizing bug finding / reporting /
 * fixing.
 *
 * @author Alexandre Dufour
 */
public class BlocksException extends RuntimeException {
    public final boolean catchException;

    /**
     * Creates a new exception with the specified message and catching behavior.
     *
     * @param message        the error message bound the current exception
     * @param catchException true, if the exception should be caught within the EzPlug layer, false to let the
     *                       exception pass to the global exception manager
     */
    public BlocksException(final String message, final boolean catchException) {
        super(message);
        this.catchException = catchException;
    }
}
