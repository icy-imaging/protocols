package plugins.adufour.blocks.tools;

import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.util.VarListener;

public class Display extends Plugin implements Block {
    private final VarMutable object = new VarMutable("object", null);

    @Override
    public void run() {
        if (Icy.getMainInterface().isHeadLess()) {
            // Print to the standard output stream
            System.out.println(object.getValueAsString());
        }
    }

    @Override
    public void declareOutput(VarList outputMap) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("object", object);

        object.addListener(new VarListener<Object>() {
            @Override
            public void referenceChanged(Var<Object> source, Var<? extends Object> oldReference, Var<? extends Object> newReference) {
                if (newReference == null && !object.isReferenced()) object.setType(null);
            }

            @Override
            public void valueChanged(Var<Object> source, Object oldValue, Object newValue) {

            }
        });
    }
}
