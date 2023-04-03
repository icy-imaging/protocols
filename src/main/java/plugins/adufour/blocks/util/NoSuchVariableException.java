package plugins.adufour.blocks.util;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.vars.lang.Var;

public class NoSuchVariableException extends BlocksException {
    public NoSuchVariableException(String varName) {
        super("Variable not found: '" + varName + "'", false);
    }

    public NoSuchVariableException(BlockDescriptor blockInfo, String varName) {
        super("Variable '" + varName + "' not found in block '" + blockInfo.getName() + "'. It may have been removed or renamed.", false);
    }

    public NoSuchVariableException(Var<?> src) {
        this(src == null ? null : src.getName());
    }
}
