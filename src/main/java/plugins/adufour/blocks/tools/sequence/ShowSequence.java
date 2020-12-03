package plugins.adufour.blocks.tools.sequence;

import icy.main.Icy;
import icy.plugin.abstract_.Plugin;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.VarSequence;

//@BlockSearchAnnotation(
//	name="show sequence",
//	category="sequence",
//	description="",
//	author="adufour"
//)
public class ShowSequence extends Plugin implements SequenceBlock
{
    VarSequence sequence = new VarSequence("sequence", null);
    
    @Override
    public void run()
    {
        if (sequence.getValue() != null) Icy.getMainInterface().addSequence(sequence.getValue());
    }
    
    @Override
    public void declareInput(VarList inputMap)
    {
        inputMap.add("sequence", sequence);
    }
    
    @Override
    public void declareOutput(VarList outputMap)
    {
    }
    
}
