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

package plugins.adufour.protocols.gui.link;

import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.protocols.gui.block.BlockPanel;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

public class ReLoopLine extends RoundedSquareLine {
    public ReLoopLine(final BlockPanel srcPanel, final BlockPanel dstPanel, final Link<?> link) {
        super(srcPanel, dstPanel, link);
    }

    @Override
    public void updateShape(final @NotNull Path2D path, final @NotNull Ellipse2D closeButton) {
        path.moveTo(x1, y1);

        final float xControl = Math.max(x1 + 10, x2 + 10);
        final float yControl = 0.5f * (y1 + y2);

        path.curveTo(xControl, y1, xControl, y1, xControl, yControl);
        path.curveTo(xControl, y2, xControl, y2, x2, y2);

        final Rectangle r = path.getBounds();
        closeButton.setFrame(r.x + r.width / 2f - 6, r.y + r.height / 2f - 6, 13, 13);
    }
}
