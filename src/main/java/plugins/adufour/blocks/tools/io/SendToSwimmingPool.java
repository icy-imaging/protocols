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

package plugins.adufour.blocks.tools.io;

import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.swimmingPool.SwimmingObject;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarMutable;

import javax.swing.*;

/**
 * Sends any data to the swimming pool
 *
 * @author Alexandre Dufour
 */
public class SendToSwimmingPool extends Plugin implements Block {
    private final VarMutable data = new VarMutable("Data", null);

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("Data", data);
    }

    @Override
    public void declareOutput(final VarList outputMap) {
    }

    @Override
    public void run() {
        if (data.getValue() == null) return;

        SwingUtilities.invokeLater(() -> Icy.getMainInterface().getSwimmingPool().add(new SwimmingObject(data.getValue(), data.getReference().getName())));
    }

}
