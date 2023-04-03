package plugins.adufour.blocks.util;

import plugins.adufour.vars.lang.Var;

public interface VarVisibilityListener {
    void visibilityChanged(Var<?> source, boolean isVisible);
}
