package plugins.adufour.blocks.tools;

import java.lang.reflect.Array;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.blocks.util.VarListListener;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarTrigger;
import plugins.adufour.vars.util.TypeChangeListener;
import plugins.adufour.vars.util.VarReferencingPolicy;

public class Accumulator extends Plugin implements ToolsBlock, TypeChangeListener {
    Class<?> type = null;

    VarList inputMap;

    VarMutable output = new VarMutable("output", null);

    @Override
    public void run() {
        int arrayLength = 0;

        for (Var<?> var : inputMap) {
            if (var.getValue() == null || var instanceof VarTrigger) continue;

            if (var.getValue().getClass().isArray()) {
                arrayLength += Array.getLength(var.getValue());
            }
            else arrayLength++;
        }

        Object array = Array.newInstance(type.getComponentType(), arrayLength);

        int i = 0;
        for (Var<?> var : inputMap) {
            if (var.getValue() == null || var instanceof VarTrigger) continue;

            if (var.getValue().getClass().isArray()) {
                int length = Array.getLength(var.getValue());
                System.arraycopy(var.getValue(), 0, array, i, length);
                i += length;
            }
            else Array.set(array, i++, var.getValue());
        }

        output.setValue(array);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void declareInput(final VarList theInputMap) {
        if (this.inputMap == null) {
            this.inputMap = theInputMap;
            VarTrigger trigger = new VarTrigger("Add variable", new VarTrigger.TriggerListener() {
                @Override
                public void valueChanged(Var<Integer> source, Integer oldValue, Integer newValue) {

                }

                @Override
                public void referenceChanged(Var<Integer> source, Var<? extends Integer> oldReference, Var<? extends Integer> newReference) {

                }

                @Override
                public void triggered(VarTrigger source) {
                    final VarMutable var = new VarMutable("input", type) {
                        public boolean isAssignableFrom(Var aVariable) {
                            if (this.type == null) return true;

                            if (super.isAssignableFrom(aVariable)) return true;

                            return type.getComponentType().isAssignableFrom(aVariable.getType());
                        }

                        ;
                    };
                    var.addTypeChangeListener(Accumulator.this);
                    theInputMap.addRuntimeVariable("" + var.hashCode(), var);
                }
            });

            trigger.setReferencingPolicy(VarReferencingPolicy.NONE);

            theInputMap.add("Add variable", trigger);

            theInputMap.addVarListListener(new VarListListener() {
                @Override
                public void variableRemoved(VarList list, Var variable) {
                }

                @Override
                public void variableAdded(VarList list, Var variable) {
                    if (variable instanceof VarMutable) {
                        ((VarMutable) variable).addTypeChangeListener(Accumulator.this);
                    }
                }
            });
        }
    }

    @Override
    public void declareOutput(VarList outputMap) {
        outputMap.add("output", output);
    }

    @Override
    public void typeChanged(Object source, Class<?> oldType, Class<?> newType) {
        if (type == null) {
            this.type = (newType.isArray() ? newType : Array.newInstance(newType, 0).getClass());

            output.setType(type);

            for (Var<?> var : inputMap) {
                if (var instanceof VarMutable) ((VarMutable) var).setType(type);
            }
        }
    }

}
