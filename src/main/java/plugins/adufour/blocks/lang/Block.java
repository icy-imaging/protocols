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

package plugins.adufour.blocks.lang;

import plugins.adufour.blocks.util.VarList;

/**
 * Interface indicating that implementing classes can be used in a block programming context
 *
 * @author Alexandre Dufour
 * @see WorkFlow
 */
public interface Block extends Runnable {
    /**
     * Fills the specified map with all the necessary input variables
     *
     * @param inputMap the list of input variables to fill
     */
    void declareInput(final VarList inputMap);

    /**
     * Fills the specified map with all the necessary output variables
     *
     * @param outputMap the list of output variables to fill
     */
    void declareOutput(final VarList outputMap);

    /**
     * Main method
     */
    @Override
    void run();
}
