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

package plugins.adufour.protocols.gui.link;

import fr.icy.system.thread.ThreadUtil;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.protocols.gui.block.BlockPanel;
import plugins.adufour.protocols.gui.block.WorkFlowContainer;
import plugins.adufour.vars.lang.Var;

import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public abstract class Line extends Line2D.Float implements ComponentListener, MouseListener {
    private final Link<?> link;

    private final Color typeColor;

    protected final BlockPanel srcPanel;

    protected final BlockPanel dstPanel;

    // dashed line:
    // new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0));
    private final Stroke stroke = new BasicStroke(2);

    private boolean isHighlighted = false;

    private final Path2D.Float path = new Path2D.Float();

    private final Ellipse2D.Float closeButton = new Ellipse2D.Float();

    private boolean isSelected = false;

    private final Stroke selectedStroke = new BasicStroke(8);

    /**
     * Creates a line representing a link between two variables
     *
     * @param srcPanel the source panel of the link
     * @param dstPanel the destination panel of the link
     * @param link     the link between the two variables
     * @throws IllegalArgumentException if the <code>link</code> argument is <code>null</code>
     */
    public Line(final BlockPanel srcPanel, final BlockPanel dstPanel, final Link<?> link) throws IllegalArgumentException {
        if (link == null)
            throw new IllegalArgumentException("link cannot be null");

        this.link = link;
        this.srcPanel = srcPanel;
        this.dstPanel = dstPanel;
        this.typeColor = DragDropZone.getColor(link.getType());

        srcPanel.addComponentListener(this);
        dstPanel.addComponentListener(this);

        attachListeners();

        update();
    }

    /**
     * Creates a line exposing a variable outside a work flow.
     *
     * @param srcPanel        the source panel of the link
     * @param dstPanel        the destination panel of the link
     * @param exposedVariable the exposed variable
     */
    public Line(final @NotNull BlockPanel srcPanel, final BlockPanel dstPanel, final @NotNull Var<?> exposedVariable) {
        this.link = null;
        this.srcPanel = srcPanel;
        this.dstPanel = dstPanel;
        this.typeColor = DragDropZone.getColor(exposedVariable.getType());

        if (srcPanel.blockDesc.isWorkFlow() && ((WorkFlow) srcPanel.blockDesc.getBlock()).contains(dstPanel.blockDesc)) {
            // srcPanel contains dstPanel => listen only to dstPanel
            dstPanel.addComponentListener(this);
        }
        else {
            // dstPanel contains srcPanel => listen only to srcPanel
            srcPanel.addComponentListener(this);
        }

        attachListeners();

        update();
    }

    private void attachListeners() {
        final DragDropZone p1z = getP1Zone();
        final DragDropZone p2z = getP2Zone();

        if (p1z != null)
            p1z.addMouseListener(this);
//        else
//            System.err.print("source drag zone null !");
        if (p2z != null)
            p2z.addMouseListener(this);
//        else
//            System.err.print("destination drop zone null !");
    }

    /**
     * @return the drag'drop zone corresponding to the first extremity of this
     * line. This method is called once when creating the line, in order
     * to attach a mouse listener to the drag'n'drop zone to support
     * line coloring
     * <p>
     * NOTE: This method *must* be overridden if this line is not backed
     * by a non-null link.
     */
    protected DragDropZone getP1Zone() {
        return srcPanel.getDragZone(link.srcVar);
    }

    /**
     * @return the drag'drop zone corresponding to the second extremity of this
     * line. This method is called once when creating the line, in order
     * to attach a mouse listener to the drag'n'drop zone to support
     * line coloring
     * <p>
     * NOTE: This method *must* be overridden if this line is not backed
     * by a non-null link.
     */
    protected DragDropZone getP2Zone() {
        return dstPanel.getDropZone(link.dstVar);
    }

    protected void updateP1() {
        final Point loc = srcPanel.getDragZoneLocation(link.srcVar);
        x1 = loc.x;
        y1 = loc.y;
    }

    protected void updateP2() {
        final Point loc = dstPanel.getDropZoneLocation(link.dstVar);
        x2 = loc.x;
        y2 = loc.y;
    }

    /**
     * updates the path describing the line and the shape and location of the close button
     */
    public abstract void updateShape(Path2D thePath, Ellipse2D theCloseButton);

    public void paint(final @NotNull Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isSelected) {
            g.setColor(WorkFlowContainer.TRANSPARENT_BLUE);
            g.setStroke(selectedStroke);
            g.draw(path);
        }

        g.setColor(typeColor);
        g.setStroke(stroke);

        path.reset();
        updateShape(path, closeButton);
        g.draw(path);

        if (isHighlighted)
            paintCloseButton(g);
    }

    private void paintCloseButton(final @NotNull Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.fill(closeButton);
        g2.setColor(Color.white);
        g2.drawLine((int) closeButton.x + 4, (int) closeButton.y + 4, (int) closeButton.x + 8, (int) closeButton.y + 8);
        g2.drawLine((int) closeButton.x + 4, (int) closeButton.y + 8, (int) closeButton.x + 8, (int) closeButton.y + 4);
        g2.dispose();
    }

    protected void updateCloseButtonShape(final @NotNull Shape s) {
        final Rectangle r = s.getBounds();
        closeButton.setFrame(r.x + r.width / 2f - 6, r.y + r.height / 2f - 6, 13, 13);
    }

    /**
     * Indicates if the location p is inside the virtual close button. This method can be used to
     * check whether the user has clicked on the close button
     *
     * @param p the point to check
     * @return true if p is within the virtual close button, false otherwise
     */
    public boolean isOverCloseButton(final @NotNull Point p) {
        return isOverCloseButton(p.x, p.y);
    }

    /**
     * Indicates if the location p is inside the virtual close button. This method can be used to
     * check whether the user has clicked on the close button
     *
     * @return true if p is within the virtual close button, false otherwise
     */
    public boolean isOverCloseButton(final double x, final double y) {
        return closeButton.contains(x, y);
    }

    public void dispose() {
        final DragDropZone p1 = getP1Zone();
        final DragDropZone p2 = getP2Zone();
        if (p1 != null)
            p1.removeMouseListener(this); // TODO leak ?
        if (p2 != null)
            p2.removeMouseListener(this); // TODO leak ?

        srcPanel.removeComponentListener(this);
        dstPanel.removeComponentListener(this);
    }

    @Override
    public void componentHidden(final ComponentEvent arg0) {
    }

    @Override
    public void componentMoved(final @NotNull ComponentEvent arg0) {
        updateLine((BlockPanel) arg0.getSource());
    }

    @Override
    public void componentResized(final @NotNull ComponentEvent arg0) {
        updateLine((BlockPanel) arg0.getSource());
    }

    @Override
    public void componentShown(final ComponentEvent arg0) {
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
    }

    @Override
    public void mousePressed(final MouseEvent e) {
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        setCustomColor(true);
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        setCustomColor(false);
    }

    /**
     * Sets whether the line should appear with a custom color (reflecting the type of transferred
     * data) or false if the default look'n'feel based color should be used. This method causes the
     * line container to repaint
     */
    public void setCustomColor(final boolean custom) {
        isHighlighted = custom;
        srcPanel.getWorkFlowContainer().repaint();
    }

    public void update() {
        // This has to be done on the EDT, in case P1 or P2 have just appeared
        ThreadUtil.invokeNow(() -> {
            try {
                updateP1();
                updateP2();
            }
            catch (final IllegalComponentStateException | NullPointerException e) {
                // Ignore it
            }
        });
    }

    private void updateLine(final BlockPanel panel) {
        if (panel == srcPanel)
            updateP1();
        else
            updateP2();
    }

    public void setSelected(final boolean selected) {
        isSelected = selected;
    }
}
