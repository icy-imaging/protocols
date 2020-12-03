package plugins.adufour.blocks.util;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.vars.lang.Var;

public interface BlockListener
{
    void blockCollapsed(BlockDescriptor block, boolean collapsed);
    
    void blockDimensionChanged(BlockDescriptor block, int newWidth, int newHeight);
    
    void blockLocationChanged(BlockDescriptor block, int newX, int newY);

    void blockStatusChanged(BlockDescriptor block, BlockStatus status);
    
    <T> void blockVariableChanged(BlockDescriptor block, Var<T> variable, T newValue);
    
    void blockVariableAdded(BlockDescriptor block, Var<?> variable);
}
