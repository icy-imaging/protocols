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

import fr.icy.model.roi.ROI;
import fr.icy.model.sequence.Sequence;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarGenericArray;
import plugins.adufour.vars.util.MutableType;
import plugins.adufour.vars.util.TypeChangeListener;
import plugins.adufour.vars.util.VarListener;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;

public abstract class DragDropZone extends JButton implements VarListener<Boolean>, TypeChangeListener {
    protected static final int DRAGnDROP_ACTION = TransferHandler.LINK;

    public static final int DEFAULT_ICON_SIZE = 13;

    private final Var<?> variable;

    private final int iconSize = DEFAULT_ICON_SIZE;

    private Icon defaultIcon;

    private final Icon errorIcon;

    /**
     * Creates a new drop zone for the given variable
     *
     * @param block the box that owns the specified input
     * @param input the input variable to link to
     * @return the drop zone
     */
    @Contract("_, _, _ -> new")
    public static <T> @NotNull DragDropZone createDropZone(final WorkFlow workFlow, final BlockDescriptor block, final Var<T> input) {
        return new DropZone<>(workFlow, block, input);
    }

    /**
     * Creates a new drag zone for the given variable
     *
     * @param workFlow the work flow where variable links will go to
     * @param block    the box that owns the specified output
     * @param variable the input variable to link
     * @return the drag zone
     */
    @Contract("_, _, _ -> new")
    public static <T> @NotNull DragDropZone createDragZone(final WorkFlow workFlow, final BlockDescriptor block, final Var<T> variable) {
        return new DragZone<>(workFlow, block, variable);
    }

    protected DragDropZone(final String toolTipText, final Var<?> variable) {
        super();
        this.variable = variable;

        if (variable instanceof MutableType)
            ((MutableType) variable).addTypeChangeListener(this);

        setToolTipText(toolTipText);
        setDefaultLinkIcon();
        setContentAreaFilled(false);
        setFocusable(false);
        setBorderPainted(false);

        errorIcon = getLinkIcon(Color.red);

        updateDefaultIcon();
        setDefaultLinkIcon();

        setPreferredSize(new Dimension(iconSize, iconSize));
        setMinimumSize(getPreferredSize());
    }

    private void updateDefaultIcon() {
        Class<?> dataType = null;

        if (variable != null)
            dataType = variable.getType();

        if (dataType != null) {
            if (variable instanceof final VarGenericArray<?> array) {
                dataType = array.getInnerType();
            }
            else if (dataType.isArray()) {
                dataType = dataType.getComponentType();
            }
        }

        defaultIcon = getLinkIcon(getColor(dataType));
    }

    public Icon getLinkIcon(final Color color) {
        final Image myImage = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = (Graphics2D) myImage.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw the colored background
        g.setColor(color);
        g.fillOval(0, 0, iconSize, iconSize);

        // draw the white arrow
        g.setColor(Color.white);
        final Path2D.Float arrow = new Path2D.Float();
        arrow.moveTo(2.5f, 5);
        arrow.lineTo(6, 5);
        arrow.lineTo(6, 2.5f);
        arrow.lineTo(11, 6.5f);
        arrow.lineTo(6, 10.5f);
        arrow.lineTo(6, 8);
        arrow.lineTo(2.5f, 8);
        arrow.closePath();
        g.fill(arrow);
        return new ImageIcon(myImage);
    }

    public void setDefaultLinkIcon() {
        setIcon(defaultIcon);
    }

    public void setErrorLinkIcon() {
        setIcon(errorIcon);
    }

    public void dispose() {
        if (variable instanceof MutableType)
            ((MutableType) variable).removeTypeChangeListener(this);
    }

    @Override
    public void valueChanged(final Var<Boolean> source, final Boolean oldValue, final Boolean newValue) {
        setDefaultLinkIcon();
    }

    @Override
    public void referenceChanged(final Var<Boolean> source, final Var<? extends Boolean> oldReference, final Var<? extends Boolean> newReference) {
        setDefaultLinkIcon();
    }

    @Override
    public void typeChanged(final Object source, final Class<?> oldType, final Class<?> newType) {
        updateDefaultIcon();
        setDefaultLinkIcon();
    }

    public static Color getColor(final Class<?> type) {
        final Color cNull = Color.GRAY;
        final Color cSequence = new Color(31, 72, 176);
        final Color cBoolean = new Color(229, 148, 0);
        final Color cPrimitive = new Color(156, 90, 156);
        final Color cString = new Color(58, 158, 142);
        final Color cROI = new Color(151, 26, 59);
        //final Color cOther = new Color(53, 123, 53);
        final Color cFile = new Color(53, 123, 53);
        final Color cEnum = new Color(181, 178, 9);

        if (type == null)
            return cNull;

        if (type == Sequence.class)
            return cSequence;

        if (type == Boolean.class || type == Boolean.TYPE)
            return cBoolean;

        if (type.isPrimitive() || Number.class.isAssignableFrom(type))
            return cPrimitive;

        if (type == String.class)
            return cString;

        if (type == ROI.class || type == ROI[].class)
            return cROI;

        if (type == File.class || type == File[].class)
            return cFile;

        if (type.isEnum())
            return cEnum;

        //return cOther;

        final int hash = type.hashCode();
        final int r = (hash & 0xFF0000) >> 16;
        final int g = (hash & 0x00FF00) >> 8;
        final int b = hash & 0x0000FF;
        return new Color(r, g, b);
    }
}
