/*
 * Copyright (c) 2010-2023. Institut Pasteur.
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
import plugins.adufour.vars.lang.Var;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

public class CurvedLine extends Line {
    /**
     * Creates a line representing a link between two variables
     *
     * @param srcPanel the source panel of the link
     * @param dstPanel the destination panel of the link
     * @param link     the link between the two variables
     * @throws IllegalArgumentException if the <code>link</code> argument is <code>null</code>
     */
    public CurvedLine(final BlockPanel srcPanel, final BlockPanel dstPanel, final Link<?> link) throws IllegalArgumentException {
        super(srcPanel, dstPanel, link);
    }

    /**
     * Creates a line exposing a variable outside a work flow.
     *
     * @param srcPanel        the source panel of the link
     * @param dstPanel        the destination panel of the link
     * @param exposedVariable the exposed variable
     */
    public CurvedLine(final BlockPanel srcPanel, final BlockPanel dstPanel, final Var<?> exposedVariable) {
        super(srcPanel, dstPanel, exposedVariable);
    }

    @Override
    public void updateShape(final Path2D path, final Ellipse2D closeButton) {
        final float border = 30;
        path.append(new CubicCurve2D.Float(x1, y1, x1 + border, y1, x2 - border, y2, x2, y2), false);
        final Rectangle r = path.getBounds();
        closeButton.setFrame(r.x + r.width / 2f - 6, r.y + r.height / 2f - 6, 13, 13);
    }
}
