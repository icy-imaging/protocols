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

import fr.icy.gui.LookAndFeelUtil;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.Batch;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.Loop;
import plugins.adufour.protocols.gui.link.DragDropZone;
import plugins.adufour.protocols.gui.link.DragZone;
import plugins.adufour.vars.lang.Var;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;

public class LoopPanel extends WorkFlowPanel {
    private final Loop loop;

    private final JPanel innerLoopPanel;

    /**
     * Option used in loops
     */
    protected final JCheckBox menuStopOnFirstError = new JCheckBox("Stop on first error");

    public LoopPanel(final WorkFlowContainer wfPane, final BlockDescriptor blockInfo) {
        super(wfPane, blockInfo);
        this.loop = (Loop) blockDesc.getBlock();

        // create a special panel to receive the loop variables
        innerLoopPanel = new JPanel(new GridBagLayout()) {
            /*@Override
            public void paintChildren(final Graphics g) {
                final Graphics2D g2 = (Graphics2D) g;

                //final Point2D p1 = gradient.getPoint1();
                //p1.setLocation(p1.getX() + 1, p1.getY() - 30);
                //g2.setPaint(new GradientPaint(p1, gradient.getColor1(), gradient.getPoint2(), gradient.getColor2()));
                //g2.setColor(getBackground());
                //g2.fillRect(5, 0, getWidth() - 11, getHeight());
                //g2.setPaint(null);
                //g2.setColor(getForeground());
                //g2.drawLine(15, getHeight() - 1, getWidth() - 6, getHeight() - 1);
                //g2.drawLine(getWidth() - 6, 0, getWidth() - 6, getHeight());

                super.paintChildren(g2);
            }*/

            @Override
            protected void paintComponent(final Graphics g) {
                super.paintComponent(g);
                //final Dimension arcs = new Dimension(ARC_SIZE, ARC_SIZE);
                final int width = getWidth();
                final int height = getHeight();
                final Graphics2D graphics = (Graphics2D) g;

                //Sets antialiasing
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

                final boolean isDarkMode = LookAndFeelUtil.isDarkMode();
                final Color bgcRoot = Objects.requireNonNull(LookAndFeelUtil.getUIDefaults()).getColor("RootPane.background");

                if (bgcRoot == null)
                    return;

                final Color bgc;
                if (isDarkMode)
                    bgc = new Color(bgcRoot.getRed() + 50, bgcRoot.getGreen() + 50, bgcRoot.getBlue() + 50);
                else
                    bgc = new Color(bgcRoot.getRed() - 20, bgcRoot.getGreen() - 20, bgcRoot.getBlue() - 20);

                //Draws the opaque panel with borders.
                graphics.setColor(bgc);
                graphics.fillRect(0, 0, width, height);//, arcs.width, arcs.height);

                if (toolBar == null)
                    return;

                // If the block is collapsed, remove the toolbar bottom border
                if (!blockDesc.isCollapsed())
                    toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getForeground()));
                else
                    toolBar.setBorder(null);
            }
        };

        innerLoopPanel.setOpaque(true);
        innerLoopPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 2));

        menuStopOnFirstError.addActionListener(this);
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == menuStopOnFirstError) {
            loop.stopOnFirstError.setValue(menuStopOnFirstError.isSelected());
        }
        else
            super.actionPerformed(e);
    }

    @Override
    protected void drawMenu() {
        super.drawMenu();

        // add an extra option to close the work flow while preserving its contents
        super.menu.add(menuStopOnFirstError);
    }

    @Override
    protected void drawContent() {
        innerLoopPanel.removeAll();

        int nextRow = 0;

        // add batch elements (if any) to the loop panel
        if (blockDesc.getBlock() instanceof Batch)
            nextRow = drawBatchVariables(nextRow);

        // add other loop variables
        for (final Var<?> loopVariable : loop.getLoopVariables()) {
            // don't re-draw batch elements again...
            if (blockDesc.getBlock() instanceof final Batch batch) {
                if (loopVariable == batch.getBatchSource() || loopVariable == batch.getBatchElement())
                    continue;
            }

            nextRow = drawLoopVariable(nextRow, loopVariable);
        }

        // add the loop panel to the main container
        final GridBagConstraints gbc_loop = new GridBagConstraints();
        gbc_loop.anchor = GridBagConstraints.NORTHWEST;
        jPanelContent.add(innerLoopPanel, gbc_loop);

        nextRow = drawExposedLinks(nextRow);

        drawInnerWorkFlowContainer(nextRow + 1);
    }

    private int drawBatchVariables(final int row) {
        final Batch batch = (Batch) blockDesc.getBlock();

        // Batch variables (source and element) are drawn in a peculiar way:
        // [source drop zone] [source label] [source editor] [element drag zone]

        // create drop zone and editor for the batch source
        final Var<?> batchSource = batch.getBatchSource();
        if (!varDropZones.containsKey(batchSource))
            varDropZones.put(batchSource, createVarDropZone(batchSource, true));
        if (!varEditors.containsKey(batchSource))
            varEditors.put(batchSource, createVarEditor(batchSource));

        // create drag zone for the batch element
        final Var<?> batchElement = batch.getBatchElement();
        if (!varDragZones.containsKey(batchElement))
            varDragZones.put(batchElement, createVarDragZone(batchElement, false));

        // Draw the first row in a particular way (source => elements)

        final GridBagConstraints gbc_dropZone = new GridBagConstraints();
        gbc_dropZone.anchor = GridBagConstraints.WEST;
        gbc_dropZone.fill = GridBagConstraints.NONE;
        gbc_dropZone.insets = new Insets(2, 0, 0, 0);
        gbc_dropZone.gridx = 0;
        gbc_dropZone.gridy = row;
        innerLoopPanel.add(varDropZones.get(batchSource), gbc_dropZone);

        final GridBagConstraints gbc_inputName = new GridBagConstraints();
        gbc_inputName.anchor = GridBagConstraints.CENTER;
        gbc_inputName.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputName.insets = new Insets(2, 5, 0, 0);
        gbc_inputName.gridx = 1;
        gbc_inputName.gridy = row;
        gbc_inputName.weightx = 1;
        final JLabel inputName = new JLabel(batchSource.getName());
        final Dimension inputNameDim = inputName.getPreferredSize();
        inputNameDim.height = 20;
        inputName.setPreferredSize(inputNameDim);
        innerLoopPanel.add(inputName, gbc_inputName);

        final GridBagConstraints gbc_inputComponent = new GridBagConstraints();
        gbc_inputComponent.anchor = GridBagConstraints.CENTER;
        gbc_inputComponent.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputComponent.insets = new Insets(2, 5, 0, 0);
        gbc_inputComponent.gridx = 2;
        gbc_inputComponent.gridy = row;
        gbc_inputComponent.weightx = 1;
        final JComponent inputEditor = (JComponent) varEditors.get(batchSource).getEditorComponent();
        inputEditor.setOpaque(false);
        inputEditor.setFocusable(false);
        inputEditor.setPreferredSize(varEditors.get(batchSource).getPreferredSize());
        innerLoopPanel.add(inputEditor, gbc_inputComponent);

        final GridBagConstraints gbc_dragZone = new GridBagConstraints();
        gbc_dragZone.anchor = GridBagConstraints.EAST;
        gbc_dragZone.fill = GridBagConstraints.NONE;
        gbc_dragZone.insets = new Insets(2, 5, 0, 0);
        gbc_dragZone.gridx = 3;
        gbc_dragZone.gridy = row;
        innerLoopPanel.add(varDragZones.get(batchElement), gbc_dragZone);

        return row + 1;
    }

    private int drawLoopVariable(final int startRow, final Var<?> loopVar) {
        final boolean isLoopInput = loop.getBlockDescriptor().inputVars.contains(loopVar);

        if (!varDropZones.containsKey(loopVar) && isLoopInput)
            varDropZones.put(loopVar, createVarDropZone(loopVar, true));
        if (!varEditors.containsKey(loopVar))
            varEditors.put(loopVar, createVarEditor(loopVar));
        if (!varDragZones.containsKey(loopVar))
            varDragZones.put(loopVar, createVarDragZone(loopVar, true));

        if (isLoopInput) {
            final GridBagConstraints gbc_dropZone = new GridBagConstraints();
            gbc_dropZone.anchor = GridBagConstraints.WEST;
            gbc_dropZone.fill = GridBagConstraints.NONE;
            gbc_dropZone.insets = new Insets(2, 0, 0, 0);
            gbc_dropZone.gridx = 0;
            gbc_dropZone.gridy = startRow;
            innerLoopPanel.add(loopVar.isEnabled() ? varDropZones.get(loopVar) : new JLabel("   "), gbc_dropZone);
        }

        final GridBagConstraints gbc_inputName = new GridBagConstraints();
        gbc_inputName.anchor = GridBagConstraints.CENTER;
        gbc_inputName.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputName.insets = new Insets(2, 5, 0, 0);
        gbc_inputName.gridx = 1;
        gbc_inputName.gridy = startRow;
        gbc_inputName.weightx = 1;
        final JLabel inputName = new JLabel(loopVar.getName());
        final Dimension inputNameDim = inputName.getPreferredSize();
        inputNameDim.height = 20;
        inputName.setPreferredSize(inputNameDim);
        innerLoopPanel.add(inputName, gbc_inputName);

        final GridBagConstraints gbc_inputComponent = new GridBagConstraints();
        gbc_inputComponent.anchor = GridBagConstraints.CENTER;
        gbc_inputComponent.fill = GridBagConstraints.HORIZONTAL;
        gbc_inputComponent.insets = new Insets(2, 5, 0, 0);
        gbc_inputComponent.gridx = 2;
        gbc_inputComponent.gridy = startRow;
        gbc_inputComponent.weightx = 1;
        final JComponent inputEditor = (JComponent) varEditors.get(loopVar).getEditorComponent();
        inputEditor.setOpaque(inputEditor instanceof JTextComponent);
        inputEditor.setFocusable(true);
        inputEditor.setEnabled(inputEditor instanceof JLabel || loopVar.getReference() == null);
        inputEditor.setPreferredSize(varEditors.get(loopVar).getPreferredSize());
        innerLoopPanel.add(inputEditor, gbc_inputComponent);

        final GridBagConstraints gbc_dragZone = new GridBagConstraints();
        gbc_dragZone.anchor = GridBagConstraints.EAST;
        gbc_dragZone.fill = GridBagConstraints.NONE;
        gbc_dragZone.insets = new Insets(2, 5, 0, 0);
        gbc_dragZone.gridx = 3;
        gbc_dragZone.gridy = startRow;
        innerLoopPanel.add(varDragZones.get(loopVar), gbc_dragZone);

        return startRow + 1;
    }

    @Override
    public Point getDragZoneLocation(final Var<?> output) {
        if (blockDesc.isCollapsed())
            return super.getDragZoneLocation(output);

        if (loop.isLoopVariable(output)) {
            final DragDropZone dz = varDragZones.get(output);

            if (dz != null) {
                final int x = dz.getLocationOnScreen().x - innerFlowPane.getLocationOnScreen().x;
                final int y = dz.getLocationOnScreen().y - innerFlowPane.getLocationOnScreen().y;
                return new Point(x + dz.getWidth(), y + dz.getHeight() / 2);
            }
        }

        return super.getDragZoneLocation(output);
    }

    @Override
    public int getVarPanelWidth() {
        return innerLoopPanel.getWidth();
    }

    @Override
    public int getVarPanelHeight() {
        // return innerLoopPanel.getHeight() + 2;
        return getHeight();
    }

    @Override
    public void drawPanel() {
        super.drawPanel();
        if (blockDesc.isCollapsed()) {
            for (final Component c : innerLoopPanel.getComponents())
                if (c instanceof DragZone<?>)
                    c.setPreferredSize(new Dimension());

            innerLoopPanel.setSize(innerLoopPanel.getPreferredSize());
            varBox.add(innerLoopPanel);
            final Dimension di = varBox.getPreferredSize();
            varBox.setPreferredSize(new Dimension(di.width, di.height + innerLoopPanel.getHeight()));

            setSize(getPreferredSize());
        }
        else
            for (final Component c : innerLoopPanel.getComponents())
                if (c instanceof DragZone<?>)
                    c.setPreferredSize(new Dimension(DragDropZone.DEFAULT_ICON_SIZE, DragDropZone.DEFAULT_ICON_SIZE));
    }

    @Override
    void dispose() {
        menuStopOnFirstError.removeActionListener(this);

        super.dispose();
    }
}
