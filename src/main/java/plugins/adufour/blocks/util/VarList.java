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

import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * Class defining a map of variables using a {@link java.util.HashMap} dictionary
 *
 * @author Alexandre Dufour
 */
public class VarList implements Iterable<Var<?>> {
    private final LinkedHashMap<String, Var<?>> varMap = new LinkedHashMap<>();

    private final HashMap<Var<?>, Boolean> visibilityMap = new HashMap<>();

    private final HashMap<Var<?>, Boolean> runtimeVariableMap = new HashMap<>();

    private final ArrayList<VarVisibilityListener> visibilityListeners = new ArrayList<>();

    private final ArrayList<VarListListener> varListListeners = new ArrayList<>();

    /**
     * Adds the given variable to this list with the specified unique ID. If a variable with same
     * unique ID already exists, an {@link IllegalArgumentException} is thrown.
     *
     * @param uid      the unique ID of the variable (within this list)
     * @param variable The variable to add
     * @throws IllegalArgumentException if the variable already exists, or if a variable with same unique ID exists
     */
    public void add(final String uid, final Var<?> variable) {
        add(uid, variable, false);
    }

    /**
     * Add a runtime variable to this list (see {@link #isRuntimeVariable(Var)} for more details).
     * Runtime variables are limited to {@link VarMutable} for reloading purposes. <br>
     * WARNING: Do *not* add a runtime variable from within {@link Block#declareInput(VarList)} or
     * {@link Block#declareOutput(VarList)} with no particular runtime condition.
     *
     * @param uid      the unique ID of the variable (within this list)
     * @param variable The variable to add
     */
    public void addRuntimeVariable(final String uid, final VarMutable variable) {
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
     * @param uid               the unique ID of the variable (within this list)
     * @param variable          The variable to add
     * @param isRuntimeVariable <code>true</code> is the variable is dynamic (see {@link #isRuntimeVariable(Var)})
     *                          for more details.
     * @throws IllegalArgumentException if the variable already exists, or if a variable with same unique ID exists
     */
    private void add(final String uid, final Var<?> variable, final boolean isRuntimeVariable) {
        if (varMap.containsKey(uid))
            throw new IllegalArgumentException("A variable with same unique ID (" + uid + ") exists in the map");

        varMap.put(uid, variable);

        // By default, all block variables are visible.
        // However, variables from embedded blocks should not be visible
        // (and should remain so until they are exposed by the user)
        visibilityMap.put(variable, !uid.contains(":"));

        runtimeVariableMap.put(variable, isRuntimeVariable);

        for (final VarListListener l : varListListeners)
            l.variableAdded(this, variable);
    }

    /**
     * Registers a new listener to receive events when a variable is added to this list
     *
     * @param listener Variable list listener to be added.
     */
    public void addVarListListener(final VarListListener listener) {
        varListListeners.add(listener);
    }

    /**
     * Registers a new listener to receive events when variables in this list change visibility
     *
     * @param listener Variable list listener to be added.
     */
    public void addVisibilityListener(final VarVisibilityListener listener) {
        visibilityListeners.add(listener);
    }

    public void clear() {
        // remove elements one by one to notify listeners properly
        final ArrayList<Var<?>> vars = new ArrayList<>(varMap.values());

        for (final Var<?> var : vars)
            remove(var);

        // FIXME this code does not remove links in the enclosing work flow...
    }

    public boolean contains(final Var<?> variable) {
        return varMap.containsValue(variable);
    }

    /**
     * @return first contained variable in the {@link VarList} and <i>null</i> if VarList is empty
     */
    public Var<?> first() {
        for (final Var<?> v : varMap.values())
            return v;

        return null;
    }

    /**
     * Generic access method to retrieve a variable from the map. This method uses generic types to
     * prevent unchecked conversion warning in higher-level code.
     *
     * @param <T> Type of the variable value.
     * @param uid The unique ID of the variable to retrieve.
     * @return The stored variable, or null if this name isn't in the map.
     */
    @SuppressWarnings("unchecked")
    public <T> Var<T> get(final String uid) {
        final Var<?> var = varMap.get(uid);
        return var == null ? null : (Var<T>) var;
    }

    /**
     * Legacy method (used to load old XML work flows).
     *
     * @param <T>   Type of the variable value.
     * @param varID Variable identifier.
     * @return The stored variable.
     * @throws NoSuchVariableException If no variable is not found with the given identifier
     */
    @SuppressWarnings("unchecked")
    <T> Var<T> get(final int varID) throws NoSuchVariableException {
        int id = 0;
        for (final Var<?> var : this) {
            if (id == varID)
                return (Var<T>) var;
            id++;
        }
        throw new NoSuchVariableException("No variable with ID " + varID);
    }


    /**
     * Returns the unique ID of the specified variable. Although the underlying structure
     *
     * @param var Target variable
     * @return Identifier of the given variable.
     * @throws NoSuchVariableException If the given variable is not present in this variable list.
     */
    public String getID(final Var<?> var) throws NoSuchVariableException {
        for (final String uid : varMap.keySet())
            if (varMap.get(uid) == var)
                return uid;

        throw new NoSuchVariableException("Variable " + var.getName() + " does not exist in this list");
    }

    /**
     * Indicates whether the specified variable is dynamic.<br>
     * A variable is considered dynamic if it has been added to the list at "design-time" (e.g., via
     * the graphical user interface) rather than at "compile-time" (i.e. via
     * {@link Block#declareInput(VarList)} or {@link Block#declareOutput(VarList)}). <br>
     * A dynamic variable is marked with an additional attribute when stored in XML, such that it
     * can be restored properly (via {@link VarMutable} objects) when the variable is reloaded from
     * XML.
     *
     * @param var Variable to be checked.
     * @return true if the variable is of type runtime.
     */
    public boolean isRuntimeVariable(final Var<?> var) {
        return runtimeVariableMap.get(var);
    }

    public boolean isVisible(final Var<?> var) {
        return visibilityMap.get(var);
    }

    @Override
    public @NotNull Iterator<Var<?>> iterator() {
        return varMap.values().iterator();
    }

    public void remove(final @NotNull Var<?> var) {
        if (var.getReference() != null) {
            var.setReference(null);
            return;
        }

        varMap.remove(getID(var));
        visibilityMap.remove(var);
        runtimeVariableMap.remove(var);

        for (final VarListListener l : varListListeners)
            l.variableRemoved(this, var);
    }

    /**
     * Registers a new listener to receive events when a variable is removed from this list.
     *
     * @param listener Listener to remove.
     */
    public void removeVarListListener(final VarListListener listener) {
        varListListeners.remove(listener);
    }

    /**
     * Registers a new listener to receive events when variables in this list change visibility.
     *
     * @param listener Listener to remove.
     */
    public void removeVisibilityListener(final VarVisibilityListener listener) {
        visibilityListeners.remove(listener);
    }

    /**
     * Adjusts the visibility of the given variable outside the enclosing work flow.
     *
     * @param var     Variable to set visibility.
     * @param visible true if the variable is to be visible. false otherwise.
     */
    public void setVisible(final Var<?> var, final boolean visible) {
        if (!visibilityMap.containsKey(var))
            return;// throw new NoSuchVariableException(var);

        if (visibilityMap.get(var) != visible) {
            visibilityMap.put(var, visible);

            for (final VarVisibilityListener l : visibilityListeners)
                l.visibilityChanged(var, visible);
        }
    }

    public int size() {
        return varMap.size();
    }

}
