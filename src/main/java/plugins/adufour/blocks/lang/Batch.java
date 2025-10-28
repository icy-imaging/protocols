/*
 * Copyright (c) 2010-2025. Institut Pasteur.
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
import plugins.adufour.vars.lang.VarArray;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.lang.VarMutableArray;
import plugins.adufour.vars.util.VarException;
import plugins.adufour.vars.util.VarListener;

import java.lang.reflect.Array;
import java.util.List;

/**
 * A batch is a particular type of work-flow that will execute repeatedly for every element of an
 * item list, allowing to process all list items with the same work-flow.<br>
 * This is the most generic type of batch available.<br>
 * However, there are more user-friendly versions available, such as for
 * instance a {@link FileBatch File batch} or a {@link SequenceFileBatch Sequence file batch}.
 *
 * @author Alexandre Dufour
 */
public class Batch extends Loop {
    private VarMutableArray array;

    private VarMutable element;

    /**
     * Defines the variable used to determine the list of objects to the batch process.<br>
     * By default, this variable is simply a {@link VarArray} containing the elements to process,
     * but overriding classes may provide special functionalities.
     *
     * @return the variable used to define the contents of the batch
     * @see FileBatch#getBatchSource()
     * @see SequenceFileBatch#getBatchSource()
     * @see SequenceSeriesBatch#getBatchSource()
     */
    @SuppressWarnings({"unchecked"})
    public Var<?> getBatchSource() {
        if (array == null) {
            array = new VarMutableArray("array", null);
            array.addListener(new VarListener<>() {
                @Override
                public void valueChanged(final Var source, final Object oldValue, final Object newValue) {
                }

                @Override
                public void referenceChanged(final Var source, final Var oldReference, final Var newReference) {
                    if (oldReference == null) {
                        if (newReference != null) {
                            element.setType(newReference.getType().getComponentType());
                        }
                    }
                    else if (element.isReferenced()) {
                        if (newReference != null && newReference.getType() != oldReference.getType())
                            throw new IllegalAccessError("Cannot change the type of a linked mutable variable");
                    }
                    else {
                        if (newReference == null) {
                            element.setType(null);
                            array.setType(null);
                        }
                        else {
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
     * @return the inner variable that will contain each element to process
     * @see FileBatch#getBatchElement()
     * @see SequenceFileBatch#getBatchElement()
     * @see SequenceSeriesBatch#getBatchElement()
     */
    public Var<?> getBatchElement() {
        if (element == null) {
            // WARNING: do *not* change the name of this variable
            // why? see declareInput() below and VarList.add()
            element = new VarMutable("element", null);
        }

        return element;
    }

    @Override
    public void initializeLoop() {
        if (array == null || Array.getLength(array.getValue(true)) == 0)
            throw new VarException(array, "Cannot loop on an empty array");
    }

    @Override
    public void beforeIteration() {
        element.setValue(array.getElementAt(getIterationCounter().getValue()));
    }

    @Override
    public boolean isStopConditionReached() {
        return getIterationCounter().getValue() == Array.getLength(array.getValue());
    }

    @Override
    public void declareInput(final VarList inputMap) {
        super.declareInput(inputMap);

        final Var<?> batchSource = getBatchSource();

        inputMap.add(batchSource.getName(), getBatchSource());
    }

    @Override
    public void declareOutput(final VarList outputMap) {
        super.declareOutput(outputMap);

        final Var<?> batchElement = getBatchElement();

        outputMap.add(batchElement.getName(), batchElement);
    }

    /**
     * {@inheritDoc} <br>
     * <br>
     * The implementation provided in this class automatically declares the source and element of
     * the batch as loop variables, therefore they don't need to be declared by overriding classes
     */
    @Override
    public void declareLoopVariables(final @NotNull List<Var<?>> loopVariables) {
        loopVariables.add(getBatchSource());
        loopVariables.add(getBatchElement());
    }
}
