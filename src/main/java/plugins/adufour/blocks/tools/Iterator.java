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

package plugins.adufour.blocks.tools;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.*;
import plugins.adufour.vars.util.VarListener;

/**
 * Utility block reading the specified index of an input array. This block uses mutable types to
 * receive any type of input and adjust the output accordingly
 *
 * @author Alexandre Dufour
 */
public class Iterator extends Plugin implements ToolsBlock {
    private final VarMutableArray array = new VarMutableArray("array", null);
    private final VarInteger index = new VarInteger("index", 0);
    private final VarBoolean end = new VarBoolean("end", false);
    private final VarMutable element = new VarMutable("element", null);

    @Override
    public void run() {
        if (array.getValue() != null && index.getValue() < array.size()) {
            element.setValue(array.getElementAt(index.getValue()));
            index.setValue(index.getValue() + 1);
        }
        else {
            end.setValue(true);
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("array", array);

        array.addListener(new VarListener<>() {
            @Override
            public void valueChanged(final Var source, final Object oldValue, final Object newValue) {
            }

            @Override
            public void referenceChanged(final Var source, final Var oldReference, final Var newReference) {
                element.setType(newReference == null ? null : newReference.getType().getComponentType());
            }
        });
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        outputMap.add("element", element);
        outputMap.add("index", index);
        outputMap.add("end", end);
    }

}
