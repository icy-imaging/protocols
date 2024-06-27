/*
 * Copyright (c) 2010-2024. Institut Pasteur.
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

import org.bioimageanalysis.icy.gui.dialog.ConfirmDialog;
import org.bioimageanalysis.icy.io.FileUtil;
import org.bioimageanalysis.icy.io.Loader;
import org.bioimageanalysis.icy.io.SequenceFileImporter;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.bioimageanalysis.icy.system.thread.ThreadUtil;
import plugins.adufour.blocks.lang.*;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.util.BlocksFinder;
import plugins.adufour.blocks.util.BlocksFinder.DND_MenuItem;
import plugins.adufour.blocks.util.LinkCutException;
import plugins.adufour.blocks.util.WorkFlowListener;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.protocols.gui.MainFrame;
import plugins.adufour.protocols.gui.link.Line;
import plugins.adufour.protocols.gui.link.RoundedSquareLine;
import plugins.adufour.vars.lang.Var;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
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

public class WorkFlowContainer extends JLayeredPane implements WorkFlowListener, DropTargetListener, KeyListener {
    protected class BlockSelector extends MouseAdapter {
        int pressedX, pressedY;
        ArrayList<Link<?>> currentlySelectedLinks = new ArrayList<>();
        ArrayList<BlockDescriptor> currentlySelectedBlocks = new ArrayList<>();

        @Override
        public void mousePressed(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (!((e.getModifiersEx() & Protocols.MENU_SHORTCUT_KEY) == Protocols.MENU_SHORTCUT_KEY)) {
                    workFlow.newSelection();
                    for (final BlockPanel bp : blockPanels.values())
                        bp.setSelected(false);
                    for (final Line l : linkLines.values())
                        l.setSelected(false);
                }

                if (e.getComponent() instanceof BlockPanel) {
                    final BlockDescriptor bd = ((BlockPanel) e.getComponent()).blockDesc;

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
        public void mouseDragged(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                final Rectangle selection = new Rectangle(Math.min(e.getX(), pressedX), Math.min(e.getY(), pressedY),
                        Math.abs(pressedX - e.getX()), Math.abs(pressedY - e.getY()));

                selectBlocks(selection);

                graphicSelectionHint = selection;

                revalidate();
                repaint();
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                validateSelection();

                requestFocus();
                revalidate();
                repaint();
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON2 || e.isMetaDown()) {
                popupMenu.removeAll();
                new BlocksFinder().createJMenu(popupMenu, WorkFlowContainer.this, e.getPoint());
                popupMenu.show(WorkFlowContainer.this, e.getX(), e.getY());
            }
            else {
                for (final Link<?> link : linkLines.keySet())
                    if (linkLines.get(link).isOverCloseButton(e.getPoint())) {
                        workFlow.removeLink(link.dstVar);
                        // prevent concurrent modification
                        break;
                    }
            }
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            for (final Line l : linkLines.values())
                l.setCustomColor(l.contains(e.getX(), e.getY()));
        }

        private void selectBlocks(final Rectangle rectangle) {
            // block selection
            boolean containsBlock;
            for (final BlockDescriptor bd : blockPanels.keySet()) {
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
            for (final Link<?> l : linkLines.keySet()) {
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
            for (final BlockDescriptor bd : currentlySelectedBlocks)
                workFlow.selectBlock(bd);

            for (final Link<?> l : currentlySelectedLinks)
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
    public final static Color TRANSPARENT_BLUE = new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(), Color.BLUE.getBlue(), TRANSPARENCY);

    // displays a visual hint while selecting blocks
    @Override
    public void paint(final Graphics g) {
        super.paint(g);
        g.setColor(TRANSPARENT_BLUE);

        g.drawRect(graphicSelectionHint.x, graphicSelectionHint.y, graphicSelectionHint.width,
                graphicSelectionHint.height);
    }

    public WorkFlowContainer(final WorkFlow workFlow, final boolean editable) {
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

        for (final BlockDescriptor blockInfo : workFlow)
            blockAdded(workFlow, blockInfo);

        for (final Link<?> link : workFlow.getLinksIterator())
            linkAdded(workFlow, link);

        // register the listener

        this.workFlow.addListener(this);
    }

    private <T> void addGraphicalLink(final Link<T> link) {
        ThreadUtil.invokeLater(() -> {
            // the blocks must be retrieved inside the invokeLater method
            // to ensure they are loaded and visible on screen
            final BlockPanel srcBlockPanel = getBlockPanel(link.srcBlock);
            final BlockPanel dstBlockPanel = getBlockPanel(link.dstBlock);

            if ((srcBlockPanel == null) || (dstBlockPanel == null)) {
                IcyLogger.warn(WorkFlowContainer.class, "Cannot recover link between " + link.srcBlock.getDefinedName() + " and " + link.dstBlock.getDefinedName() + " !");
            }
            else {
                linkLines.put(link, new RoundedSquareLine(srcBlockPanel, dstBlockPanel, link));

                // refresh the target block
                if (dstBlockPanel instanceof WorkFlowPanel) {
                    final WorkFlowContainer innerFlow = ((WorkFlowPanel) dstBlockPanel).innerFlowPane;

                    final BlockDescriptor targetInnerlBlock = innerFlow.workFlow.getInputOwner(link.dstVar);

                    innerFlow.getBlockPanel(targetInnerlBlock).refreshNow();
                }
                else
                    dstBlockPanel.refreshNow();

                repaint();
            }
        }, true);
    }

    @Override
    public void blockAdded(final WorkFlow targetWorkFlow, final BlockDescriptor blockDesc) {
        if (targetWorkFlow != this.workFlow)
            return;

        ThreadUtil.invokeNow(() -> {
            final BlockPanel panel;

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
            // this is really an ugly fix, but the whole stuff is a complete mess honestly :-/
            if (blockDesc.isWorkFlow()) {
                synchronized (blockDesc.inputVars) {
                    // update exposed links
                    for (final Var<?> var : blockDesc.inputVars)
                        ((WorkFlowPanel) panel).updateExposedLink(var, blockDesc.inputVars.isVisible(var));
                }

                synchronized (blockDesc.outputVars) {
                    // update exposed links
                    for (final Var<?> var : blockDesc.outputVars)
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
    public void blockCollapsed(final WorkFlow source, final BlockDescriptor block, final boolean collapsed) {

    }

    @Override
    public void blockDimensionChanged(final WorkFlow source, final BlockDescriptor block, final int newWidth, final int newHeight) {
        if (source != this.workFlow)
            return;

        updatePreferredSize();
        repaint();
    }

    @Override
    public void blockLocationChanged(final WorkFlow source, final BlockDescriptor block, final int newX, final int newY) {
        if (source != this.workFlow)
            return;

        updatePreferredSize();
        repaint();
    }

    @Override
    public void blockRemoved(final WorkFlow targetWorkFlow, final BlockDescriptor blockInfo) {
        if (targetWorkFlow != this.workFlow)
            return;

        final BlockPanel panel = blockPanels.remove(blockInfo);

        targetWorkFlow.unselectBlock(blockInfo);
        for (final Link<?> l : linkLines.keySet())
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
    public void blockStatusChanged(final WorkFlow source, final BlockDescriptor block, final BlockStatus status) {

    }

    @Override
    public void blockVariableAdded(final WorkFlow source, final BlockDescriptor block, final Var<?> variable) {

    }

    @Override
    public <T> void blockVariableChanged(final WorkFlow source, final BlockDescriptor block, final Var<T> variable, final T newValue) {

    }

    public void dispose() {
        workFlow.removeListener(this);
        removeMouseListener(blockSelector);
        removeMouseMotionListener(blockSelector);
        removeKeyListener(this);

        for (final BlockPanel panel : blockPanels.values())
            panel.dispose();
        blockPanels.clear();

        for (final Line line : linkLines.values())
            line.dispose();
        linkLines.clear();

    }

    /**
     * Creates a new work flow embedding the current one, depending on the specified type parameter.
     * This method does nothing if the work flow is empty.
     *
     * @param embedType the type of work flow to create
     * @see Loop
     */
    public void embedWorkFlow(final Class<? extends WorkFlow> embedType) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (workFlow.size() == 0)
            return;

        try {
            MainFrame.copySelection(workFlow, true);
            doEmbedWorkflow(embedType);
        }
        catch (final LinkCutException | NoSuchMethodException | InvocationTargetException e) {
            if (ConfirmDialog.confirm("Warning", e.getMessage(), ConfirmDialog.OK_CANCEL_OPTION)) {
                doEmbedWorkflow(embedType);
            }
            requestFocus();
        }
    }

    private void doEmbedWorkflow(final Class<? extends WorkFlow> embedType) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final WorkFlow embed = embedType.getDeclaredConstructor().newInstance();

        if (!workFlow.getBlockSelection().isEmpty()) {
            for (final Link<?> l : workFlow.getLinkSelection())
                workFlow.removeLink(l.dstVar);
            while (!workFlow.getBlockSelection().isEmpty()) {
                final BlockDescriptor tmp = workFlow.getBlockSelection().getFirst();
                if (tmp.getBlock() instanceof WorkFlow)
                    ((WorkFlow) tmp.getBlock()).newSelection();
                workFlow.removeBlock(tmp, true);
            }
        }
        else {
            final ArrayList<Link<?>> tmp = new ArrayList<>();
            for (final Link<?> l : workFlow.getLinksIterator())
                tmp.add(l); // avoid concurrent modification exception
            for (final Link<?> l : tmp)
                workFlow.removeLink(l.dstVar);
            for (int i = 0; i < workFlow.size(); i++) // TODO: 09/06/2023 i++ was not present, check if it was for purpose
                workFlow.removeBlock(workFlow.getBlock(i), true);
        }

        MainFrame.pasteSelection(embed, true);

        workFlow.addBlock(embed.getBlockDescriptor());
    }

    /**
     * @return A rectangle representing the bounds of this container's contents
     */
    public Rectangle getContentsBoundingBox() {
        if (blockPanels.isEmpty())
            return new Rectangle(0, 0, getWidth(), getHeight());

        Rectangle bounds = null;

        for (final BlockPanel panel : blockPanels.values()) {
            bounds = (bounds == null) ? new Rectangle(panel.getBounds()) : bounds.union(panel.getBounds());
        }

        // add an artificial border to ensure that links are within the bounds
        final int border = 20;
        bounds.x -= border;
        bounds.y -= border;
        bounds.width += border * 2;
        bounds.height += border * 2;

        return bounds;
    }

    public BlockPanel getBlockPanel(final BlockDescriptor blockInfo) {
        if (blockInfo == null)
            return null;

        if (blockInfo.getBlock() == workFlow)
            return getParentPanel();

        return blockPanels.get(blockInfo);
    }

    /**
     * Returns the block panel containing this container, or null if this container is top-level
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
    public void linkAdded(final WorkFlow targetWorkFlow, final Link<?> link) {
        if (targetWorkFlow != this.workFlow)
            return;

        addGraphicalLink(link);
    }

    @Override
    public void linkRemoved(final WorkFlow targetWorkFlow, final Link<?> link) {
        if (targetWorkFlow != this.workFlow)
            return;

        ThreadUtil.invokeLater(() -> removeGraphicalLink(link));
    }

    @Override
    public void paintChildren(final Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (workFlow.size() == 0) {
            final String s = "Right-click to insert a block...";
            g2.setColor(Color.gray);
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC));
            final Rectangle2D r = g2.getFontMetrics().getStringBounds(s, g);
            g2.drawString(s, (float) (getWidth() / 2 - r.getWidth() / 2), (float) (getHeight() / 2 + r.getHeight() / 2));
        }

        for (final Line line : linkLines.values()) {
            line.update();
            line.paint(g2);
        }

        g2.dispose();

        super.paintChildren(g);
    }

    private void removeGraphicalLink(final Link<?> link) {
        final Line line = linkLines.remove(link);

        if (line == null)
            IcyLogger.warn(WorkFlowContainer.class, "Missing link, cannot be removed properly.");
        else
            line.dispose();

        final BlockPanel dstBlockPanel = getBlockPanel(link.dstBlock);

        if (dstBlockPanel != null) {
            // refresh the target block
            if (dstBlockPanel instanceof WorkFlowPanel) {
                final WorkFlowContainer innerFlow = ((WorkFlowPanel) dstBlockPanel).innerFlowPane;

                final BlockDescriptor targetInnerlBlock = innerFlow.workFlow.getInputOwner(link.dstVar);

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

    void setParentPanel(final WorkFlowPanel panel) {
        this.parentPanel = panel;
    }

    @Override
    public void statusChanged(final WorkFlow source, final String message) {

    }

    protected void updatePreferredSize() {
        // PROBLEM: scroll panes can't extend "live" in north and west directions
        // TRICK: when a component hits the north (left) border, move everything south (east)

        // 1) find top-left far-most components

        final Rectangle r = new Rectangle();
        int minx = 0, miny = 0;
        for (final Component component : getComponents()) {
            component.getBounds(r);
            if (r.x < minx)
                minx = r.x;
            if (r.y < miny)
                miny = r.y;
        }

        // 2) compute the new preferred size of this container

        final Dimension dim = new Dimension();
        for (final Component component : getComponents()) {
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
    public void workFlowReordered(final WorkFlow source) {
        // refresh all blocks
        for (final BlockPanel panel : blockPanels.values())
            panel.refresh();
    }

    @Override
    public void keyPressed(final KeyEvent key) {
        if (key.getKeyCode() == KeyEvent.VK_DELETE || key.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            final ArrayList<BlockDescriptor> tmp = new ArrayList<>(workFlow.getBlockSelection());
            for (final BlockDescriptor bd : tmp)
                workFlow.removeBlock(bd, true);
        }
        else {
            Protocols.dispatchEvent(key);
        }
    }

    @Override
    public void keyTyped(final KeyEvent key) {
    }

    @Override
    public void keyReleased(final KeyEvent key) {
    }

    @Override
    public void dragEnter(final DropTargetDragEvent dtde) {

    }

    @Override
    public void dragExit(final DropTargetEvent dte) {

    }

    @Override
    public void dragOver(final DropTargetDragEvent dtde) {

    }

    @Override
    public void drop(final DropTargetDropEvent dtde) {
        try {
            final Transferable t = dtde.getTransferable();
            final DataFlavor[] d = t.getTransferDataFlavors();
            final Point mouseLocation = dtde.getLocation();

            final MenuItemTransferHandler menuItemTransferHandler = (MenuItemTransferHandler) getTransferHandler();

            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

            // quick & easy case first
            if (t.getTransferData(d[0]) instanceof final DND_MenuItem menuItem) {
                // block coming from the search panel
                if (menuItemTransferHandler.canImport(this, d))
                    menuItemTransferHandler.importData(this, menuItem, mouseLocation);
            }
            else if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                // a list of files

                @SuppressWarnings("unchecked") final List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

                final ArrayList<String> xmlFiles = new ArrayList<>();
                final ArrayList<String> otherFiles = new ArrayList<>();

                for (final File file : files) {
                    final String path = file.getPath();

                    if (FileUtil.getFileExtension(path, false).equalsIgnoreCase("xml")
                            || FileUtil.getFileExtension(path, false).equalsIgnoreCase("protocol")) {
                        xmlFiles.add(path);
                    }
                    else {
                        otherFiles.add(path);
                    }
                }

                for (final String path : xmlFiles) {
                    Protocols.loadWorkFlow(new File(path));
                }
                for (final String path : otherFiles) {
                    final SequenceFileImporter importer = Loader.getSequenceFileImporter(path, true);
                    if (importer != null)
                        Loader.load(path, true);
                }
            }
        }
        catch (final UnsupportedFlavorException | IOException e) {
            IcyLogger.error(WorkFlowContainer.class, e, e.getLocalizedMessage());
        }
        finally {
            dtde.dropComplete(true);
        }
    }

    @Override
    public void dropActionChanged(final DropTargetDragEvent dtde) {

    }

    private class MenuItemTransferHandler extends TransferHandler {

        @Override
        public boolean canImport(final JComponent c, final DataFlavor[] f) {
            final DataFlavor tmp;
            try {
                tmp = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.util.ArrayList");
                for (final DataFlavor d : f)
                    if (d.equals(tmp))
                        return true;
            }
            catch (final ClassNotFoundException e) {
                IcyLogger.error(WorkFlowContainer.class, e, e.getLocalizedMessage());
            }
            return false;
        }

        public void importData(final WorkFlowContainer workFlowContainer, final Transferable t, final Point p) {
            final Block block;
            final Class<? extends Block> blockClass;
            try {
                final DND_MenuItem item = (DND_MenuItem) t.getTransferData(new DataFlavor(DND_MenuItem.class, "MenuItem"));
                blockClass = item.getDescriptor().getPluginClass().asSubclass(Block.class);
                block = blockClass.getDeclaredConstructor().newInstance();
                final BlockDescriptor blockDesc = (block instanceof WorkFlow ? ((WorkFlow) block).getBlockDescriptor() : new BlockDescriptor(-1, block));
                blockDesc.setLocation(p.x, p.y);
                workFlowContainer.getWorkFlow().addBlock(blockDesc);
                requestFocus();
            }
            catch (
                    final UnsupportedFlavorException
                          | IOException
                          | InstantiationException
                          | IllegalAccessException
                          | NoSuchMethodException
                          | InvocationTargetException e
            ) {
                IcyLogger.error(WorkFlowContainer.class, e, e.getLocalizedMessage());
            }
        }
    }
}
