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

import fr.icy.extension.plugin.abstract_.Plugin;
import fr.icy.extension.plugin.annotation_.IcyPluginCategory;
import fr.icy.extension.plugin.annotation_.IcyPluginName;
import fr.icy.extension.plugin.annotation_.IcyPluginTags;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.blocks.util.VarListListener;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarTrigger;
import plugins.adufour.vars.util.TypeChangeListener;
import plugins.adufour.vars.util.VarReferencingPolicy;

import java.lang.reflect.Array;

@IcyPluginName("Accumulator")
@IcyPluginCategory("Protocols")
@IcyPluginTags({"Protocols", "Tool", "Accumulate"})
public class Accumulator extends Plugin implements ToolsBlock, TypeChangeListener {
    private Class<?> type = null;

    private VarList inputMap;

    private final VarMutable output = new VarMutable("output", null);

    @SuppressWarnings("SuspiciousSystemArraycopy")
    @Override
    public void run() {
        int arrayLength = 0;

        for (final Var<?> var : inputMap) {
            if (var.getValue() == null || var instanceof VarTrigger) continue;

            if (var.getValue().getClass().isArray()) {
                arrayLength += Array.getLength(var.getValue());
            }
            else arrayLength++;
        }

        final Object array = Array.newInstance(type.getComponentType(), arrayLength);

        int i = 0;
        for (final Var<?> var : inputMap) {
            if (var.getValue() == null || var instanceof VarTrigger) continue;

            if (var.getValue().getClass().isArray()) {
                final int length = Array.getLength(var.getValue());
                System.arraycopy(var.getValue(), 0, array, i, length);
                i += length;
            }
            else Array.set(array, i++, var.getValue());
        }

        output.setValue(array);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void declareInput(final VarList theInputMap) {
        if (this.inputMap == null) {
            this.inputMap = theInputMap;
            final VarTrigger trigger = new VarTrigger("Add variable", new VarTrigger.TriggerListener() {
                @Override
                public void valueChanged(final Var<Integer> source, final Integer oldValue, final Integer newValue) {

                }

                @Override
                public void referenceChanged(final Var<Integer> source, final Var<? extends Integer> oldReference, final Var<? extends Integer> newReference) {

                }

                @Override
                public void triggered(final VarTrigger source) {
                    final VarMutable var = new VarMutable("input", type) {
                        @Override
                        public boolean isAssignableFrom(final Var aVariable) {
                            if (this.type == null) return true;

                            if (super.isAssignableFrom(aVariable)) return true;

                            return type.getComponentType().isAssignableFrom(aVariable.getType());
                        }
                    };
                    var.addTypeChangeListener(Accumulator.this);
                    theInputMap.addRuntimeVariable(String.valueOf(var.hashCode()), var);
                }
            });

            trigger.setReferencingPolicy(VarReferencingPolicy.NONE);

            theInputMap.add("Add variable", trigger);

            theInputMap.addVarListListener(new VarListListener() {
                @Override
                public void variableRemoved(final VarList list, final Var<?> variable) {
                }

                @Override
                public void variableAdded(final VarList list, final Var<?> variable) {
                    if (variable instanceof VarMutable) {
                        ((VarMutable) variable).addTypeChangeListener(Accumulator.this);
                    }
                }
            });
        }
    }

    @Override
    public void declareOutput(final @NotNull VarList outputMap) {
        outputMap.add("output", output);
    }

    @Override
    public void typeChanged(final Object source, final Class<?> oldType, final Class<?> newType) {
        if (type == null) {
            this.type = (newType.isArray() ? newType : Array.newInstance(newType, 0).getClass());

            output.setType(type);

            for (final Var<?> var : inputMap) {
                if (var instanceof VarMutable) ((VarMutable) var).setType(type);
            }
        }
    }

}
