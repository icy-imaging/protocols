/*
 * Copyright (c) 2010-2026. Institut Pasteur.
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

package plugins.adufour.blocks.tools.text;

import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.model.sequence.Sequence;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.tools.ToolsBlock;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarObject;
import plugins.adufour.vars.lang.VarString;

public class AppendText extends Plugin implements ToolsBlock {
    private final VarObject in = new VarObject("input", null);
    private final VarString separator = new VarString("Separator", "_");
    private final VarObject suffix = new VarObject("Suffix", "");

    private final VarString out = new VarString("output", "");

    @Override
    public void run() {
        final Object i = in.getValue();

        final Object o = suffix.getValue();

        String prefix = i == null ? "" : i.toString();

        if (i instanceof Sequence) prefix = ((Sequence) i).getName();

        out.setValue(prefix + separator.getValue() + (o == null ? "" : o));
    }

    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("input", in);
        inputMap.add("Separator", separator);
        inputMap.add("Suffix", suffix);
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("output", out);
    }
}
