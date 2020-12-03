package plugins.adufour.blocks.tools.text;

import icy.plugin.abstract_.Plugin;
import icy.plugin.interface_.PluginBundled;
import icy.sequence.Sequence;
import plugins.adufour.blocks.tools.ToolsBlock;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.vars.lang.VarObject;
import plugins.adufour.vars.lang.VarString;

public class AppendText extends Plugin implements ToolsBlock, PluginBundled
{
    VarObject in        = new VarObject("input", null);
    VarString separator = new VarString("Separator", "_");
    VarObject suffix    = new VarObject("Suffix", "");
    
    VarString out       = new VarString("output", "");
    
    @Override
    public void run()
    {
        Object i = in.getValue();
        
        Object o = suffix.getValue();
        
        String prefix = i == null ? "" : i.toString();
        
        if (i instanceof Sequence) prefix = ((Sequence) i).getName();
        
        out.setValue(prefix + separator.getValue() + (o == null ? "" : o));
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("input", in);
        inputMap.add("Separator", separator);
        inputMap.add("Suffix", suffix);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
        outputMap.add("output", out);
    }
    
    @Override
    public String getMainPluginClassName()
    {
        return Protocols.class.getName();
    }
}
