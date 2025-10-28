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

package plugins.adufour.blocks.util;

import org.bioimageanalysis.icy.extension.plugin.abstract_.Plugin;
import org.bioimageanalysis.icy.model.sequence.Sequence;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.vars.lang.*;

import java.io.File;
import java.lang.annotation.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Utility class that parses block annotations and generates blocks from valid methods
 *
 * @author Alexandre Dufour
 * @see BlockInput
 * @see BlockMethod
 */
public class BlockAnnotations {
    /**
     * Annotation used to mark a method as block-compatible
     *
     * @author Alexandre Dufour
     * @see BlockAnnotations
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface BlockMethod {
        /**
         * @return The method's description
         */
        String value();
    }

    /**
     * Annotation used to mark a method parameter as a block input parameter
     *
     * @author Alexandre Dufour
     * @see BlockAnnotations
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface BlockInput {
        String value();
    }

    public static Class<?> getInnerType(final Class<? extends Var<?>> variableClass) {
        if (variableClass == VarInteger.class) return Integer.class;

        if (variableClass == VarDouble.class) return Double.class;

        if (variableClass == VarFloat.class) return Float.class;

        if (variableClass == VarBoolean.class) return Boolean.class;

        if (variableClass == VarFile.class) return File.class;

        if (variableClass == VarSequence.class) return Sequence.class;

        if (variableClass == VarString.class) return String.class;

        throw new UnsupportedOperationException("Unsupported type: " + variableClass.getSimpleName());
    }

    public static Class<? extends Var<?>> getVariableType(final Class<?> clazz) {
        if (clazz == Integer.class) return VarInteger.class;

        if (clazz == Double.class) return VarDouble.class;

        if (clazz == Float.class) return VarFloat.class;

        if (clazz == Boolean.class) return VarBoolean.class;

        if (clazz == File.class) return VarFile.class;

        if (clazz == Sequence.class) return VarSequence.class;

        if (clazz == String.class) return VarString.class;

        throw new UnsupportedOperationException("Unsupported type: " + clazz.getSimpleName());
    }

    public static @NotNull ArrayList<BlockAnnotations> findBlockMethods(final Class<? extends Plugin> clazz) {
        final ArrayList<BlockAnnotations> blockDescriptors = new ArrayList<>();

        final Method[] methods = clazz.getDeclaredMethods();

        for (final Method method : methods) {
            // make sure the method is static and annotated

            if (!Modifier.isStatic(method.getModifiers())) continue;

            final BlockMethod blockFunction = method.getAnnotation(BlockMethod.class);
            if (blockFunction == null) continue;

            blockDescriptors.add(new BlockAnnotations(method));
        }

        return blockDescriptors;
    }

    private final Method method;

    public BlockAnnotations(final Method method) {
        this.method = method;
    }

    public String getDescription() {
        return method.getAnnotation(BlockMethod.class).value();
    }

    public Block createBlock() {
        return new Block() {
            @SuppressWarnings("rawtypes")
            private Var outputVariable;

            private VarList inputMap;

            @SuppressWarnings("unchecked")
            @Override
            public void run() {
                try {
                    final Object[] arguments = new Object[inputMap.size()];
                    final Iterator<Var<?>> inputIterator = inputMap.iterator();

                    for (int i = 0; i < arguments.length; i++)
                        arguments[i] = inputIterator.next().getValue();

                    final Object result = method.invoke(null, arguments);

                    if (outputVariable != null) outputVariable.setValue(result);
                }
                catch (final Exception e) {
                    IcyLogger.error(BlockAnnotations.class, e, e.getLocalizedMessage());
                }
            }

            @Override
            public void declareOutput(final VarList outputMap) {
                try {
                    final Class<?> returnType = method.getReturnType();
                    if (returnType == Void.TYPE) {
                        outputVariable = null;
                    }
                    else {
                        final Class<? extends Var<?>> outputVarClass = getVariableType(returnType);
                        final Constructor<? extends Var<?>> outputConstructor = outputVarClass.getDeclaredConstructor(String.class, returnType);
                        outputVariable = outputConstructor.newInstance("output (" + returnType.getSimpleName() + ")", null);
                    }
                }
                catch (final Exception e) {
                    IcyLogger.error(BlockAnnotations.class, e, e.getLocalizedMessage());
                }

                if (outputVariable != null) outputMap.add("output", outputVariable);
            }

            @Override
            public void declareInput(final VarList theInputMap) {
                this.inputMap = theInputMap;

                try {
                    final Class<?>[] types = method.getParameterTypes();
                    final Annotation[][] annotations = method.getParameterAnnotations();

                    for (int i = 0; i < types.length; i++) {
                        final Class<?> parameterClass = types[i];
                        String parameterName = parameterClass.getSimpleName();

                        for (final Annotation annotation : annotations[i])
                            if (annotation.annotationType() == BlockInput.class) {
                                parameterName = ((BlockInput) annotation).value();
                            }

                        final Class<? extends Var<?>> inputVarClass = getVariableType(parameterClass);
                        final Constructor<? extends Var<?>> inputConstructor = inputVarClass.getDeclaredConstructor(String.class, parameterClass);
                        final Var<?> instance = inputConstructor.newInstance(parameterName, null);
                        theInputMap.add(instance.getName(), instance);
                    }
                }
                catch (final Exception e) {
                    IcyLogger.error(BlockAnnotations.class, e, e.getLocalizedMessage());
                }
            }
        };
    }

    public String getName() {
        return method.getName();
    }
}
