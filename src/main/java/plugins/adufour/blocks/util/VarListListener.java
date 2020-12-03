package plugins.adufour.blocks.util;

import plugins.adufour.vars.lang.Var;

public interface VarListListener
{
    void variableAdded(VarList list, Var<?> variable);
    
    void variableRemoved(VarList list, Var<?> variable);
}
