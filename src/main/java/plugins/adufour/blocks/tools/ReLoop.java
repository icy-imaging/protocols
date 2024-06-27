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

package plugins.adufour.blocks.tools;

import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarMutable;

public class ReLoop extends Plugin implements ToolsBlock {
    public final VarMutable initValue = new VarMutable("init", null);

    public final VarMutable reloopValue = new VarMutable("reloop", null);

    public final VarMutable output = new VarMutable("output", null);

    @Override
    public void run() {
        // first run: output is null
        if (output.getValue() == null) {
            output.setValue(initValue.getValue());
        }
        else {
            output.setValue(reloopValue.getValue());
        }
    }

    @Override
    public void declareInput(final VarList inputMap) {
        inputMap.add("init", initValue);
        inputMap.add("reloop", reloopValue);

        initValue.addTypeChangeListener((source, oldType, newType) -> {
            if (newType != null) {
                reloopValue.setType(newType);
                output.setType(newType);
            }
        });

        reloopValue.addTypeChangeListener((source, oldType, newType) -> {
            if (newType != null) {
                initValue.setType(newType);
                output.setType(newType);
            }
        });
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        outputMap.add("output", output);
    }
}
