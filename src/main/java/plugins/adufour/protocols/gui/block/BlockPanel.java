package plugins.adufour.protocols.gui.block;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;

import icy.gui.component.button.IcyButton;
import icy.gui.component.menu.IcyMenuItem;
import icy.gui.util.LookAndFeelUtil;
import icy.system.thread.ThreadUtil;
import jiconfont.icons.google_material_design_icons.GoogleMaterialDesignIcons;
import jiconfont.swing.IconFontSwing;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.tools.input.InputBlock;
import plugins.adufour.blocks.tools.output.OutputBlock;
import plugins.adufour.blocks.util.BlockListener;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.protocols.gui.link.DragDropZone;
import plugins.adufour.vars.gui.VarEditor;
import plugins.adufour.vars.gui.model.TypeSelectionModel;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarMutable;
import plugins.adufour.vars.util.VarReferencingPolicy;

public class BlockPanel extends JPanel implements ActionListener, BlockListener {
    private final class BlockMotionManager implements MouseInputListener {
        private int pressedX, pressedY;
        private boolean dragging;
        private boolean resizing_EAST, resizing_SOUTH, resizing_NORTH, resizing_WEST;

        private boolean resizeable = false;

        @Override
        public void mouseDragged(final MouseEvent e) {
            int x = getX();
            int y = getY();
            int dx = e.getX();
            int dy = e.getY();

            int minWidth = jPanelTitle.getPreferredSize().width;
            int w = resizing_EAST ? Math.max(minWidth, dx) : getWidth();

            int minHeight = MIN_HEIGHT + 5;
            int h = resizing_SOUTH ? Math.max(minHeight, dy) : getHeight();

            if (dragging) {
                WorkFlow wf = workFlowPane.getWorkFlow();

                if (wf.isBlockSelected(blockDesc)) {
                    for (BlockDescriptor bd : wf.getBlockSelection()) {
                        BlockPanel bp = workFlowPane.getBlockPanel(bd);

                        if (bp != null) {
                            bp.setBounds(
                                    Math.max(0, dx - pressedX + bp.getX()),
                                    Math.max(0, dy - pressedY + bp.getY()),
                                    bp.getWidth(), bp.getHeight()
                            );
                        }
                    }
                }
                else
                    setBounds(Math.max(0, dx - pressedX + x), Math.max(0, dy - pressedY + y), w, h);
            }
            else if (resizeable) {
                if (resizing_WEST) {
                    if (w - dx < minWidth)
                        dx = w - minWidth;
                    x += dx;
                    w -= dx;
                }
                if (resizing_NORTH) {
                    if (h - dy < minHeight)
                        dy = h - minHeight;
                    y += dy;
                    h -= dy;
                }
                if (x >= 0 && y >= 0)
                    setBounds(x, y, w, h);
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            toFront();
            pressedX = e.getX();
            pressedY = e.getY();

            if (resizeable && !e.getSource().equals(name)) {
                if (pressedX < BORDER_SIZE_X) {
                    resizing_WEST = true;
                }
                else if (pressedX > getWidth() - BORDER_SIZE_X) {
                    resizing_EAST = true;
                }

                if (pressedY < BORDER_SIZE_Y) {
                    resizing_NORTH = true;
                }
                else if (pressedY > getHeight() - BORDER_SIZE_Y) {
                    resizing_SOUTH = true;
                }

                dragging = !(resizing_NORTH || resizing_EAST || resizing_WEST || resizing_SOUTH);
            }
            else {
                dragging = true;
            }

            if ((e.getModifiersEx() & Protocols.MENU_SHORTCUT_KEY) == Protocols.MENU_SHORTCUT_KEY) {
                getParent().dispatchEvent(e);
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            dragging = false;
            resizing_EAST = false;
            resizing_SOUTH = false;
            resizing_WEST = false;
            resizing_NORTH = false;

            if ((e.getModifiersEx() & Protocols.MENU_SHORTCUT_KEY) == Protocols.MENU_SHORTCUT_KEY) {
                getParent().dispatchEvent(e);
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            Component c = getComponentAt(e.getX(), e.getY());
            if (e.getClickCount() == 2 && !e.isConsumed() && c != null && c.equals(jPanelTitle)) {
                e.consume();

                final JPanel renamePanel = new JPanel();
                final JTextField newName = new JTextField(blockDesc.getDefinedName());
                JPanel buttonPanel = new JPanel();
                JButton ok = new JButton("OK");
                JButton cancel = new JButton("Cancel");

                ok.addActionListener(okEvent -> {
                    blockDesc.setDefinedName(newName.getText());
                    name.setText(blockDesc.getDefinedName() + " ");
                    workFlowPane.moveToBack(renamePanel);
                    workFlowPane.remove(renamePanel);

                    setSize(getPreferredSize());
                });

                cancel.addActionListener(cancelEvent -> {
                    workFlowPane.moveToBack(renamePanel);
                    workFlowPane.remove(renamePanel);
                });

                buttonPanel.add(ok);
                buttonPanel.add(cancel);

                renamePanel.setLayout(new GridLayout(2, 1));
                renamePanel.add(newName);
                renamePanel.add(buttonPanel);

                //renamePanel.setOpaque(false);
                renamePanel.setSize(new Dimension(180, 60));
                //renamePanel.setBorder(BorderFactory.createRaisedBevelBorder());
                renamePanel.setLocation(
                        jPanelTitle.getLocationOnScreen().x - workFlowPane.getLocationOnScreen().x,
                        jPanelTitle.getLocationOnScreen().y - workFlowPane.getLocationOnScreen().y - renamePanel.getHeight()
                );

                workFlowPane.add(renamePanel);
                workFlowPane.moveToFront(renamePanel);
            }
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
            // Nothing to do
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            int x = e.getX();
            int y = e.getY();

            if (resizeable && !e.getSource().equals(name)) {
                if (x < BORDER_SIZE_X) {
                    if (y < BORDER_SIZE_Y) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    }
                    else if (y > getHeight() - BORDER_SIZE_Y) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    }
                    else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                    }
                }
                else if (x > getWidth() - BORDER_SIZE_X) {
                    if (y < BORDER_SIZE_Y) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    }
                    else if (y > getHeight() - BORDER_SIZE_Y) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    }
                    else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    }
                }
                else if (y < BORDER_SIZE_Y) {
                    if (x < BORDER_SIZE_X) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    }
                    else if (x > getWidth() - BORDER_SIZE_X) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    }
                    else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    }
                }
                else if (y > getHeight() - BORDER_SIZE_Y) {
                    if (x < BORDER_SIZE_X) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
                    }
                    else if (x > getWidth() - BORDER_SIZE_X) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                    }
                    else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
                    }
                }
                else
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            else {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }

        public void setResizeable(final boolean resizeable) {
            this.resizeable = resizeable;
        }
    }

    @Deprecated
    //protected static final int SHADOW_SIZE = 4;
    protected static final int SHADOW_SIZE = 0;

    /**
     * Minimal panel height (default in collapsed mode)
     */
    protected static final int MIN_HEIGHT = 34;

    protected static final int BORDER_SIZE_X = 8;

    protected static final int BORDER_SIZE_Y = 8;

    protected int ARC_SIZE = 10;

    //@Deprecated
    //protected static final int ICON_SIZE = 16;
    protected static final int ICON_SIZE = LookAndFeelUtil.getDefaultIconSizeAsInt();

    private static final Image BLOCKS = new Protocols().getDescriptor().getIconAsImage();
    private static final ImageIcon ICON_BLOCKS = new ImageIcon(BLOCKS.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));

    private static final float ICON_SIZE_F = LookAndFeelUtil.getDefaultIconSizeAsFloat();

    /**
     * Indicates that the block is dirty (results isn't ready)
     */
    private static final ImageIcon ICON_DIRTY = new ImageIcon(IconFontSwing.buildImage(GoogleMaterialDesignIcons.WARNING, ICON_SIZE_F, Color.ORANGE.darker()));

    /**
     * Indicates that the block is currently running
     */
    private static final ImageIcon ICON_RUNNING = new ImageIcon(IconFontSwing.buildImage(GoogleMaterialDesignIcons.FLASH_ON, ICON_SIZE_F, Color.BLUE.brighter()));

    /**
     * Indicates that the block has finished running (the result is ready)
     */
    private static final ImageIcon ICON_READY = new ImageIcon(IconFontSwing.buildImage(GoogleMaterialDesignIcons.CHECK_CIRCLE, ICON_SIZE_F, Color.GREEN.darker()));

    /**
     * Indicates that the block generated an error
     */
    private static final ImageIcon ICON_ERROR = new ImageIcon(IconFontSwing.buildImage(GoogleMaterialDesignIcons.ERROR, ICON_SIZE_F, Color.RED.darker()));

    @Deprecated
    protected final JPanel jPanelTitle = new JPanel(new GridBagLayout());

    protected final JToolBar toolBar;

    protected final JPanel jPanelContent = new JPanel(new GridBagLayout());

    protected final IcyButton bMenu = new IcyButton(ICON_BLOCKS);
    protected final IcyButton bRunUntilHere = new IcyButton(GoogleMaterialDesignIcons.PLAY_ARROW);
    protected final IcyButton bID = new IcyButton("");
    protected final JLabel bStatus = new JLabel(ICON_DIRTY);
    protected final JPopupMenu menu = new JPopupMenu();
    protected final JCheckBox menuKeepResults = new JCheckBox("Remember results");
    // protected final JPanel menuCommandLineID = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    protected final IcyMenuItem menuCollapse = new IcyMenuItem("Collapse", GoogleMaterialDesignIcons.UNFOLD_LESS);
    protected final IcyMenuItem menuRemove = new IcyMenuItem("Remove block", GoogleMaterialDesignIcons.DELETE);

    //private final ShadowRenderer renderer = new ShadowRenderer(SHADOW_SIZE, 0.5f, Color.BLACK);

    //private BufferedImage backgroundImage;

    protected GradientPaint gradient;

    private final Dimension expandedSize = new Dimension();

    private final BlockMotionManager mouseAdapter = new BlockMotionManager();

    public final BlockDescriptor blockDesc;

    /**
     * The work flow container where this block panel resides
     */
    protected final WorkFlowContainer workFlowPane;

    protected final HashMap<Var<?>, DragDropZone> varDragZones = new HashMap<>();

    protected final HashMap<Var<?>, DragDropZone> varDropZones = new HashMap<>();

    protected final HashMap<Var<?>, VarEditor<?>> varEditors = new HashMap<>();

    protected final Runnable refresher;

    private final JLabel name;

    private boolean selected = false;

    /**
     * Creates a new block panel for the specified block
     *
     * @param wfPane    The work flow pane containing this panel
     * @param blockDesc The block controlled by this panel
     */
    public BlockPanel(WorkFlowContainer wfPane, BlockDescriptor blockDesc) {
        this.blockDesc = blockDesc;
        this.workFlowPane = wfPane;

        // create the label
        name = new JLabel(" " + blockDesc.getDefinedName() + " ", JLabel.CENTER);

        // Layout
        setLayout(new BorderLayout());

        // Title bar
        //jPanelTitle.setOpaque(false);
        //add(jPanelTitle, BorderLayout.NORTH);
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.add(bMenu);
        toolBar.add(bRunUntilHere);
        int id = 1 + workFlowPane.getWorkFlow().indexOf(blockDesc);
        bID.setText(String.valueOf(id));
        bID.setMargin(new Insets(0, 0, 0, 0));
        toolBar.add(bID);
        //toolBar.add(new JLabel(blockDesc.getName()));
        toolBar.add(name);
        toolBar.add(Box.createHorizontalStrut(10));
        updateStatusIcon(blockDesc.getStatus());
        toolBar.add(bStatus);

        // Need to remove all the mouse listeners to dispatch the event
        for (MouseListener ml : toolBar.getMouseListeners())
            toolBar.removeMouseListener(ml);
        for (MouseMotionListener mml : toolBar.getMouseMotionListeners())
            toolBar.removeMouseMotionListener(mml);

        add(toolBar, BorderLayout.NORTH);

        // Menu
        bMenu.setComponentPopupMenu(menu);
        for (AbstractButton button : new AbstractButton[]{bMenu, menuCollapse, bRunUntilHere, menuKeepResults, menuRemove, bID}) {
            button.addActionListener(this);
        }

        // Status
        //bStatus.setPreferredSize(new Dimension(ICON_SIZE + 3, ICON_SIZE + 3));
        //bStatus.setEnabled(false);

        // Main content
        add(jPanelContent, BorderLayout.CENTER);

        expandedSize.setSize(blockDesc.getDimension());

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        blockDesc.addBlockListener(this);

        //setBorder(BorderFactory.createLineBorder(null, 1, true));
        //setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(null, 1, true), BorderFactory.createEmptyBorder(BORDER_SIZE_Y, 0, BORDER_SIZE_Y, 0)));

        // set all panels as resizable by default
        setResizeable(true);

        setOpaque(false);
        jPanelContent.setOpaque(false);
        toolBar.setOpaque(false);

        refresher = () -> {
            ThreadUtil.invokeNow(this::refreshNow);

            // we don't want to spent too much time with refresh so we force sleep here
            ThreadUtil.sleep(100);
        };
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Dimension arcs = new Dimension(ARC_SIZE, ARC_SIZE);
        final int width = getWidth();
        final int height = getHeight();
        final Graphics2D graphics = (Graphics2D) g;

        //Sets antialiasing
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final boolean isDarkMode = LookAndFeelUtil.isDarkMode();
        final Color bgcRoot = LookAndFeelUtil.getUIDefaults().getColor("RootPane.background");

        if (bgcRoot == null)
            return;

        final Color bgc;
        if (isDarkMode)
            bgc = new Color(bgcRoot.getRed() + 50, bgcRoot.getGreen() + 50, bgcRoot.getBlue() + 50);
        else
            bgc = new Color(bgcRoot.getRed() - 20, bgcRoot.getGreen() - 20, bgcRoot.getBlue() - 20);

        //Draws the rounded opaque panel with borders.
        graphics.setColor(bgc);
        graphics.fillRoundRect(0, 0, width, height, arcs.width, arcs.height);

        if (toolBar == null)
            return;

        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, getForeground()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == bMenu) {
            bMenu.getComponentPopupMenu().show(bMenu, 0, bMenu.getHeight());
        }
        else if (e.getSource() == bStatus && blockDesc.getStatus() == BlockStatus.RUNNING) {
            // stop the work flow prematurely
            blockDesc.setFinalBlock(true);
            workFlowPane.getWorkFlow().interrupt();
        }
        else if (e.getSource() == menuRemove) {
            workFlowPane.getWorkFlow().removeBlock(blockDesc, false);
        }
        else if (e.getSource() == menuCollapse) {
            blockDesc.setCollapsed(!blockDesc.isCollapsed());
        }
        else if (e.getSource() == bRunUntilHere || e.getSource() == bID) {
            // Make the block a priority (recursively)

            WorkFlow workflow = null;
            BlockDescriptor tmp = blockDesc;

            while (tmp.getContainer() != null) {
                // get the block container
                workflow = tmp.getContainer();

                // make the block a priority
                if (e.getSource() == bID)
                    workflow.prioritize(tmp);

                // up-stream recursion
                tmp = workflow.getBlockDescriptor();
            }

            if (e.getSource() == bRunUntilHere) {
                blockDesc.setFinalBlock(true);
                if (workflow != null)
                    workflow.runWorkFlow();
            }
        }
        else if (e.getSource() == menuKeepResults) {
            blockDesc.keepResults(!blockDesc.keepsResults());
            menuKeepResults.setSelected(blockDesc.keepsResults());

            // reset now if necessary
            if (!blockDesc.keepsResults())
                blockDesc.reset();
        }
    }

    @Override
    public void blockCollapsed(BlockDescriptor block, final boolean collapsed) {
        ThreadUtil.invokeLater(() -> {
            // redraw the panel in minimised mode
            refreshNow();
            // change the status icon
            menuCollapse.setText(blockDesc.isCollapsed() ? "Expand" : "Collapse");
            //menuCollapse.setIcon(collapsed ? ICON_EXPAND : ICON_COLLAPSE);
            // set its resizability
            setResizeable(!collapsed);
            // update the preferred size of the container for scrolling purposes
            workFlowPane.updatePreferredSize();
        });
    }

    @Override
    public void blockDimensionChanged(BlockDescriptor block, int newWidth, int newHeight) {

    }

    @Override
    public void blockLocationChanged(BlockDescriptor block, int newX, int newY) {

    }

    @Override
    public void blockStatusChanged(BlockDescriptor blockInfo, BlockStatus status) {
        updateStatusIcon(status);
    }

    @Override
    public void blockVariableAdded(BlockDescriptor block, Var<?> variable) {
        ThreadUtil.invokeNow(this::refreshNow);
        // refresh();
    }

    @Override
    public <T> void blockVariableChanged(BlockDescriptor block, Var<T> variable, T newValue) {

    }

    protected DragDropZone createVarDragZone(final Var<?> variable, boolean allowExposure) {
        final DragDropZone dragZone = DragDropZone.createDragZone(workFlowPane.getWorkFlow(), blockDesc, variable);

        if (allowExposure) {
            final JPopupMenu popup = new JPopupMenu();
            final JMenuItem itemExposeVar = new JMenuItem("Expose variable");
            popup.add(itemExposeVar);

            dragZone.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // don't show if the owner is the top-level work flow
                    WorkFlow parent = blockDesc.getContainer();

                    if (parent.getBlockDescriptor().isTopLevelWorkFlow())
                        return;

                    if (e.getButton() == MouseEvent.BUTTON2 || e.isMetaDown()) {
                        VarList parentOutputs = parent.getBlockDescriptor().outputVars;
                        itemExposeVar.setText(parentOutputs.isVisible(variable) ? "Hide variable" : "Expose variable");
                        popup.show(dragZone, 5, 10);
                    }
                }
            });

            itemExposeVar.addActionListener(arg0 -> ThreadUtil.bgRun(() -> {
                VarList outputWorkFlowVars = blockDesc.getContainer().getBlockDescriptor().outputVars;
                outputWorkFlowVars.setVisible(variable, !outputWorkFlowVars.isVisible(variable));
            }));
        }

        return dragZone;
    }

    @Deprecated
    protected DragDropZone createVarDropZone(final Var<?> variable, final boolean allowExposure, final Image icon) {
        return createVarDropZone(variable, allowExposure);
    }

    protected DragDropZone createVarDropZone(final Var<?> variable, final boolean allowExposure) {
        final DragDropZone dropZone = DragDropZone.createDropZone(workFlowPane.getWorkFlow(), blockDesc, variable);

        if (allowExposure) {
            final JPopupMenu popup = new JPopupMenu();
            final JMenuItem itemExposeVar = new JMenuItem("Expose variable");
            popup.add(itemExposeVar);

            final WorkFlow parent = blockDesc.getContainer();

            dropZone.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // don't show if:
                    // 1) the owner is the top-level work flow
                    if (parent.getBlockDescriptor().isTopLevelWorkFlow())
                        return;
                    // 2) the variable is already linked to something else
                    if (variable.getReference() != null)
                        return;

                    if (e.getButton() == MouseEvent.BUTTON2 || e.isMetaDown()) {
                        VarList parentInputs = parent.getBlockDescriptor().inputVars;
                        itemExposeVar.setText(parentInputs.isVisible(variable) ? "Hide variable" : "Expose variable");
                        popup.show(dropZone, 5, 10);
                    }
                }
            });

            itemExposeVar.addActionListener(arg0 -> ThreadUtil.bgRun(() -> {
                VarList inputWorkFlowVars = blockDesc.getContainer().getBlockDescriptor().inputVars;
                inputWorkFlowVars.setVisible(variable, !inputWorkFlowVars.isVisible(variable));
            }));
        }

        return dropZone;
    }

    protected <T> VarEditor<T> createVarEditor(Var<T> variable) {
        VarEditor<T> editor = variable.isEnabled() ? variable.createVarEditor() : variable.createVarViewer();
        editor.setEnabled(true); // activates listeners
        return editor;
    }

    void dispose() {
        setVisible(false);

        removeAll();

        getUI().uninstallUI(this);

        // clean resources
        for (AbstractButton button : new AbstractButton[]{bMenu, menuCollapse, bRunUntilHere, menuKeepResults, menuRemove, bID}) {
            button.removeActionListener(this);
        }
        removeMouseListener(mouseAdapter);
        removeMouseMotionListener(mouseAdapter);
        blockDesc.removeBlockListener(this);

        for (VarEditor<?> editor : varEditors.values())
            editor.dispose();
        varEditors.clear();

        for (DragDropZone dp : varDropZones.values())
            dp.dispose();
        varDropZones.clear();

        for (DragDropZone dg : varDragZones.values())
            dg.dispose();
        varDragZones.clear();
    }

    public void drawPanel() {
        Dimension oldSize = getSize();

        if (oldSize.width == 0 || oldSize.height == 0) {
            // first run, create components just in case
            drawContent();
        }

        jPanelContent.removeAll();
        jPanelTitle.removeAll();
        menu.removeAll();

        drawMenu();
        drawTitle();

        if (!blockDesc.isCollapsed()) {
            drawContent();
            if (expandedSize.width == 0 || expandedSize.height == 0)
                expandedSize.setSize(getPreferredSize());
            oldSize = expandedSize;
        }
        else {
            if (oldSize.width == 0 || oldSize.height == 0) {
                // check if the descriptor has a stored size
                if (!blockDesc.isCollapsed() && blockDesc.getDimension().width != 0) {
                    // use it
                    oldSize = blockDesc.getDimension();
                }
                else {
                    // use the default preferred size
                    oldSize = getPreferredSize();
                }
            }
            else
                oldSize = getPreferredSize();
        }

        setBounds(blockDesc.getLocation().x, blockDesc.getLocation().y, oldSize.width, oldSize.height);
    }

    protected void drawMenu() {
        // collapse / expand
        menu.add(menuCollapse);
        menuCollapse.setText(blockDesc.isCollapsed() ? "Expand" : "Collapse");
        //menuCollapse.setIcon(blockDesc.isCollapsed() ? ICON_EXPAND : ICON_COLLAPSE);
        menuCollapse.setToolTipText("Expand or collapse this block");

        menu.addSeparator();

        if ((blockDesc.getBlock() instanceof InputBlock) || (blockDesc.getBlock() instanceof OutputBlock)) {
            // option to retrieve parameter from command line
            String tooltip = "Identifier used to set the value from the command line";

            JPanel menuCLID = new JPanel(new FlowLayout(FlowLayout.LEADING));
            //menuCLID.setOpaque(false);
            menuCLID.setToolTipText(tooltip);

            menuCLID.add(new JLabel("ID"));

            final JTextField fieldID = new JTextField(blockDesc.getCommandLineID());
            fieldID.setPreferredSize(new Dimension(80, 20));
            fieldID.setToolTipText(tooltip);
            menuCLID.add(fieldID);

            IcyButton bStoreFieldID = new IcyButton(GoogleMaterialDesignIcons.CHECK);
            bStoreFieldID.setFlat(true);
            //bStoreFieldID.setFocusable(false);
            //bStoreFieldID.setBorderPainted(false);
            bStoreFieldID.setToolTipText("Save the new ID");
            bStoreFieldID.addActionListener(e -> blockDesc.setCommandLineID(fieldID.getText()));
            menuCLID.add(bStoreFieldID);

            menu.add(menuCLID);
        }
        else {
            // option to keep previous results in memory
            menu.add(menuKeepResults);
            menuKeepResults.setSelected(blockDesc.keepsResults());
        }
        menu.addSeparator();

        // remove the block
        menu.add(menuRemove);
        //menuRemove.setIcon(ICON_REMOVE);
        menuRemove.setToolTipText("Remove this block from the protocol");
    }

    protected void drawContent() {
        int row = 1;

        for (final Var<?> input : blockDesc.inputVars) {
            if (!varDropZones.containsKey(input))
                varDropZones.put(input, createVarDropZone(input, true));
            if (!varEditors.containsKey(input))
                varEditors.put(input, createVarEditor(input));
            if (!varDragZones.containsKey(input))
                varDragZones.put(input, createVarDragZone(input, false));

            JLabel inputName = new JLabel(input.getName());
            Dimension inputNameDim = inputName.getPreferredSize();
            inputNameDim.height = 20;
            inputName.setPreferredSize(inputNameDim);

            VarEditor<?> varEditor = varEditors.get(input);

            JComponent inputComponent = (JComponent) varEditor.getEditorComponent();

            VarReferencingPolicy policy = input.getReferencingPolicy();

            if (policy == VarReferencingPolicy.IN || policy == VarReferencingPolicy.BOTH) {
                GridBagConstraints gbc_dropZone = new GridBagConstraints();
                gbc_dropZone.anchor = GridBagConstraints.WEST;
                gbc_dropZone.fill = GridBagConstraints.NONE;
                gbc_dropZone.insets = new Insets(2, 0, 0, 0);
                gbc_dropZone.gridx = 0;
                gbc_dropZone.gridy = row;
                jPanelContent.add(varDropZones.get(input), gbc_dropZone);
            }

            if (!blockDesc.isSingleBlock() && varEditor.isNameVisible()) {
                GridBagConstraints gbc_inputName = new GridBagConstraints();
                gbc_inputName.anchor = GridBagConstraints.CENTER;
                gbc_inputName.fill = GridBagConstraints.HORIZONTAL;
                gbc_inputName.insets = new Insets(2, 5, 0, 0);
                gbc_inputName.gridx = 1;
                gbc_inputName.gridy = row;
                gbc_inputName.weightx = 0;
                jPanelContent.add(inputName, gbc_inputName);
            }

            if (inputComponent != null) {
                GridBagConstraints gbc_inputComponent = new GridBagConstraints();
                gbc_inputComponent.anchor = GridBagConstraints.CENTER;
                gbc_inputComponent.fill = GridBagConstraints.BOTH;
                gbc_inputComponent.insets = new Insets(2, 5, 0, 0);

                if (!blockDesc.isSingleBlock() && varEditor.isNameVisible()) {
                    gbc_inputComponent.gridwidth = 1;
                    gbc_inputComponent.gridx = 2;
                }
                else {
                    gbc_inputComponent.gridwidth = 2;
                    gbc_inputComponent.gridx = 1;
                }
                gbc_inputComponent.gridy = row;
                gbc_inputComponent.weightx = varEditor.getComponentHorizontalResizeFactor();
                gbc_inputComponent.weighty = varEditor.getComponentVerticalResizeFactor();

                inputComponent.setOpaque(varEditor.isComponentOpaque());
                inputComponent.setFocusable(varEditor.isComponentFocusable());
                inputComponent.setEnabled(varEditor.isComponentEnabled());
                inputComponent.setPreferredSize(varEditor.getPreferredSize());

                jPanelContent.add(inputComponent, gbc_inputComponent);
            }

            if (policy == VarReferencingPolicy.OUT || policy == VarReferencingPolicy.BOTH) {
                GridBagConstraints gbc_dragZone = new GridBagConstraints();
                gbc_dragZone.anchor = GridBagConstraints.EAST;
                gbc_dragZone.fill = GridBagConstraints.NONE;
                gbc_dragZone.insets = new Insets(2, 5, 0, 0);
                gbc_dragZone.gridx = 3;
                gbc_dragZone.gridy = row;
                jPanelContent.add(varDragZones.get(input), gbc_dragZone);
            }
            row++;
        }

        for (Var<?> output : blockDesc.outputVars) {
            GridBagConstraints gbc_outputName = new GridBagConstraints();
            gbc_outputName.fill = GridBagConstraints.NONE;
            gbc_outputName.anchor = GridBagConstraints.EAST;
            gbc_outputName.insets = new Insets(2, 0, 0, 0);
            gbc_outputName.gridx = 2;
            gbc_outputName.gridy = row;
            if (blockDesc.inputVars.size() == 0) {
                gbc_outputName.weightx = 1;
                gbc_outputName.insets.right = 5;
            }

            JLabel label = new JLabel(output.getName());
            Dimension dim = label.getPreferredSize();
            dim.height = 20;
            label.setPreferredSize(dim);

            if (output instanceof VarMutable && output.getDefaultEditorModel() instanceof TypeSelectionModel) {
                if (!varEditors.containsKey(output))
                    varEditors.put(output, createVarEditor(output));

                JPanel panel = new JPanel(new FlowLayout());
                panel.add(label);
                panel.add((JComponent) varEditors.get(output).getEditorComponent());
                //panel.setOpaque(false);
                jPanelContent.add(panel, gbc_outputName);
            }
            else {
                jPanelContent.add(label, gbc_outputName);
            }

            if (!varDragZones.containsKey(output))
                varDragZones.put(output, createVarDragZone(output, true));
            GridBagConstraints gbc_dragZone = new GridBagConstraints();
            gbc_dragZone.anchor = GridBagConstraints.EAST;
            gbc_dragZone.fill = GridBagConstraints.NONE;
            gbc_dragZone.insets = new Insets(2, 0, 0, 0);
            gbc_dragZone.gridx = 3;
            gbc_dragZone.gridy = row;
            jPanelContent.add(varDragZones.get(output), gbc_dragZone);

            row++;
        }
    }

    protected void drawTitle() {
        int id = 1 + workFlowPane.getWorkFlow().indexOf(blockDesc);
        bID.setText(String.valueOf(id));
        updateStatusIcon(blockDesc.getStatus());
    }

    @Deprecated
    protected void drawTitleOld() {
        GridBagConstraints gbc_icon = new GridBagConstraints();
        gbc_icon.insets = new Insets(0, BORDER_SIZE_X, 4, 2);
        gbc_icon.gridx = 0;
        gbc_icon.gridy = 0;
        gbc_icon.anchor = GridBagConstraints.WEST;
        gbc_icon.fill = GridBagConstraints.NONE;
        jPanelTitle.add(bMenu, gbc_icon);

        gbc_icon.insets = new Insets(0, -1, 4, 2);
        gbc_icon.gridx = 1;
        jPanelTitle.add(bRunUntilHere, gbc_icon);

        bRunUntilHere.setToolTipText("<html><h4>Run the work-flow until this block (inclusive)</h4></html>");

        GridBagConstraints gbc_name = new GridBagConstraints();
        gbc_name.insets = new Insets(0, 3, 4, 3);
        gbc_name.anchor = GridBagConstraints.CENTER;
        gbc_name.fill = GridBagConstraints.HORIZONTAL;
        gbc_name.gridx = 2;
        gbc_name.gridy = 0;
        gbc_name.weightx = 1;
        name.setFont(name.getFont().deriveFont(Font.BOLD + Font.ITALIC, 12));
        jPanelTitle.add(name, gbc_name);

        int id = 1 + workFlowPane.getWorkFlow().indexOf(blockDesc);
        GridBagConstraints gbc_id = new GridBagConstraints();
        gbc_id.insets = new Insets(0, 0, 4, 0);
        gbc_id.gridx = 3;
        gbc_id.gridy = 0;
        gbc_id.anchor = GridBagConstraints.EAST;
        gbc_id.fill = GridBagConstraints.HORIZONTAL;
        //BufferedImage idImage = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        //Graphics2D g = idImage.createGraphics();
        //g.setColor(Color.black);
        //g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        //g.setFont(new Font(id > 99 ? "Arial Narrow" : "Arial", Font.BOLD, id > 99 ? 12 : 13));
        //GraphicsUtil.drawCenteredString(g, "" + id, ICON_SIZE / 2, 1 + ICON_SIZE / 2, false);
        //bID.setIcon(new ImageIcon(idImage));
        bID.setText(String.valueOf(id));
        jPanelTitle.add(bID, gbc_id);

        GridBagConstraints gbc_right = new GridBagConstraints();
        gbc_right.insets = new Insets(0, 0, 4, 6);
        gbc_right.gridx = 4;
        gbc_right.gridy = 0;
        gbc_right.anchor = GridBagConstraints.EAST;
        gbc_right.fill = GridBagConstraints.HORIZONTAL;
        jPanelTitle.add(bStatus, gbc_right);

        updateStatusIcon(blockDesc.getStatus());
    }

    private void updateStatusIcon(BlockStatus status) {
        switch (status) {
            case READY:
                bStatus.setIcon(ICON_READY);
                break;
            case DIRTY:
                bStatus.setIcon(ICON_DIRTY);
                break;
            case RUNNING:
                bStatus.setIcon(ICON_RUNNING);
                break;
            case ERROR:
                bStatus.setIcon(ICON_ERROR);
        }

        bStatus.setToolTipText("<html>" + status.toString().replace("\n", "<br/>") + "</html>");
    }

    /**
     * @return the {@link WorkFlowContainer} object containing this panel
     */
    public WorkFlowContainer getWorkFlowContainer() {
        return workFlowPane;
    }

    public DragDropZone getDragZone(Var<?> input) {
        return varDragZones.get(input);
    }

    public DragDropZone getDropZone(Var<?> output) {
        return varDropZones.get(output);
    }

    /**
     * @param output the output variable to retrieve
     * @return The right-hand border of the drag zone for the specifed output variable (or the edge
     * of the panel if minimized). Coordinates are given relatively to this panel's top
     * left-hand corner
     */
    public Point getDragZoneLocation(Var<?> output) {
        if (blockDesc.isCollapsed() && !(this instanceof WorkFlowPanel))
            return new Point(getX() + getWidth() - SHADOW_SIZE, getY() + getHeight() / 2);

        DragDropZone dz = getDragZone(output);
        if (!dz.isVisible())
            return new Point(getX() + getWidth() - SHADOW_SIZE, getY() + getHeight() / 2);

        int x = dz.getLocationOnScreen().x - getParent().getLocationOnScreen().x;
        int y = dz.getLocationOnScreen().y - getParent().getLocationOnScreen().y;

        return new Point(x + dz.getWidth(), y + dz.getHeight() / 2);
    }

    public Point getDropZoneLocation(Var<?> input) {
        if (blockDesc.isCollapsed() && !(this instanceof WorkFlowPanel))
            return new Point(getX() + SHADOW_SIZE, getY() + getHeight() / 2);

        DragDropZone dz = getDropZone(input);
        if (!dz.isVisible())
            return new Point(getX() + SHADOW_SIZE, getY() + getHeight() / 2);

        int x = dz.getLocationOnScreen().x - getParent().getLocationOnScreen().x;
        int y = dz.getLocationOnScreen().y - getParent().getLocationOnScreen().y;
        return new Point(x, y + dz.getHeight() / 2);
    }

    /**
     * @return the height of the area in the panel containing the variables. By default, this is
     * equal to the actual height of the panel, but may be overridden to provide a specific
     * implementation (e.g. loops)
     */
    public int getVarPanelHeight() {
        return getHeight();
    }

    /**
     * @return the width of the area in the panel containing the variables. By default, this is
     * equal to the actual width of the panel, but may be overridden to provide a specific
     * implementation (e.g. loops)
     */
    public int getVarPanelWidth() {
        return getWidth();
    }

    @Override
    public Dimension getPreferredSize() {
        if (blockDesc.isCollapsed())
            return new Dimension(jPanelTitle.getPreferredSize().width, MIN_HEIGHT);

        return super.getPreferredSize();
    }

    public VarEditor<?> getVarEditor(Var<?> variable) {
        return varEditors.get(variable);
    }

    public void refresh() {
        // lazy refresh so we don't spent too much time with it
        ThreadUtil.bgRunSingle(refresher);
    }

    public void refreshNow() {
        setVisible(false);
        drawPanel();
        setVisible(true);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        if (width == getWidth() && height == getHeight() && x == getX() && y == getY())
            return;

        if (width != getWidth() || height != getHeight()) {
            // Store new values for XML I/O
            if (!blockDesc.isCollapsed())
                expandedSize.setSize(width, height);

            blockDesc.setDimension(expandedSize.width, expandedSize.height);
        }

        // Store new values for XML I/O
        blockDesc.setLocation(x, y);

        super.setBounds(x, y, width, height);
    }

    protected void setResizeable(boolean resizeable) {
        mouseAdapter.setResizeable(resizeable);
    }

    public void toFront() {
        if (getParent() instanceof JLayeredPane) {
            // Layer 0 for blocks
            // Layer 1 for work flows
            ((JLayeredPane) getParent()).setLayer(this, blockDesc.isWorkFlow() ? 1 : 0, 0);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean getSelected() {
        return selected;
    }
}
