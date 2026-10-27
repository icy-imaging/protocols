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

package plugins.adufour.blocks.tools;

import fr.icy.Icy;
import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.extension.plugin.annotation_.IcyPluginCategory;
import fr.icy.extension.plugin.annotation_.IcyPluginName;
import fr.icy.extension.plugin.annotation_.IcyPluginTags;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.util.VarListener;

import java.util.logging.Level;
import java.util.logging.Logger;

@IcyPluginName("Display")
@IcyPluginCategory("Protocols")
@IcyPluginTags({"Protocols", "Tool", "Display"})
public class Display extends Plugin implements Block {
    private static final Logger LOGGER = Logger.getLogger(Display.class.getName());

    private final VarMutable object = new VarMutable("object", null);

    @Override
    public void run() {
        if (Icy.getMainInterface().isHeadLess() && LOGGER.isLoggable(Level.INFO)) {
            // Print to the standard output stream
            LOGGER.log(Level.INFO, object.getValueAsString());
        }
    }

    @Override
    public void declareOutput(final VarList outputMap) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void declareInput(final @NotNull VarList inputMap) {
        inputMap.add("object", object);

        object.addListener(new VarListener<>() {
            @Override
            public void referenceChanged(final Var<Object> source, final Var<?> oldReference, final Var<?> newReference) {
                if (newReference == null && !object.isReferenced()) object.setType(null);
            }

            @Override
            public void valueChanged(final Var<Object> source, final Object oldValue, final Object newValue) {

            }
        });
    }
}
