package plugins.adufour.blocks.tools.io;

import javax.swing.SwingUtilities;

import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import icy.swimmingPool.SwimmingObject;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarMutable;

/**
 * Sends any data to the swimming pool
 *
 * @author Alexandre Dufour
 */
public class SendToSwimmingPool extends Plugin implements Block {
    private final VarMutable data = new VarMutable("Data", null);

    @Override
    public void declareInput(VarList inputMap) {
        inputMap.add("Data", data);
    }

    @Override
    public void declareOutput(VarList outputMap) {
    }

    @Override
    public void run() {
        if (data.getValue() == null) return;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Icy.getMainInterface().getSwimmingPool().add(new SwimmingObject(data.getValue(), data.getReference().getName()));
            }
        });
    }

}
