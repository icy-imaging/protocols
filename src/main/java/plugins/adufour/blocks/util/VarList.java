package plugins.adufour.blocks.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import plugins.adufour.blocks.lang.Block;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;

/**
 * Class defining a map of variables using a {@link java.util.HashMap} dictionary
 * 
 * @author Alexandre Dufour
 */
public class VarList implements Iterable<Var<?>>
{
    private final LinkedHashMap<String, Var<?>> varMap = new LinkedHashMap<String, Var<?>>();

    private final HashMap<Var<?>, Boolean> visibilityMap = new HashMap<Var<?>, Boolean>();

    private final HashMap<Var<?>, Boolean> runtimeVariableMap = new HashMap<Var<?>, Boolean>();

    private final ArrayList<VarVisibilityListener> visibilityListeners = new ArrayList<VarVisibilityListener>();

    private final ArrayList<VarListListener> varListListeners = new ArrayList<VarListListener>();

    /**
     * Adds the specified variable to this variable list. Each variable is given a unique
     * identifier, which is here the variable's name. Adding another variable with the same name
     * using this method will throw a {@link IllegalArgumentException}. Instead, use the
     * {@link #add(String, Var)} method to specify a unique identifier for each variable.
     * 
     * @param variable
     *        The variable to add
     * @throws IllegalArgumentException
     *         if the variable already exists, or if a variable with same unique ID exists
     * @deprecated Changing the name of a variable will also change the default UID, and protocols
     *             containing an older version of this block (with the old name) will not reload
     *             properly. Use {@link #add(String, Var)} to specify a unique identifier (and make
     *             sure this identifier never changes across versions!)<br>
     *             NB: for the same reason, when migrating to {@link #add(String, Var)}, use the
     *             current name of the variable as unique identifier
     */
    @Deprecated
    public void add(Var<?> variable)
    {
        add(variable.getName(), variable);
    }

    /**
     * Adds the given variable to this list with the specified unique ID. If a variable with same
     * unique ID already exists, an {@link IllegalArgumentException} is thrown.
     * 
     * @param uid
     *        the unique ID of the variable (within this list)
     * @param variable
     *        The variable to add
     * @throws IllegalArgumentException
     *         if the variable already exists, or if a variable with same unique ID exists
     */
    public void add(String uid, Var<?> variable)
    {
        add(uid, variable, false);
    }

    /**
     * Add a runtime variable to this list (see {@link #isRuntimeVariable(Var)} for more details).
     * Runtime variables are limited to {@link VarMutable} for reloading purposes. <br>
     * WARNING: Do *not* add a runtime variable from within {@link Block#declareInput(VarList)} or
     * {@link Block#declareOutput(VarList)} with no particular runtime condition.
     * 
     * @param uid
     *        the unique ID of the variable (within this list)
     * @param variable
     *        The variable to add
     */
    public void addRuntimeVariable(String uid, VarMutable variable)
    {
        add(uid, variable, true);
    }

    /**
     * Adds the given variable to this list with the specified unique ID. Note that if a variable
     * with same unique ID already exists, an {@link IllegalArgumentException} is thrown. <br/>
     * <br/>
     * WARNING: Do *not* mark a variable as dynamic if it is created and added from within
     * {@link Block#declareInput(VarList)} or {@link Block#declareOutput(VarList)} with no
     * particular runtime condition.
     * 
     * @param uid
     *        the unique ID of the variable (within this list)
     * @param variable
     *        The variable to add
     * @param isRuntimeVariable
     *        <code>true</code> is the variable is dynamic (see {@link #isRuntimeVariable(Var)})
     *        for more details.
     * @throws IllegalArgumentException
     *         if the variable already exists, or if a variable with same unique ID exists
     */
    private void add(String uid, Var<?> variable, boolean isRuntimeVariable)
    {
        if (varMap.containsKey(uid))
            throw new IllegalArgumentException("A variable with same unique ID (" + uid + ") exists in the map");

        varMap.put(uid, variable);

        // By default, all block variables are visible
        // However, variables from embedded blocks should not be visible
        // (and should remain so until they are exposed by the user)
        visibilityMap.put(variable, !uid.contains(":"));

        runtimeVariableMap.put(variable, isRuntimeVariable);

        for (VarListListener l : varListListeners)
            l.variableAdded(this, variable);
    }

    /**
     * Registers a new listener to receive events when a variable is added to this list
     * 
     * @param listener
     *        Variable list listener to be added.
     */
    public void addVarListListener(VarListListener listener)
    {
        varListListeners.add(listener);
    }

    /**
     * Registers a new listener to receive events when variables in this list change visibility
     * 
     * @param listener
     *        Variable list listener to be added.
     */
    public void addVisibilityListener(VarVisibilityListener listener)
    {
        visibilityListeners.add(listener);
    }

