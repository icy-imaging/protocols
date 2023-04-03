package plugins.adufour.blocks.tools;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarMutableArray;
import plugins.adufour.vars.util.VarListener;

/**
 * Utility block reading the specified index of an input array. This block uses mutable types to
 * receive any type of input and adjust the output accordingly
 *
 * @author Alexandre Dufour
 */
public class Iterator extends Plugin implements ToolsBlock {
    VarMutableArray array = new VarMutableArray("array", null);
    VarInteger index = new VarInteger("index", 0);
    VarBoolean end = new VarBoolean("end", false);
    VarMutable element = new VarMutable("element", null);

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

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("array", array);

        array.addListener(new VarListener() {
            @Override
            public void valueChanged(Var source, Object oldValue, Object newValue) {
            }

            @Override
            public void referenceChanged(Var source, Var oldReference, Var newReference) {
                element.setType(newReference == null ? null : newReference.getType().getComponentType());
            }
        });
    }

    @Override
    public void declareOutput(VarList outputMap) {
        outputMap.add("element", element);
        outputMap.add("index", index);
        outputMap.add("end", end);
    }

}
