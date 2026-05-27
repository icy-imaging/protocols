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

package plugins.adufour.blocks.util;

import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.vars.lang.Var;

public interface WorkFlowListener {
    String WORKFLOW_MODIFIED = "WORKFLOW_MODIFIED";

    String WORKFLOW_REPLACED = "WORKFLOW_REPLACED";

    /**
     * Called when a block has been added to a work flow
     *
     * @param source     The workflow on which the block has been added.
     * @param addedBlock The block that has been added.
     */
    void blockAdded(WorkFlow source, BlockDescriptor addedBlock);

    void blockCollapsed(WorkFlow source, BlockDescriptor block, boolean collapsed);

    void blockDimensionChanged(WorkFlow source, BlockDescriptor block, int newWidth, int newHeight);

    void blockLocationChanged(WorkFlow source, BlockDescriptor block, int newX, int newY);

    void blockStatusChanged(WorkFlow source, BlockDescriptor block, BlockStatus status);

    void blockVariableAdded(WorkFlow source, BlockDescriptor block, Var<?> variable);

    <T> void blockVariableChanged(WorkFlow source, BlockDescriptor block, Var<T> variable, T newValue);

    /**
     * Called when a block has been removed from a work flow
     *
     * @param source       The workflow on which the block has been removed.
     * @param removedBlock The block that has been removed.
     */
    void blockRemoved(WorkFlow source, BlockDescriptor removedBlock);

    /**
     * Called when a link has been added to a work flow
     *
     * @param source    The workflow to which the link has been added.
     * @param addedLink The link that has been added to the workflow.
     */
    void linkAdded(WorkFlow source, Link<?> addedLink);

    /**
     * Called when a link has been removed from a work flow
     *
     * @param source      The workflow to which the link has been removed.
     * @param removedLink The link that has been added to the workflow.
     */
    void linkRemoved(WorkFlow source, Link<?> removedLink);

    /**
     * Called when the order of the blocks in the work flow has changed
     *
     * @param source The workflow that has been reordered.
     */
    void workFlowReordered(WorkFlow source);

    /**
     * Called when the work flow changes status (useful to display messages to the user via a status
     * bar)
     *
     * @param source  The workflow which its status has been changed.
     * @param message The message describing the change.
     */
    void statusChanged(WorkFlow source, String message);
}