    public void clear()
    {
        // remove elements one by one to notify listeners properly
        ArrayList<Var<?>> vars = new ArrayList<Var<?>>(varMap.values());

        for (Var<?> var : vars)
            remove(var);

        // FIXME this code does not remove links in the enclosing work flow...
    }

    public boolean contains(Var<?> variable)
    {
        return varMap.containsValue(variable);
    }

    /**
     * @deprecated Legacy method (used to load old XML work flows).
     * @param <T>
     *        Type of the variable value.
     * @param varID
     *        Variable identifier.
     * @return The stored variable.
     * @throws NoSuchVariableException
     *         If no variable is not found with the given identifier
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T> Var<T> get(int varID) throws NoSuchVariableException
    {
        int id = 0;
        for (Var<?> var : this)
        {
            if (id == varID)
                return (Var<T>) var;
            id++;
        }
        throw new NoSuchVariableException("No variable with ID " + varID);
    }

    /**
     * Generic access method to retrieve a variable from the map. This method uses generic types to
     * prevent unchecked conversion warning in higher-level code.
     * 
     * @param <T>
     *        Type of the variable value.
     * @param uid
     *        The unique ID of the variable to retrieve.
     * @return The stored variable, or null if this name isn't in the map.
     */
    @SuppressWarnings("unchecked")
    public <T> Var<T> get(String uid)
    {
        Var<?> var = varMap.get(uid);
        return var == null ? null : (Var<T>) var;
    }

    /**
     * Returns the unique ID of the specified variable. Although the underlying structure
     * 
     * @param var
     *        Target variable
     * @return Identifier of the given variable.
     * @throws NoSuchVariableException
     *         If given variable is not present in this variable list.
     */
    public String getID(Var<?> var) throws NoSuchVariableException
    {
        for (String uid : varMap.keySet())
            if (varMap.get(uid) == var)
                return uid;

        throw new NoSuchVariableException("Variable " + var.getName() + " does not exist in this list");
    }

    /**
     * @deprecated Variable index should not be used to refer to a variable
     * @param variable
     *        Target variable.
     * @return The index of the list where the given variable is stored.
     * @throws NoSuchVariableException
     *         If the variable is not found in this variable list.
     */
    @Deprecated
    public int indexOf(Var<?> variable) throws NoSuchVariableException
    {
        // throw new UnsupportedOperationException("Cannot retrieve the index of a variable");
        // return varMap.indexOf(variable);
        int index = 0;
        for (Var<?> var : this)
        {
            if (variable == var)
                return index;
            index++;
        }
        throw new NoSuchVariableException(variable.getName());
    }

    /**
     * Indicates whether the specified variable is dynamic.<br>
     * A variable is considered dynamic if it has been added to the list at "design-time" (e.g. via
     * the graphical user interface) rather than at "compile-time" (i.e. via
     * {@link Block#declareInput(VarList)} or {@link Block#declareOutput(VarList)}). <br>
     * A dynamic variable is marked with an additional attribute when stored in XML, such that it
     * can be restored properly (via {@link VarMutable} objects) when the variable is reloaded from
     * XML.
     * 
     * @param var
     *        Variable to be checked.
     * @return true if the variable is of type runtime.
     */
    public boolean isRuntimeVariable(Var<?> var)
    {
        return runtimeVariableMap.get(var);
    }

    public boolean isVisible(Var<?> var)
    {
        return visibilityMap.get(var);
    }

    @Override
    public Iterator<Var<?>> iterator()
    {
        return varMap.values().iterator();
    }

    public void remove(Var<?> var)
    {
        if (var.getReference() != null)
        {
            var.setReference(null);
            return;
        }

        varMap.remove(getID(var));
        visibilityMap.remove(var);
        runtimeVariableMap.remove(var);

        for (VarListListener l : varListListeners)
            l.variableRemoved(this, var);
    }

    /**
     * Registers a new listener to receive events when a variable is removed from this list.
     * 
     * @param listener
     *        Listener to remove.
     */
    public void removeVarListListener(VarListListener listener)
    {
        varListListeners.remove(listener);
    }

    /**
     * Registers a new listener to receive events when variables in this list change visibility.
     * 
     * @param listener
     *        Listener to remove.
     */
    public void removeVisibilityListener(VarVisibilityListener listener)
    {
        visibilityListeners.remove(listener);
    }

    /**
     * Adjusts the visibility of the given variable outside the enclosing work flow.
     * 
     * @param var
     *        Variable to set visibility.
     * @param visible
     *        true if the variable is to be visible. false otherwise.
     */
    public void setVisible(Var<?> var, boolean visible)
    {
        if (!visibilityMap.containsKey(var))
            return;// throw new NoSuchVariableException(var);

        if (visibilityMap.get(var) != visible)
        {
            visibilityMap.put(var, visible);

            for (VarVisibilityListener l : visibilityListeners)
                l.visibilityChanged(var, visible);
        }
    }

    public int size()
    {
        return varMap.size();
    }

}
