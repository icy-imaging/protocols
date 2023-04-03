package plugins.adufour.blocks.util;

import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;

import plugins.adufour.blocks.lang.Block;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarDouble;
import plugins.adufour.vars.lang.VarFile;
import plugins.adufour.vars.lang.VarFloat;
import plugins.adufour.vars.lang.VarInteger;
import plugins.adufour.vars.lang.VarSequence;
import plugins.adufour.vars.lang.VarString;

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

    public static Class<?> getInnerType(Class<? extends Var<?>> variableClass) {
        if (variableClass == VarInteger.class) return Integer.class;

        if (variableClass == VarDouble.class) return Double.class;

        if (variableClass == VarFloat.class) return Float.class;

        if (variableClass == VarBoolean.class) return Boolean.class;

        if (variableClass == VarFile.class) return File.class;

        if (variableClass == VarSequence.class) return Sequence.class;

        if (variableClass == VarString.class) return String.class;

        throw new UnsupportedOperationException("Unsupported type: " + variableClass.getSimpleName());
    }

    public static Class<? extends Var<?>> getVariableType(Class<?> clazz) {
        if (clazz == Integer.class) return VarInteger.class;

        if (clazz == Double.class) return VarDouble.class;

        if (clazz == Float.class) return VarFloat.class;

        if (clazz == Boolean.class) return VarBoolean.class;

        if (clazz == File.class) return VarFile.class;

        if (clazz == Sequence.class) return VarSequence.class;

        if (clazz == String.class) return VarString.class;

        throw new UnsupportedOperationException("Unsupported type: " + clazz.getSimpleName());
    }

    public static ArrayList<BlockAnnotations> findBlockMethods(Class<? extends Plugin> clazz) {
        final ArrayList<BlockAnnotations> blockDescriptors = new ArrayList<>();

        Method[] methods = clazz.getDeclaredMethods();

        for (final Method method : methods) {
            // make sure the method is static and annotated

            if (!Modifier.isStatic(method.getModifiers())) continue;

            BlockMethod blockFunction = method.getAnnotation(BlockMethod.class);
            if (blockFunction == null) continue;

            blockDescriptors.add(new BlockAnnotations(method));
        }

        return blockDescriptors;
    }

    private final Method method;

    public BlockAnnotations(Method method) {
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
                    Object[] arguments = new Object[inputMap.size()];
                    Iterator<Var<?>> inputIterator = inputMap.iterator();

                    for (int i = 0; i < arguments.length; i++)
                        arguments[i] = inputIterator.next().getValue();

                    Object result = method.invoke(null, arguments);

                    if (outputVariable != null) outputVariable.setValue(result);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void declareOutput(VarList outputMap) {
                try {
                    Class<?> returnType = method.getReturnType();
                    if (returnType == Void.TYPE) {
                        outputVariable = null;
                    }
                    else {
                        Class<? extends Var<?>> outputVarClass = getVariableType(returnType);
                        Constructor<? extends Var<?>> outputConstructor = outputVarClass.getDeclaredConstructor(String.class, returnType);
                        outputVariable = outputConstructor.newInstance("output (" + returnType.getSimpleName() + ")", null);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                if (outputVariable != null) outputMap.add("output", outputVariable);
            }

            @Override
            public void declareInput(VarList theInputMap) {
                this.inputMap = theInputMap;

                try {
                    Class<?>[] types = method.getParameterTypes();
                    Annotation[][] annotations = method.getParameterAnnotations();

                    for (int i = 0; i < types.length; i++) {
                        Class<?> parameterClass = types[i];
                        String parameterName = parameterClass.getSimpleName();

                        for (Annotation annotation : annotations[i])
                            if (annotation.annotationType() == BlockInput.class) {
                                parameterName = ((BlockInput) annotation).value();
                            }

                        Class<? extends Var<?>> inputVarClass = getVariableType(parameterClass);
                        Constructor<? extends Var<?>> inputConstructor = inputVarClass.getDeclaredConstructor(String.class, parameterClass);
                        Var<?> instance = inputConstructor.newInstance(parameterName, null);
                        theInputMap.add(instance.getName(), instance);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public String getName() {
        return method.getName();
    }
}
