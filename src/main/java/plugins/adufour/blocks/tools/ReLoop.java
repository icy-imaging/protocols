package plugins.adufour.blocks.tools;

import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.util.TypeChangeListener;

public class ReLoop extends Plugin implements ToolsBlock
{
    public final VarMutable initValue   = new VarMutable("init", null);
    
    public final VarMutable reloopValue = new VarMutable("reloop", null);
    
    public final VarMutable output      = new VarMutable("output", null);
    
    @Override
    public void run()
    {
        // first run: output is null
        if (output.getValue() == null)
        {
            output.setValue(initValue.getValue());
        }
        else
        {
            output.setValue(reloopValue.getValue());
        }
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("init", initValue);
        inputMap.add("reloop", reloopValue);
        
        initValue.addTypeChangeListener(new TypeChangeListener()
        {
            @Override
            public void typeChanged(Object source, Class<?> oldType, Class<?> newType)
            {
                if (newType != null)
                {
                    reloopValue.setType(newType);
                    output.setType(newType);
                }
            }
        });
        
        reloopValue.addTypeChangeListener(new TypeChangeListener()
        {
            @Override
            public void typeChanged(Object source, Class<?> oldType, Class<?> newType)
            {
                if (newType != null)
                {
                    initValue.setType(newType);
                    output.setType(newType);
                }
            }
        });
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("output", output);
    }
}
