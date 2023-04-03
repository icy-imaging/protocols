package plugins.adufour.protocols.gui.block;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;

import icy.file.FileUtil;
import icy.file.Loader;
import icy.file.SequenceFileImporter;
import icy.gui.dialog.ConfirmDialog;
import icy.system.thread.ThreadUtil;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.Loop;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.util.BlocksFinder;
import plugins.adufour.blocks.util.BlocksFinder.DND_MenuItem;
import plugins.adufour.blocks.util.LinkCutException;
import plugins.adufour.blocks.util.WorkFlowListener;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.protocols.gui.MainFrame;
import plugins.adufour.protocols.gui.link.Line;
import plugins.adufour.protocols.gui.link.RoundedSquareLine;
import plugins.adufour.vars.lang.Var;

public class WorkFlowContainer extends JLayeredPane implements WorkFlowListener, DropTargetListener, KeyListener {
    protected class BlockSelector extends MouseAdapter {
        int pressedX, pressedY;
        ArrayList<Link<?>> currentlySelectedLinks = new ArrayList<>();
        ArrayList<BlockDescriptor> currentlySelectedBlocks = new ArrayList<>();

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (!((e.getModifiersEx() & Protocols.MENU_SHORTCUT_KEY) == Protocols.MENU_SHORTCUT_KEY)) {
                    workFlow.newSelection();
                    for (BlockPanel bp : blockPanels.values())
                        bp.setSelected(false);
                    for (Line l : linkLines.values())
                        l.setSelected(false);
                }

                if (e.getComponent() instanceof BlockPanel) {
                    BlockDescriptor bd = ((BlockPanel) e.getComponent()).blockDesc;

                    if (!currentlySelectedBlocks.contains(bd) && !workFlow.isBlockSelected(bd)) {
                        currentlySelectedBlocks.add(bd);
                        blockPanels.get(bd).setSelected(true);
                    }
                    else {
                        currentlySelectedBlocks.remove(bd);
                        workFlow.unselectBlock(bd);
                        blockPanels.get(bd).setSelected(false);
                    }

                    selectLinks();
                }
                else {
                    currentlySelectedBlocks = new ArrayList<>();
                    currentlySelectedLinks = new ArrayList<>();
                    graphicSelectionHint = new Rectangle();
                }

                pressedX = e.getX();
                pressedY = e.getY();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Rectangle selection = new Rectangle(Math.min(e.getX(), pressedX), Math.min(e.getY(), pressedY),
                        Math.abs(pressedX - e.getX()), Math.abs(pressedY - e.getY()));

                selectBlocks(selection);

                graphicSelectionHint = selection;

                revalidate();
                repaint();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                validateSelection();

                requestFocus();
                revalidate();
                repaint();
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON2 || e.isMetaDown()) {
                popupMenu.removeAll();
                new BlocksFinder().createJMenu(popupMenu, WorkFlowContainer.this, e.getPoint());
                popupMenu.show(WorkFlowContainer.this, e.getX(), e.getY());
            }
            else {
                for (Link<?> link : linkLines.keySet())
                    if (linkLines.get(link).isOverCloseButton(e.getPoint())) {
                        workFlow.removeLink(link.dstVar);
                        // prevent concurrent modification
                        break;
                    }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            for (Line l : linkLines.values())
                l.setCustomColor(l.contains(e.getX(), e.getY()));
        }

        private void selectBlocks(Rectangle rectangle) {
            // block selection
            boolean containsBlock;
            for (BlockDescriptor bd : blockPanels.keySet()) {
                containsBlock = rectangle.intersects(blockPanels.get(bd).getBounds());
                // selection
                if (!currentlySelectedBlocks.contains(bd)) {
                    if (containsBlock) {
                        currentlySelectedBlocks.add(bd);
                        // selectedBlockUI(blockPanels.get(bd));
                        blockPanels.get(bd).setSelected(true);
                    }
                }
                // deselection
                else if (!containsBlock) {
                    currentlySelectedBlocks.remove(bd);
                    if (!workFlow.isBlockSelected(bd))
                        blockPanels.get(bd).setSelected(false);
                }
            }

            selectLinks();
        }

