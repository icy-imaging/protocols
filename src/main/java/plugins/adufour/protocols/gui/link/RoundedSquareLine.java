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

import plugins.adufour.blocks.lang.Link;
import plugins.adufour.protocols.gui.block.BlockPanel;
import plugins.adufour.protocols.gui.block.WorkFlowPanel;
import plugins.adufour.vars.lang.Var;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

/**
 * Class defining a line linking two block variables
 *
 * @author Alexandre Dufour
 */
public class RoundedSquareLine extends Line {
    private final ArrayList<Shape> shapes = new ArrayList<>();

    /**
     * Creates a line representing a link between two variables
     *
     * @param srcPanel the source panel of the link
     * @param dstPanel the destination panel of the link
     * @param link     the link between the two variables
     * @throws IllegalArgumentException if the <code>link</code> argument is <code>null</code>
     */
    public RoundedSquareLine(final BlockPanel srcPanel, final BlockPanel dstPanel, final Link<?> link) throws IllegalArgumentException {
        super(srcPanel, dstPanel, link);
    }

    /**
     * Creates a line exposing a variable outside a work flow.
     *
     * @param srcPanel        the source panel of the link
     * @param dstPanel        the destination panel of the link
     * @param exposedVariable the exposed variable
     */
    public RoundedSquareLine(final BlockPanel srcPanel, final BlockPanel dstPanel, final Var<?> exposedVariable) {
        super(srcPanel, dstPanel, exposedVariable);
    }

    @Override
    public void updateShape(final Path2D path, final Ellipse2D closeButton) {
        shapes.clear();

        // Path2D path = new Path2D.Float();
        // path.moveTo(x1, y1);

        float x1Control = Math.max(0.5f * (x1 + x2), x1 + 10);
        float x2Control = Math.min(0.5f * (x1 + x2), x2 - 10);

        if (x1Control <= x2Control) {
            final float yGap = Math.min(Math.abs(y2 - y1), 10);

            shapes.add(new Line2D.Float(x1, y1, x1Control - 10, y1));
            if (Math.abs(y1 - y2) > 20) {
                shapes.add(new QuadCurve2D.Float(x1Control - 10, y1, x1Control, y1, x1Control, y1 < y2 ? y1 + yGap : y1 - yGap));
                shapes.add(new Line2D.Float(x1Control, y1 < y2 ? y1 + yGap : y1 - yGap, x1Control, y1 < y2 ? y2 - yGap : y2 + yGap));
                shapes.add(new QuadCurve2D.Float(x1Control, y1 < y2 ? y2 - yGap : y2 + yGap, x1Control, y2, x1Control + 10, y2));
            }
            else {
                shapes.add(new CubicCurve2D.Float(x1Control - 10, y1, x1Control + 9, y1, x1Control - 9, y2, x1Control + 10, y2));
            }
            shapes.add(new Line2D.Float(x1Control + 10, y2, x2, y2));

            for (final Shape shape : shapes)
                path.append(shape.getPathIterator(null), true);

            final Rectangle r = path.getBounds();
            closeButton.setFrame(r.x + r.width / 2f - 6, r.y + r.height / 2f - 6, 13, 13);
        }
        else {
            final float yControl;

            if (dstPanel.getY() > srcPanel.getY() + srcPanel.getVarPanelHeight() + 5) {
                yControl = srcPanel.getY() + srcPanel.getVarPanelHeight() + 5;
            }
            else if (srcPanel.getY() > dstPanel.getY() + dstPanel.getVarPanelHeight() + 10) {
                yControl = srcPanel.getY() - 5;
            }
            else {
                if (srcPanel instanceof WorkFlowPanel || y2 < srcPanel.getY() + dstPanel.getVarPanelHeight() && x2 > x1) {
                    // special case: src and dst are *very* close
                    closeButton.setFrame((int) (0.5f * (x1 + x2)) - 6, (int) (0.5f * (y1 + y2)) - 6, 13, 13);
                    final float xShift = 30 + 0.1f * Math.abs(x2 - x1);
                    path.append(new CubicCurve2D.Float(x1, y1, x1 + xShift, y1, x2 - xShift, y2, x2, y2), false);
                    return;
                }

                yControl = Math.min(srcPanel.getY(), dstPanel.getY()) - 10;
                if (dstPanel.getY() < y1) x1Control = Math.max(srcPanel.getX() + srcPanel.getWidth(), dstPanel.getX() + dstPanel.getWidth()) + 10;
                if (srcPanel.getY() < y2) x2Control = Math.min(srcPanel.getX(), dstPanel.getX()) - 10;
            }

            final float up1 = Math.signum(yControl - y1);
            final float up2 = Math.signum(y2 - yControl);
            // go straight
            shapes.add(new Line2D.Float(x1, y1, x1Control - 10, y1));
            // turn (up or down)
            shapes.add(new QuadCurve2D.Float(x1Control - 10, y1, x1Control, y1, x1Control, y1 + 10 * up1));
            // go straight
            shapes.add(new Line2D.Float(x1Control, y1 + 10 * up1, x1Control, yControl - 10 * up1));

            if (x1Control - x2Control >= 20) {
                // turn left
                shapes.add(new QuadCurve2D.Float(x1Control, yControl - 10 * up1, x1Control, yControl, x1Control - 10, yControl));
                // go straight
                shapes.add(new Line2D.Float(x1Control - 10, yControl, x2Control + 10, yControl));
                // turn (up or down)
                shapes.add(new QuadCurve2D.Float(x2Control + 10, yControl, x2Control, yControl, x2Control, yControl + 10 * up2));
            }
            else {
                shapes.add(new CubicCurve2D.Float(x1Control, yControl - 10 * up1, x1Control, yControl + 9 * up2, x2Control, yControl - 9 * up2, x2Control, yControl + 10 * up2));
            }
            // go straight
            shapes.add(new Line2D.Float(x2Control, yControl + 10 * up2, x2Control, y2 - 10 * up2));
            // add the close button here
            closeButton.setFrame(x2Control - 6, (int) (0.5f * (yControl + y2)) - 6, 13, 13);
            // turn right
            shapes.add(new QuadCurve2D.Float(x2Control, y2 - 10 * up2, x2Control, y2, x2Control + 10, y2));
            // go straight
            shapes.add(new Line2D.Float(x2Control + 10, y2, x2, y2));

            for (final Shape shape : shapes)
                path.append(shape.getPathIterator(null), true);
        }
    }

    @Override
    public boolean contains(final double x, final double y) {
        if (shapes.isEmpty())
            return false;

        for (final Shape shape : shapes)
            if (shape.intersects(x - 2, y - 2, 3, 3)) return true;

        return isOverCloseButton(x, y);
    }
}
