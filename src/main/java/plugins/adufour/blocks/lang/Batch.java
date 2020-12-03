package plugins.adufour.blocks.lang;

import java.lang.reflect.Array;
import java.util.List;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarArray;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarMutableArray;
import plugins.adufour.vars.util.VarException;
import plugins.adufour.vars.util.VarListener;

/**
 * A batch is a particular type of work-flow that will execute repeatedly for every element of a
 * list of items, allowing to process all list items with a same work-flow. This is the most generic
 * type of batch available, however there are more user-friendly versions available, such as for
 * instance a {@link FileBatch File batch} or a {@link SequenceFileBatch Sequence file batch}.
 * 
 * @author Alexandre Dufour
 */
public class Batch extends Loop
{
    private VarMutableArray array;

    private VarMutable element;

    /**
     * Defines the variable used to determine the list of objects to batch process.<br>
     * By default, this variable is simply a {@link VarArray} containing the elements to process,
     * but overriding classes may provide special functionalities.
     * 
     * @see FileBatch#getBatchSource()
     * @see SequenceFileBatch#getBatchSource()
     * @see SequenceSeriesBatch#getBatchSource()
     * @return the variable used to define the contents of the batch
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Var<?> getBatchSource()
    {
        if (array == null)
        {
            array = new VarMutableArray("array", null);
            array.addListener(new VarListener()
            {
                @Override
                public void valueChanged(Var source, Object oldValue, Object newValue)
                {
                }

                @Override
                public void referenceChanged(Var source, Var oldReference, Var newReference)
                {
                    if (oldReference == null)
                    {
                        if (newReference != null)
                        {
                            element.setType(newReference.getType().getComponentType());
                        }
                    }
                    else if (element.isReferenced())
                    {
                        if (newReference != null && newReference.getType() != oldReference.getType())
                            throw new IllegalAccessError("Cannot change the type of a linked mutable variable");
                    }
                    else
                    {
                        if (newReference == null)
                        {
                            element.setType(null);
                            array.setType(null);
                        }
                        else
                        {
                            element.setType(newReference.getType().getComponentType());
                        }
                    }
                }
            });
        }
        return array;
    }

    /**
     * Defines the variable that will contain each element of the batch process, consecutively.
     * 
     * @see FileBatch#getBatchElement()
     * @see SequenceFileBatch#getBatchElement()
     * @see SequenceSeriesBatch#getBatchElement()
     * @return the inner variable that will contain each element to process
     */
    public Var<?> getBatchElement()
    {
        if (element == null)
        {
            // WARNING: do *not* change the name of this variable
            // why? see declareInput() below and VarList.add()
            element = new VarMutable("element", null);
        }

        return element;
    }

    @Override
    public void initializeLoop()
    {
        if (array == null || Array.getLength(array.getValue(true)) == 0)
            throw new VarException(array, "Cannot loop on an empty array");
    }

    @Override
    public void beforeIteration()
    {
        element.setValue(array.getElementAt(getIterationCounter().getValue()));
    }

    @Override
    public boolean isStopConditionReached()
    {
        return getIterationCounter().getValue() == Array.getLength(array.getValue());
    }

    @Override
    public void declareInput(VarList inputMap)
    {
        super.declareInput(inputMap);

        Var<?> batchSource = getBatchSource();

        inputMap.add(batchSource.getName(), getBatchSource());
    }

    @Override
    public void declareOutput(VarList outputMap)
    {
        super.declareOutput(outputMap);

        Var<?> batchElement = getBatchElement();

        outputMap.add(batchElement.getName(), batchElement);
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * The implementation provided in this class automatically declares the source and element of
     * the batch as loop variables, therefore they don't need to be declared by overriding classes
     */
    @Override
    public void declareLoopVariables(List<Var<?>> loopVariables)
    {
        loopVariables.add(getBatchSource());
        loopVariables.add(getBatchElement());
    }
}