        private void selectLinks() {
            boolean containsLink;
            for (Link<?> l : linkLines.keySet()) {
                containsLink = (currentlySelectedBlocks.contains(l.srcBlock) || workFlow.isBlockSelected(l.srcBlock))
                        && (currentlySelectedBlocks.contains(l.dstBlock) || workFlow.isBlockSelected(l.dstBlock));

                // selection
                if (!currentlySelectedLinks.contains(l)) {
                    if (containsLink) {
                        currentlySelectedLinks.add(l);
                        linkLines.get(l).setSelected(true);
                    }
                }
                // deselection
                else if (!containsLink) {
                    currentlySelectedLinks.remove(l);
                    linkLines.get(l).setSelected(false);
                    workFlow.unselectLink(l);
                }
            }
        }

        private void validateSelection() {
            for (BlockDescriptor bd : currentlySelectedBlocks)
                workFlow.selectBlock(bd);

            for (Link<?> l : currentlySelectedLinks)
                workFlow.selectLink(l);

            graphicSelectionHint = new Rectangle();
        }
    }

    private final WorkFlow workFlow;

    private final HashMap<BlockDescriptor, BlockPanel> blockPanels = new HashMap<>();

    private final HashMap<Link<?>, Line> linkLines = new HashMap<>();

    private final boolean editable;

    private final BlockSelector blockSelector = new BlockSelector();

    private final JPopupMenu popupMenu;

    /**
     * The {@link BlockPanel} containing this container (this field is null for top-level work
     * flows)
     */
    private WorkFlowPanel parentPanel = null;

    private Rectangle graphicSelectionHint = new Rectangle();

    private final static int TRANSPARENCY = 25;
    public final static Color TRANSPARENT_BLUE = new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(),
            Color.BLUE.getBlue(), TRANSPARENCY);

    // displays visual hint while selecting blocks
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(TRANSPARENT_BLUE);

        g.drawRect(graphicSelectionHint.x, graphicSelectionHint.y, graphicSelectionHint.width,
                graphicSelectionHint.height);
    }

    public WorkFlowContainer(final WorkFlow workFlow, boolean editable) {
        new DropTarget(this, this);
        setTransferHandler(new MenuItemTransferHandler());

        this.editable = editable;

        popupMenu = new JPopupMenu("Insert a new block...");

        if (editable) {
            addMouseListener(blockSelector);
            addMouseMotionListener(blockSelector);
        }

        addKeyListener(this);

        setFocusable(true);
        requestFocus();
        // replace the work flow with the new one
        this.workFlow = workFlow;

        // add graphical components from the new work flow

        for (BlockDescriptor blockInfo : workFlow)
            blockAdded(workFlow, blockInfo);

        for (Link<?> link : workFlow.getLinksIterator())
            linkAdded(workFlow, link);

        // register the listener

        this.workFlow.addListener(this);
    }

    private <T> void addGraphicalLink(final Link<T> link) {
        ThreadUtil.invokeLater(() -> {
            // the blocks must be retrieved inside the invokeLater method
            // to ensure they are loaded and visible on screen
            BlockPanel srcBlockPanel = getBlockPanel(link.srcBlock);
            BlockPanel dstBlockPanel = getBlockPanel(link.dstBlock);

            if ((srcBlockPanel == null) || (dstBlockPanel == null)) {
                System.err.println("Warning: cannot recover link between " + link.srcBlock.getDefinedName()
                        + " and " + link.dstBlock.getDefinedName() + " !");
            }
            else {
                linkLines.put(link, new RoundedSquareLine(srcBlockPanel, dstBlockPanel, link));

                // refresh the target block
                if (dstBlockPanel instanceof WorkFlowPanel) {
                    WorkFlowContainer innerFlow = ((WorkFlowPanel) dstBlockPanel).innerFlowPane;

                    BlockDescriptor targetInnerlBlock = innerFlow.workFlow.getInputOwner(link.dstVar);

                    innerFlow.getBlockPanel(targetInnerlBlock).refreshNow();
                }
                else
                    dstBlockPanel.refreshNow();

                repaint();
            }
        }, true);
    }

    @Override
    public void blockAdded(WorkFlow targetWorkFlow, final BlockDescriptor blockDesc) {
        if (targetWorkFlow != this.workFlow)
            return;

        ThreadUtil.invokeNow(() -> {
            BlockPanel panel;

            if (blockDesc.isLoop()) {
                panel = new LoopPanel(WorkFlowContainer.this, blockDesc);
            }
            else if (blockDesc.isWorkFlow()) {
                panel = new WorkFlowPanel(WorkFlowContainer.this, blockDesc);
            }
            else {
                panel = new BlockPanel(WorkFlowContainer.this, blockDesc);
            }

            panel.drawPanel();

            blockPanels.put(blockDesc, panel);
            add(panel);

            // FIX: get exposed links correctly restored on copy/paste as setVisibility isn't called in that case (Stephane)
            // this is really an ugly fix but the whole stuff is a complete mess honestly :-/
            if (blockDesc.isWorkFlow()) {
                synchronized (blockDesc.inputVars) {
                    // update exposed links
                    for (Var<?> var : blockDesc.inputVars)
                        ((WorkFlowPanel) panel).updateExposedLink(var, blockDesc.inputVars.isVisible(var));
                }

                synchronized (blockDesc.outputVars) {
                    // update exposed links
                    for (Var<?> var : blockDesc.outputVars)
                        ((WorkFlowPanel) panel).updateExposedLink(var, blockDesc.outputVars.isVisible(var));
                }
            }

            // Layer 0 for blocks
            // Layer 1 for work flows
            setLayer(panel, blockDesc.isWorkFlow() ? 1 : 0, 0);

            updatePreferredSize();

            repaint();
        });
    }

    @Override
    public void blockCollapsed(WorkFlow source, BlockDescriptor block, boolean collapsed) {

    }

    @Override
    public void blockDimensionChanged(WorkFlow source, BlockDescriptor block, int newWidth, int newHeight) {
        if (source != this.workFlow)
            return;

        updatePreferredSize();
        repaint();
    }

    @Override
    public void blockLocationChanged(WorkFlow source, BlockDescriptor block, int newX, int newY) {
        if (source != this.workFlow)
            return;

        updatePreferredSize();
        repaint();
    }

    @Override
    public void blockRemoved(WorkFlow targetWorkFlow, BlockDescriptor blockInfo) {
        if (targetWorkFlow != this.workFlow)
            return;

        final BlockPanel panel = blockPanels.remove(blockInfo);

        targetWorkFlow.unselectBlock(blockInfo);
        for (Link<?> l : linkLines.keySet())
            if (l.dstBlock.equals(blockInfo) || l.srcBlock.equals(blockInfo))
                targetWorkFlow.unselectLink(l);

        if (panel == null)
            return;

        ThreadUtil.invokeLater(() -> {
            panel.dispose();
            remove(panel);

            updatePreferredSize();

            repaint();
        });
    }

    @Override
    public void blockStatusChanged(WorkFlow source, BlockDescriptor block, BlockStatus status) {

    }

    @Override
    public void blockVariableAdded(WorkFlow source, BlockDescriptor block, Var<?> variable) {

    }

    @Override
    public <T> void blockVariableChanged(WorkFlow source, BlockDescriptor block, Var<T> variable, T newValue) {

    }

    public void dispose() {
        workFlow.removeListener(this);
        removeMouseListener(blockSelector);
        removeMouseMotionListener(blockSelector);
        removeKeyListener(this);

        for (BlockPanel panel : blockPanels.values())
            panel.dispose();
        blockPanels.clear();

        for (Line line : linkLines.values())
            line.dispose();
        linkLines.clear();

    }

    /**
     * Creates a new work flow embedding the current one, depending on the specified type parameter.
     * This method does nothing if the work flow is empty.
     *
     * @param embedType the type of work flow to create
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @see Loop
     */
    public void embedWorkFlow(final Class<? extends WorkFlow> embedType)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (workFlow.size() == 0)
            return;

        try {
            MainFrame.copySelection(workFlow, true);
            doEmbedWorkflow(embedType);
        }
        catch (LinkCutException | NoSuchMethodException | InvocationTargetException e) {
            if (ConfirmDialog.confirm("Warning", e.getMessage(), ConfirmDialog.OK_CANCEL_OPTION)) {
                doEmbedWorkflow(embedType);
            }
            requestFocus();
        }
    }

    private void doEmbedWorkflow(Class<? extends WorkFlow> embedType) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        WorkFlow embed = embedType.getDeclaredConstructor().newInstance();

        if (!workFlow.getBlockSelection().isEmpty()) {
            for (Link<?> l : workFlow.getLinkSelection())
                workFlow.removeLink(l.dstVar);
            while (!workFlow.getBlockSelection().isEmpty()) {
                BlockDescriptor tmp = workFlow.getBlockSelection().get(0);
                if (tmp.getBlock() instanceof WorkFlow)
                    ((WorkFlow) tmp.getBlock()).newSelection();
                workFlow.removeBlock(tmp, true);
            }
        }
        else {
            ArrayList<Link<?>> tmp = new ArrayList<>();
            for (Link<?> l : workFlow.getLinksIterator())
                tmp.add(l); // avoid concurrent modification exception
            for (Link<?> l : tmp)
                workFlow.removeLink(l.dstVar);
            for (int i = 0; i < workFlow.size(); )
                workFlow.removeBlock(workFlow.getBlock(i), true);
        }

        MainFrame.pasteSelection(embed, true);

        workFlow.addBlock(embed.getBlockDescriptor());
    }

    /**
     * @return A rectangle representing the bounds of this container's contents
     */
    public Rectangle getContentsBoundingBox() {
        if (blockPanels.size() == 0)
            return new Rectangle(0, 0, getWidth(), getHeight());

        Rectangle bounds = null;

        for (BlockPanel panel : blockPanels.values()) {
            bounds = (bounds == null) ? new Rectangle(panel.getBounds()) : bounds.union(panel.getBounds());
        }

        // add an artificial border to ensure that links are within the bounds
        int border = 20;
        bounds.x -= border;
        bounds.y -= border;
        bounds.width += border * 2;
        bounds.height += border * 2;

        return bounds;
    }

    public BlockPanel getBlockPanel(BlockDescriptor blockInfo) {
        if (blockInfo == null)
            return null;

        if (blockInfo.getBlock() == workFlow)
            return getParentPanel();

        return blockPanels.get(blockInfo);
    }

    /**
     * Returns the block panel containing this container, or null if this container is top-level
     *
     * @return
     */
    WorkFlowPanel getParentPanel() {
        return this.parentPanel;
    }

    public WorkFlow getWorkFlow() {
        return workFlow;
    }

    public boolean isEditable() {
        return editable;
    }

    @Override
    public void linkAdded(WorkFlow targetWorkFlow, final Link<?> link) {
        if (targetWorkFlow != this.workFlow)
            return;

        addGraphicalLink(link);
    }

    @Override
    public void linkRemoved(WorkFlow targetWorkFlow, final Link<?> link) {
        if (targetWorkFlow != this.workFlow)
            return;

        ThreadUtil.invokeLater(() -> removeGraphicalLink(link));
    }

    @Override
    public void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (workFlow.size() == 0) {
            String s = "Right-click to insert a block...";
            g2.setColor(Color.gray);
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC));
            Rectangle2D r = g2.getFontMetrics().getStringBounds(s, g);
            g2.drawString(s, (float) (getWidth() / 2 - r.getWidth() / 2),
                    (float) (getHeight() / 2 + r.getHeight() / 2));
        }

        for (Line line : linkLines.values()) {
            line.update();
            line.paint(g2);
        }

        g2.dispose();

        super.paintChildren(g);
    }

    private void removeGraphicalLink(Link<?> link) {
        Line line = linkLines.remove(link);

        if (line == null)
            System.err.println("Warning: missing link, cannot be removed properly");
        else
            line.dispose();

        BlockPanel dstBlockPanel = getBlockPanel(link.dstBlock);

        if (dstBlockPanel != null) {
            // refresh the target block
            if (dstBlockPanel instanceof WorkFlowPanel) {
                WorkFlowContainer innerFlow = ((WorkFlowPanel) dstBlockPanel).innerFlowPane;

                BlockDescriptor targetInnerlBlock = innerFlow.workFlow.getInputOwner(link.dstVar);

                innerFlow.getBlockPanel(targetInnerlBlock).refreshNow();
            }
            else
                dstBlockPanel.refreshNow();
        }

        repaint();
    }

    public void selectAllBlocks() {
        blockSelector.selectBlocks(getBounds());
        blockSelector.validateSelection();
    }

    @Override
    public void repaint() {
        super.repaint();
        if (parentPanel != null)
            parentPanel.repaint();
    }

    void setParentPanel(WorkFlowPanel panel) {
        this.parentPanel = panel;
    }

    @Override
    public void statusChanged(WorkFlow source, String message) {

    }

    protected void updatePreferredSize() {
        // PROBLEM: scroll panes can't extend "live" in north and west directions
        // TRICK: when a component hits the north (left) border, move everything south (east)

        // 1) find top-left far-most components

        Rectangle r = new Rectangle();
        int minx = 0, miny = 0;
        for (Component component : getComponents()) {
            component.getBounds(r);
            if (r.x < minx)
                minx = r.x;
            if (r.y < miny)
                miny = r.y;
        }

        // 2) compute the new preferred size of this container

        Dimension dim = new Dimension();
        for (Component component : getComponents()) {
            component.getBounds(r);

            // if the far-most location is negative, shift all components by that amount
            if (minx < 0)
                r.x -= minx;
            if (miny < 0)
                r.y -= miny;

            if (!r.getLocation().equals(component.getLocation()))
                component.setLocation(r.getLocation());

            dim.width = Math.max(Math.max(dim.width, dim.width - r.x), r.x + r.width);
            dim.height = Math.max(dim.height, r.y + r.height);
        }

        // 3) notify the scroll pane

        setPreferredSize(dim);

        revalidate();
    }

    @Override
    public void workFlowReordered(WorkFlow source) {
        // refresh all blocks
        for (BlockPanel panel : blockPanels.values())
            panel.refresh();
    }

    @Override
    public void keyPressed(KeyEvent key) {
        if (key.getKeyCode() == KeyEvent.VK_DELETE || key.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            ArrayList<BlockDescriptor> tmp = new ArrayList<>(workFlow.getBlockSelection());
            for (BlockDescriptor bd : tmp)
                workFlow.removeBlock(bd, true);
        }
        else {
            Protocols.dispatchEvent(key);
        }
    }

    @Override
    public void keyTyped(KeyEvent key) {
    }

    @Override
    public void keyReleased(KeyEvent key) {
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {

    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        try {
            Transferable t = dtde.getTransferable();
            DataFlavor[] d = t.getTransferDataFlavors();
            Point mouseLocation = dtde.getLocation();

            MenuItemTransferHandler menuItemTransferHandler = (MenuItemTransferHandler) getTransferHandler();

            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

            // quick & easy case first
            if (t.getTransferData(d[0]) instanceof DND_MenuItem) {
                // block coming from the search panel
                DND_MenuItem menuItem = (DND_MenuItem) t.getTransferData(d[0]);
                if (menuItemTransferHandler.canImport(this, d))
                    menuItemTransferHandler.importData(this, menuItem, mouseLocation);
            }
            else if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                // a list of files

                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

                ArrayList<String> xmlFiles = new ArrayList<>();
                ArrayList<String> otherFiles = new ArrayList<>();

                for (File file : files) {
                    String path = file.getPath();

                    if (FileUtil.getFileExtension(path, false).equalsIgnoreCase("xml")
                            || FileUtil.getFileExtension(path, false).equalsIgnoreCase("protocol")) {
                        xmlFiles.add(path);
                    }
                    else {
                        otherFiles.add(path);
                    }
                }

                for (String path : xmlFiles) {
                    Protocols.loadWorkFlow(new File(path));
                }
                for (String path : otherFiles) {
                    SequenceFileImporter importer = Loader.getSequenceFileImporter(path, true);
                    if (importer != null)
                        Loader.load(path, true);
                }
            }
        }
        catch (UnsupportedFlavorException | IOException e) {
            e.printStackTrace();
        }
        finally {
            dtde.dropComplete(true);
        }
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {

    }

    private class MenuItemTransferHandler extends TransferHandler {

        public boolean canImport(JComponent c, DataFlavor[] f) {
            DataFlavor tmp;
            try {
                tmp = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList");
                for (DataFlavor d : f)
                    if (d.equals(tmp))
                        return true;
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return false;
        }

        public void importData(WorkFlowContainer workFlowContainer, Transferable t, Point p) {
            Block block;
            Class<? extends Block> blockClass;
            try {
                DND_MenuItem item = (DND_MenuItem) t.getTransferData(new DataFlavor(DND_MenuItem.class, "MenuItem"));
                blockClass = item.getDescriptor().getPluginClass().asSubclass(Block.class);
                block = blockClass.getDeclaredConstructor().newInstance();
                BlockDescriptor blockDesc = (block instanceof WorkFlow ? ((WorkFlow) block).getBlockDescriptor()
                        : new BlockDescriptor(-1, block));
                blockDesc.setLocation(p.x, p.y);
                workFlowContainer.getWorkFlow().addBlock(blockDesc);
                requestFocus();
            }
            catch (
                    UnsupportedFlavorException
                    | IOException
                    | InstantiationException
                    | IllegalAccessException
                    | NoSuchMethodException
                    | InvocationTargetException e
            ) {
                e.printStackTrace();
            }
        }
    }
}
