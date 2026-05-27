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

package plugins.adufour.protocols.gui.block;

import fr.icy.gui.component.icon.SVGResource;
import fr.icy.gui.component.menu.IcyMenuItem;
import fr.icy.system.thread.ThreadUtil;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.Loop;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.blocks.util.VarVisibilityListener;
import plugins.adufour.protocols.gui.link.DragDropZone;
import plugins.adufour.protocols.gui.link.Line;
import plugins.adufour.protocols.gui.link.RoundedSquareLine;
import plugins.adufour.vars.lang.Var;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class WorkFlowPanel extends BlockPanel implements VarVisibilityListener {
    private final WorkFlow innerWorkFlow;

    protected final JScrollPane scrollPane;

    protected final WorkFlowContainer innerFlowPane;

    private final HashMap<Var<?>, Line> exposingLinks = new HashMap<>();

    private final MouseAdapter mouseAdapter;

    final protected Box varBox;

    protected final IcyMenuItem menuRemoveEnclosure = new IcyMenuItem("Remove block but keep contents", SVGResource.CLOSE);

    public WorkFlowPanel(final WorkFlowContainer wfPane, final BlockDescriptor blockDesc) {
        super(wfPane, blockDesc);

        this.innerWorkFlow = (WorkFlow) blockDesc.getBlock();

        mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                final int xShift = innerFlowPane.getLocationOnScreen().x - getLocationOnScreen().x;
                final int yShift = innerFlowPane.getLocationOnScreen().y - getLocationOnScreen().y;
                final Point realPoint = new Point(e.getX() + xShift, e.getY() + yShift);

                for (final Var<?> var : exposingLinks.keySet())
                    if (exposingLinks.get(var).isOverCloseButton(realPoint)) {
                        VarList vars = blockDesc.inputVars;

                        if (!vars.contains(var))
                            vars = blockDesc.outputVars;

                        vars.setVisible(var, !vars.isVisible(var));

                        // prevent concurrent modification
                        break;
                    }
            }

            @Override
            public void mouseMoved(final MouseEvent e) {
                final int xShift = innerFlowPane.getLocationOnScreen().x - getLocationOnScreen().x;
                final int yShift = innerFlowPane.getLocationOnScreen().y - getLocationOnScreen().y;
                final Point realPoint = new Point(e.getX() + xShift, e.getY() + yShift);

                for (final Line l : exposingLinks.values())
                    l.setCustomColor(l.contains(realPoint.x, realPoint.y));
            }
        };

        blockDesc.inputVars.addVisibilityListener(this);
        blockDesc.outputVars.addVisibilityListener(this);

        this.innerFlowPane = new WorkFlowContainer(innerWorkFlow, wfPane.isEditable());
        this.innerFlowPane.setParentPanel(this);

        this.innerFlowPane.addMouseListener(mouseAdapter);
        this.innerFlowPane.addMouseMotionListener(mouseAdapter);

        this.scrollPane = new JScrollPane(innerFlowPane);
        scrollPane.setOpaque(false);
        scrollPane.setPreferredSize(new Dimension());

        // XXX
        varBox = Box.createVerticalBox();
        varBox.setOpaque(false);
        varBox.setVisible(false);
        add(varBox, BorderLayout.SOUTH);

        // check whether the descriptor has a stored size
        final Dimension size = innerWorkFlow.getBlockDescriptor().getDimension();

        if (size.width != 0) {
            // use it
            setPreferredSize(size);
        }
        else {
            // set a default (non-zero) size (because the "real" preferred size is currently 0)
            setPreferredSize(new Dimension(400, 400));
        }
        setResizeable(!blockDesc.isCollapsed());

        // add an extra option to close the work flow while preserving its contents
        menuRemoveEnclosure.addActionListener(this);
        menuRemoveEnclosure.setToolTipText("Remove this block but keep its contents");
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == menuRemoveEnclosure) {
            innerFlowPane.selectAllBlocks();
            workFlowPane.getWorkFlow().removeBlock(blockDesc, true);
        }
        else
            super.actionPerformed(e);
    }

    @Override
    protected void drawMenu() {
        super.drawMenu();

        // add an extra option to close the work flow while preserving its contents
        super.menu.add(menuRemoveEnclosure);
    }

    @Override
    protected void drawContent() {
        final int maxRow = drawExposedLinks(0);

        drawInnerWorkFlowContainer(maxRow + 1);
    }

    /**
     * Draws the drag n drop zones representing the exposed variables of the
     * inner work flow
     *
     * @param row the row where to start drawing within the {@link GridBagLayout}
     * @return the last row used in the {@link GridBagLayout}
     */
    protected int drawExposedLinks(final int row) {
        int rowIn = row;
        for (final Var<?> input : innerWorkFlow.getBlockDescriptor().inputVars) {
            // don't show hidden variable
            if (!blockDesc.inputVars.isVisible(input))
                continue;

            // don't show local loop variables
            if (blockDesc.isLoop() && ((Loop) blockDesc.getBlock()).isLoopVariable(input))
                continue;

            if (!varDropZones.containsKey(input))
                varDropZones.put(input, createVarDropZone(input, true));

            final GridBagConstraints gbc_dropZone = new GridBagConstraints();
            gbc_dropZone.anchor = GridBagConstraints.WEST;
            gbc_dropZone.fill = GridBagConstraints.NONE;
            gbc_dropZone.insets = new Insets(2, 0, 0, 0);
            gbc_dropZone.gridx = 0;
            gbc_dropZone.gridy = rowIn++;
            gbc_dropZone.weighty = 0;
            jPanelContent.add(varDropZones.get(input), gbc_dropZone);
        }

        int rowOut = row;
        for (final Var<?> output : innerWorkFlow.getBlockDescriptor().outputVars) {
            // don't show hidden variable
            if (!blockDesc.outputVars.isVisible(output))
                continue;

            // don't show local loop variables
            if (blockDesc.isLoop() && ((Loop) blockDesc.getBlock()).isLoopVariable(output))
                continue;

            if (!varDragZones.containsKey(output))
                varDragZones.put(output, createVarDragZone(output, true));

            final GridBagConstraints gbc_dragZone = new GridBagConstraints();
            gbc_dragZone.anchor = GridBagConstraints.EAST;
            gbc_dragZone.fill = GridBagConstraints.NONE;
            gbc_dragZone.insets = new Insets(2, 0, 0, 0);
            gbc_dragZone.gridx = 0;
            gbc_dragZone.gridy = rowOut++;
            final DragDropZone dragZone = varDragZones.get(output);
            jPanelContent.add(dragZone, gbc_dragZone);
        }

        return Math.max(rowIn, rowOut);
    }

    /**
     * Draws the container representing the inner work flow
     *
     * @param row the row where the container should be added within the {@link GridBagLayout}
     */
    protected void drawInnerWorkFlowContainer(final int row) {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 15, 5, 13);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = row;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        jPanelContent.add(scrollPane, gbc);
    }

    @Override
    void dispose() {
        blockDesc.inputVars.addVisibilityListener(this);
        blockDesc.outputVars.addVisibilityListener(this);

        menuRemoveEnclosure.removeActionListener(this);

        innerFlowPane.removeMouseListener(mouseAdapter);
        innerFlowPane.removeMouseMotionListener(mouseAdapter);

        innerFlowPane.dispose();

        super.dispose();
    }

    @Override
    public void paintChildren(final @NotNull Graphics g) {
        final Graphics gg = g.create();

        super.paintChildren(g);

        final Rectangle clip = scrollPane.getBounds();
        gg.clipRect(clip.x, clip.y, clip.width, clip.height);
        if (!blockDesc.isCollapsed())
            for (final Line line : exposingLinks.values()) {
                line.update();
                line.paint((Graphics2D) gg);
            }
        gg.dispose();

    }

    @Override
    public void visibilityChanged(final Var<?> variable, final boolean isVisible) {
        // Refresh first to ensure all existing blocks and links are visible
        ThreadUtil.invokeNow(this::refreshNow);

        updateExposedLink(variable, isVisible);
    }

    public void updateExposedLink(final Var<?> variable, final boolean visible) {
        if (visible) {
            if (innerWorkFlow.getInputOwner(variable) != null) {
                // source is an input variable
                final BlockDescriptor innerBlock = innerWorkFlow.getInputOwner(variable);

                ThreadUtil.invokeLater(() -> {
                    final BlockPanel ownerPanel = innerFlowPane.getBlockPanel(innerBlock);

                    if (ownerPanel == null)
                        return;

                    exposingLinks.put(variable, new RoundedSquareLine(WorkFlowPanel.this, ownerPanel, variable) {
                        @Override
                        protected DragDropZone getP1Zone() {
                            return varDropZones.get(variable);
                        }

                        @Override
                        protected DragDropZone getP2Zone() {
                            return ownerPanel.varDropZones.get(variable);
                        }

                        @Override
                        public void updateP1() {
                            if (blockDesc.isCollapsed())
                                return;

                            final DragDropZone dz = getP1Zone();

                            if (dz != null) {
                                final int y = dz.getLocationOnScreen().y;
                                final int offsetY = getLocationOnScreen().y;

                                x1 = dz.getWidth();
                                y1 = y - offsetY + dz.getHeight() / 2f;
                            }
                        }

                        @Override
                        public void updateP2() {
                            if (ownerPanel.blockDesc.isCollapsed()) {
                                final Point loc = ownerPanel.getLocationOnScreen();
                                x2 = loc.x - getLocationOnScreen().x + SHADOW_SIZE;
                                y2 = loc.y - getLocationOnScreen().y + ownerPanel.getHeight() / 2f;
                            }
                            else {
                                final DragDropZone dz = ownerPanel.varDropZones.get(variable);
                                if (dz != null) {
                                    final Point loc = dz.getLocationOnScreen();
                                    x2 = loc.x - getLocationOnScreen().x;
                                    y2 = loc.y - getLocationOnScreen().y + dz.getHeight() / 2f;
                                }
                            }
                        }
                    });
                }, true);
            }
            else {
                // source is an output variable
                final BlockDescriptor innerBlock = innerWorkFlow.getOutputOwner(variable);

                ThreadUtil.invokeLater(() -> {
                    final BlockPanel ownerPanel = innerFlowPane.getBlockPanel(innerBlock);

                    if (ownerPanel == null)
                        return;

                    exposingLinks.put(variable, new RoundedSquareLine(ownerPanel, WorkFlowPanel.this, variable) {
                        @Override
                        protected DragDropZone getP1Zone() {
                            return ownerPanel.varDragZones.get(variable);
                        }

                        @Override
                        protected DragDropZone getP2Zone() {
                            return varDragZones.get(variable);
                        }

                        @Override
                        public void updateP1() {
                            if (ownerPanel.blockDesc.isCollapsed()) {
                                final Point loc = ownerPanel.getLocationOnScreen();
                                x1 = loc.x - getLocationOnScreen().x + ownerPanel.getWidth() - SHADOW_SIZE;
                                y1 = loc.y - getLocationOnScreen().y + ownerPanel.getHeight() / 2f;
                            }
                            else {
                                final DragDropZone dz = ownerPanel.varDragZones.get(variable);

                                if (dz != null) {
                                    final Point loc = dz.getLocationOnScreen();
                                    x1 = loc.x - getLocationOnScreen().x + dz.getWidth();
                                    y1 = loc.y - getLocationOnScreen().y + dz.getHeight() / 2f;
                                }
                            }
                        }

                        @Override
                        public void updateP2() {
                            if (blockDesc.isCollapsed())
                                return;

                            final DragDropZone dz = varDragZones.get(variable);

                            if (dz != null) {
                                final int y = dz.getLocationOnScreen().y;
                                final int innerY = getLocationOnScreen().y;

                                x2 = dz.getLocation().x;
                                y2 = y - innerY + dz.getHeight() / 2f;
                            }
                        }
                    });
                }, true);
            }
        }
        else if (exposingLinks.containsKey(variable)) {
            variable.setReference(null);

            exposingLinks.remove(variable).dispose();

            BlockDescriptor block = innerWorkFlow.getInputOwner(variable);

            final WorkFlow parentWorkFlow = blockDesc.getContainer();

            if (block != null) {
                // source is an input variable
                varDropZones.remove(variable);
                blockDesc.getContainer().getBlockDescriptor().removeInput(variable);

                // remove links pointed to the exposed variable (if any)
                if (parentWorkFlow.isLinked(variable))
                    parentWorkFlow.removeLink(variable);
            }
            else {
                block = innerWorkFlow.getOutputOwner(variable);

                if (block != null) {
                    varDragZones.remove(variable);
                    blockDesc.getContainer().getBlockDescriptor().removeOutput(variable);

                    // remove links pointed to the exposed variable (if any)
                    final ArrayList<Link<?>> linksToDelete = new ArrayList<>();
                    for (final Link<?> link : parentWorkFlow.getLinksIterator())
                        if (link.srcVar == variable)
                            linksToDelete.add(link);

                    for (final Link<?> link : linksToDelete)
                        parentWorkFlow.removeLink(link.dstVar);
                }
            }
        }

        repaint();
        // Repaint the top-level container
        getParent().repaint();
    }

    @Override
    public void drawPanel() {
        super.drawPanel();
        varBox.setVisible(blockDesc.isCollapsed());
        if (blockDesc.isCollapsed()) {
            final GridBagConstraints gbc_dropZone = new GridBagConstraints();
            gbc_dropZone.anchor = GridBagConstraints.WEST;
            gbc_dropZone.fill = GridBagConstraints.NONE;
            gbc_dropZone.insets = new Insets(2, 0, 0, 0);
            gbc_dropZone.gridx = 0;

            final GridBagConstraints gbc_varName = new GridBagConstraints();
            gbc_varName.anchor = GridBagConstraints.CENTER;
            gbc_varName.fill = GridBagConstraints.HORIZONTAL;
            gbc_varName.insets = new Insets(2, 5, 0, 0);
            gbc_varName.gridx = 1;
            gbc_varName.weightx = 1;

            final GridBagConstraints gbc_dragZone = new GridBagConstraints();
            gbc_dragZone.anchor = GridBagConstraints.EAST;
            gbc_dragZone.fill = GridBagConstraints.NONE;
            gbc_dragZone.insets = new Insets(2, 5, 0, 0);
            gbc_dragZone.gridx = 2;

            final LinkedHashMap<BlockDescriptor, ArrayList<Var<?>>> inputs = new LinkedHashMap<>();
            final LinkedHashMap<BlockDescriptor, ArrayList<Var<?>>> outputs = new LinkedHashMap<>();

            JPanel blockP;
            Box blockBox;
            JPanel varPanel;

            varBox.removeAll();

            ArrayList<Var<?>> in;
            ArrayList<Var<?>> out;

            int nbExposingBlocks = 0;

            for (final BlockDescriptor bd : innerWorkFlow) {
                in = new ArrayList<>();
                out = new ArrayList<>();

                for (final Var<?> v : bd.inputVars)
                    if (exposingLinks.containsKey(v)) {
                        in.add(v);
                        if (!inputs.containsKey(bd)) {
                            inputs.put(bd, in);
                            outputs.put(bd, out);
                            nbExposingBlocks++;
                        }
                    }
                for (final Var<?> v : bd.outputVars)
                    if (exposingLinks.containsKey(v)) {
                        out.add(v);
                        if (!inputs.containsKey(bd)) {
                            inputs.put(bd, in);
                            outputs.put(bd, out);
                            nbExposingBlocks++;
                        }
                    }
            }

            varBox.setPreferredSize(new Dimension(0, exposingLinks.size() * 20 + nbExposingBlocks * 25));

            for (final BlockDescriptor bd : inputs.keySet()) {
                varBox.add(new JSeparator(JSeparator.HORIZONTAL));
                blockP = new JPanel(new BorderLayout());
                blockP.setOpaque(false);

                final JLabel blockName = new JLabel("\t" + bd.getDefinedName(), JLabel.CENTER);
                blockName.setFont(blockName.getFont().deriveFont(Font.BOLD + Font.ITALIC, 12));
                blockP.add(blockName, BorderLayout.NORTH);

                blockBox = Box.createVerticalBox();
                blockBox.setOpaque(false);
                blockP.add(blockBox, BorderLayout.CENTER);

                for (final Var<?> v : inputs.get(bd)) {
                    varPanel = new JPanel(new GridBagLayout());
                    varPanel.setOpaque(false);

                    final DragDropZone dz = varDropZones.get(v);

                    if (dz != null) {
                        varPanel.add(dz, gbc_dropZone);
                        varPanel.add(new JLabel("\t" + v.getName()), gbc_varName);

                        blockBox.add(varPanel);
                    }
                }

                for (final Var<?> v : outputs.get(bd)) {
                    varPanel = new JPanel(new GridBagLayout());
                    varPanel.setOpaque(false);

                    varPanel.add(new JLabel("\t\t\t"), gbc_dropZone);
                    varPanel.add(new JLabel("\t" + v.getName()), gbc_varName);

                    final DragDropZone dz = varDragZones.get(v);

                    if (dz != null) {
                        varPanel.add(dz, gbc_dragZone);

                        blockBox.add(varPanel);
                    }
                }

                varBox.add(blockP);
            }
            setSize(getPreferredSize());
        }
    }

    @Override
    public Dimension getPreferredSize() {
        final Dimension d = super.getPreferredSize();

        if (!blockDesc.isCollapsed())
            return d;

        return new Dimension(200, d.height + varBox.getPreferredSize().height);
    }
}
