/*
 * Copyright (c) 2010-2026. Institut Pasteur.
 *
 * This file is part of Icy.
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <https://www.gnu.org/licenses/>.
 */

package plugins.adufour.blocks.lang;

import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarInteger;

import java.util.List;

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
    public void declareInput(final VarList inputMap) {
        super.declareInput(inputMap);

        inputMap.add("start", startIndex = new VarInteger("start", 0));
        inputMap.add("step", step = new VarInteger("step", 1));
        inputMap.add("end", endIndex = new VarInteger("end", 10));
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        super.declareOutput(outputMap);

        outputMap.add("index", index = new VarInteger("index", startIndex.getDefaultValue()));
    }

    @Override
    public void declareLoopVariables(final @NotNull List<Var<?>> loopVariables) {
        loopVariables.add(startIndex);
        loopVariables.add(endIndex);
        loopVariables.add(step);
        loopVariables.add(index);

        super.declareLoopVariables(loopVariables);
    }
}