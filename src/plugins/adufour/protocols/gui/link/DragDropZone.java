package plugins.adufour.protocols.gui.link;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.TransferHandler;

import icy.resource.ResourceUtil;
import icy.sequence.Sequence;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarGenericArray;
import plugins.adufour.vars.util.MutableType;
import plugins.adufour.vars.util.TypeChangeListener;
import plugins.adufour.vars.util.VarListener;

@SuppressWarnings("serial")
public abstract class DragDropZone extends JButton implements VarListener<Boolean>, TypeChangeListener
{
    protected static final int DRAGnDROP_ACTION = TransferHandler.LINK;

    public static final int DEFAULT_ICON_SIZE = 13;

    public static final Image LINK_RIGHT = ResourceUtil.getAlphaIconAsImage("round_arrow_right.png");

    public static final Image LINK_LEFT = ResourceUtil.getAlphaIconAsImage("round_arrow_left.png");

    public static final Image PIN_LEFT = ResourceUtil.getAlphaIconAsImage("pin_map_left.png");

    public static final Image PIN_RIGHT = ResourceUtil.getAlphaIconAsImage("pin_map_right.png");

    public static final Image BUTTON = ResourceUtil.getAlphaIconAsImage("round.png");

    private final Var<?> variable;

    private int iconSize = DEFAULT_ICON_SIZE;

    private Icon defaultIcon;

    private final Icon errorIcon;

    private final Icon emptyIcon = new ImageIcon(ResourceUtil.ICON_NULL);

    /**
     * Creates a new drop zone for the given variable
     * 
     * @param flowPane
     *        the flow pane where variable links will come from
     * @param block
     *        the box that owns the specified input
     * @param input
     *        the input variable to link to
     * @return
     */
    public static <T> DragDropZone createDropZone(Image image, WorkFlow workFlow, BlockDescriptor block, Var<T> input)
    {
        return new DropZone<T>(image, workFlow, block, input);
    }

    /**
     * Creates a new drag zone for the given variable
     * 
     * @param workFlow
     *        the work flow where variable links will go to
     * @param block
     *        the box that owns the specified output
     * @param variable
     *        the input variable to link
     * @return
     */
    public static <T> DragDropZone createDragZone(Image image, WorkFlow workFlow, BlockDescriptor block,
            Var<T> variable)
    {
        return new DragZone<T>(image, workFlow, block, variable);
    }

    protected DragDropZone(String toolTipText, Image image, Var<?> variable)
    {
        super("");
        this.variable = variable;

        if (variable instanceof MutableType)
            ((MutableType) variable).addTypeChangeListener(this);

        setToolTipText("<html><h4>" + toolTipText.replace("\n", "<br/>") + "</h4></html>");
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

    private void updateDefaultIcon()
    {
        Class<?> dataType = null;

        if (variable != null)
            dataType = variable.getType();

        if (dataType != null)
        {
            if (variable instanceof VarGenericArray)
            {
                VarGenericArray<?> array = (VarGenericArray<?>) variable;
                dataType = array.getInnerType();
            }
            else if (dataType.isArray())
            {
                dataType = dataType.getComponentType();
            }
        }

        defaultIcon = getLinkIcon(getColor(dataType));
    }

    public Icon getLinkIcon(Color color)
    {
        Image myImage = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) myImage.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw the colored background
        g.setColor(color);
        g.fillOval(0, 0, iconSize, iconSize);

        // draw the white arrow
        g.setColor(Color.white);
        Path2D.Float arrow = new Path2D.Float();
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

    public void setEmptyIcon()
    {
        setIcon(emptyIcon);
    }

    public void setDefaultLinkIcon()
    {
        setIcon(defaultIcon);
    }

    public void setErrorLinkIcon()
    {
        setIcon(errorIcon);
    }

    public void setIconSize(int size)
    {
        // this.iconSize = size;
        setDefaultLinkIcon();
    }

    public void dispose()
    {
        if (variable instanceof MutableType)
            ((MutableType) variable).removeTypeChangeListener(this);
    }

    @Override
    public void valueChanged(Var<Boolean> source, Boolean oldValue, Boolean newValue)
    {
        setDefaultLinkIcon();
    }

    @Override
    public void referenceChanged(Var<Boolean> source, Var<? extends Boolean> oldReference,
            Var<? extends Boolean> newReference)
    {
        setDefaultLinkIcon();
    }

    @Override
    public void typeChanged(Object source, Class<?> oldType, Class<?> newType)
    {
        updateDefaultIcon();
        setDefaultLinkIcon();
    }

    public static Color getColor(Class<?> type)
    {
        if (type == null)
            return Color.gray;

        if (type == Sequence.class)
            return Color.blue;

        if (type == Boolean.class || type == Boolean.TYPE)
            return Color.orange.darker().darker();

        if (type.isPrimitive() || Number.class.isAssignableFrom(type))
            return Color.magenta.darker().darker();

        return Color.black;
    }
}
