package plugins.adufour.blocks.lang;

import java.util.List;

import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarInteger;

/**
 * Particular type of loop that will repeats its contents for every number within the provided range
 * of values
 *
 * @author Alexandre Dufour
 */
public class RangeLoop extends Loop {
    private VarInteger startIndex;
    private VarInteger endIndex;
    private VarInteger step;
    private VarInteger index;

    public VarInteger getEndIndex() {
        return endIndex;
    }

    public VarInteger getIndex() {
        return index;
    }

    public VarInteger getStartIndex() {
        return startIndex;
    }

    public VarInteger getStep() {
        return step;
    }

    @Override
    public void initializeLoop() {
        index.setValue(startIndex.getValue());
    }

    @Override
    public void beforeIteration() {
        // nothing to do here
    }

    @Override
    public void afterIteration() {
        index.setValue(index.getValue() + step.getValue());
    }

    @Override
    public boolean isStopConditionReached() {
        if (super.isStopConditionReached()) return true;

        if (step.getValue() > 0) return index.getValue() >= endIndex.getValue();

        return index.getValue() <= endIndex.getValue();
    }

    @Override
    public void declareInput(VarList inputMap) {
        super.declareInput(inputMap);

        inputMap.add("start", startIndex = new VarInteger("start", 0));
        inputMap.add("step", step = new VarInteger("step", 1));
        inputMap.add("end", endIndex = new VarInteger("end", 10));
    }

    @Override
    public void declareOutput(VarList outputMap) {
        super.declareOutput(outputMap);

        outputMap.add("index", index = new VarInteger("index", startIndex.getDefaultValue().intValue()));
    }

    @Override
    public void declareLoopVariables(List<Var<?>> loopVariables) {
        loopVariables.add(startIndex);
        loopVariables.add(endIndex);
        loopVariables.add(step);
        loopVariables.add(index);

        super.declareLoopVariables(loopVariables);
    }
}