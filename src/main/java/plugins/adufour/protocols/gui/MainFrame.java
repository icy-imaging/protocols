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

package plugins.adufour.protocols.gui;

import fr.icy.Icy;
import fr.icy.common.listener.AcceptListener;
import fr.icy.extension.ExtensionLoader;
import fr.icy.extension.plugin.PluginDescriptor;
import fr.icy.gui.component.button.IcyButton;
import fr.icy.gui.component.button.IcyToggleButton;
import fr.icy.gui.component.icon.IcyIconPack;
import fr.icy.gui.component.icon.SVGResource;
import fr.icy.gui.component.tabbedpane.CloseableTabbedPane;
import fr.icy.gui.frame.IcyFrame;
import fr.icy.gui.frame.IcyFrameEvent;
import fr.icy.gui.frame.IcyFrameListener;
import fr.icy.gui.frame.progress.AnnounceFrame;
import fr.icy.io.Loader;
import fr.icy.io.xml.XMLUtil;
import fr.icy.system.FileDrop;
import fr.icy.system.IcyExceptionHandler;
import fr.icy.system.IcyHandledException;
import fr.icy.system.logging.IcyLogger;
import fr.icy.system.thread.ThreadUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.lang.BlockDescriptor;
import plugins.adufour.blocks.lang.BlockDescriptor.BlockStatus;
import plugins.adufour.blocks.lang.Link;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.util.*;
import plugins.adufour.protocols.Protocols;
import plugins.adufour.protocols.gui.block.WorkFlowContainer;
import plugins.adufour.vars.lang.Var;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class MainFrame extends IcyFrame implements IcyFrameListener, ActionListener, CloseableTabbedPane.CloseableTabbedPaneListener, AcceptListener, ChangeListener, BlockListener, FileDrop.FileDropListener {
    private static final String UNTITLED = "untitled";

    private final JToolBar toolBar;

    private final IcyButton bNew = new IcyButton("New", SVGResource.NOTE_ADD);
    private final IcyButton bLoad = new IcyButton("Load", SVGResource.FOLDER_OPEN);
    private final IcyButton bSave = new IcyButton("Save", SVGResource.SAVE);
    private final IcyButton bSaveAs = new IcyButton("Save as", SVGResource.SAVE_AS);
    //private final IcyButton bEmbed = new IcyButton("Embed", SVGIcon.OPEN_IN_NEW);
    private final IcyToggleButton bRun = new IcyToggleButton("Run", new IcyIconPack(SVGResource.PLAY_CIRCLE, SVGResource.STOP_CIRCLE));
    private final IcyButton bClean = new IcyButton("Reset", SVGResource.DEVICE_RESET);

    private final CloseableTabbedPane closeableTabbedPane = new CloseableTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

    private final ArrayList<ProtocolPanel> protocolPanels = new ArrayList<>();

    // Copy-paste support
    private static final HashMap<BlockDescriptor, BlockDescriptor> blocksClipBoard = new HashMap<>();
    private static final HashMap<BlockDescriptor, BlockDescriptor> blocksClipBoardBackup = new HashMap<>();
    private static final ArrayList<Link<?>> linksClipBoard = new ArrayList<>();
    private static final ArrayList<Link<?>> linksClipBoardBackup = new ArrayList<>();

    private static final String LINKS_CUT_MSG = "Some links will not be copied. Continue?";

    private final Protocols pluginInstance;

    private boolean isFrameClosing = false;

    public MainFrame(final Protocols pluginInstance) {
        //super("Protocols editor (" + pluginInstance.getFriendlyVersion() + ")", true, true, true, true, true);
        super("Protocols editor", true, true, true, true, true);

        this.pluginInstance = pluginInstance;

        // handle close event locally
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addFrameListener(this);

        setPreferredSize(new Dimension(800, 640));

        // Menus

        toolBar = new JToolBar("Blocks toolbar", SwingConstants.HORIZONTAL);

        getContentPane().add(toolBar, BorderLayout.NORTH);

        for (final IcyButton button : new IcyButton[]{bNew, bLoad, bSave, bSaveAs, /*bEmbed,*/ bClean})
            button.addActionListener(this);

        for (final IcyToggleButton button : new IcyToggleButton[]{bRun})
            button.addActionListener(this);

        bNew.setToolTipText("Create a new empty protocol");
        bLoad.setToolTipText("Load a protocol from a file");
        bSave.setToolTipText("Save the current protocol");
        bSaveAs.setToolTipText("Save the protocol to a new file");
        /*bEmbed.setToolTipText(
                "Embed the current selection into\r\n" +
                        "a new work flow or a batch"
        );*/
        bRun.setToolTipText("Run the current protocol");
        bClean.setToolTipText(
                "Clear all intermediate results\r\n" +
                        "(may help saving memory)"
        );

        toolBar.add(bNew);
        toolBar.add(bLoad);
        toolBar.add(bSave);
        toolBar.add(bSaveAs);
        //toolBar.add(new JToolBar.Separator());
        //toolBar.add(bEmbed);
        toolBar.add(new JToolBar.Separator());
        toolBar.add(bRun);
        toolBar.add(bClean);

        closeableTabbedPane.addCloseableTabbedPaneListener(this);

        final BlockSearchPanel blockSearchPanel = new BlockSearchPanel();
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, blockSearchPanel, closeableTabbedPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        splitPane.setUI(new MySplitPaneUI());
        getContentPane().add(splitPane, BorderLayout.CENTER);

        getContentPane().addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent key) {
                if ((key.getModifiersEx() & Protocols.MENU_SHORTCUT_KEY) == Protocols.MENU_SHORTCUT_KEY
                        && key.getComponent() instanceof WorkFlowContainer) {
                    final WorkFlow wf = ((WorkFlowContainer) key.getComponent()).getWorkFlow();

                    switch (key.getKeyCode()) {
                        case KeyEvent.VK_C:
                            copySelection(wf, false);
                            break;
                        case KeyEvent.VK_V:
                            pasteSelection(wf, false);
                            break;
                        case KeyEvent.VK_X:
                            copySelection(wf, false);
                            deleteSelection(wf);
                            break;
                        case KeyEvent.VK_BACK_SPACE:
                        case KeyEvent.VK_DELETE:
                            deleteSelection(wf);
                    }

                    key.getComponent().requestFocus(); // enable successive operations
                }
            }

            @Override
            public void keyTyped(final KeyEvent arg0) {
            }

            @Override
            public void keyReleased(final KeyEvent arg0) {
            }
        });

        final JToolBar statusBar = new JToolBar(JToolBar.HORIZONTAL);
        statusBar.setFloatable(false);
        statusBar.add(new JLabel("Engine v" + Protocols.VERSION.toShortString()));

        getContentPane().add(statusBar, BorderLayout.SOUTH);

        Icy.getMainInterface().addCanExitListener(this);

        pack();
    }

    public static void copySelection(final WorkFlow wf, final boolean embedding) throws LinkCutException {
        // do not modify clip-board when embedding/dis-embedding
        if (embedding) {
            // blocksClipBoardBackup = new HashMap<BlockDescriptor,
            // BlockDescriptor>(blocksClipBoard);
            // linksClipBoardBackup = new ArrayList<Link<?>>(linksClipBoard);
            blocksClipBoardBackup.clear();
            blocksClipBoardBackup.putAll(blocksClipBoard);
            linksClipBoardBackup.clear();
            linksClipBoardBackup.addAll(linksClipBoard);
        }

        blocksClipBoard.clear();
        linksClipBoard.clear();

        // copy the whole workflow
        if (wf.getBlockSelection().isEmpty()) {
            for (final BlockDescriptor bd : wf)
                blocksClipBoard.put(bd, bd.clone(embedding));
            for (final Link<?> l : wf.getLinksIterator())
                linksClipBoard.add(l);
        }
        // copy the selection
        else {
            for (final BlockDescriptor bd : wf.getBlockSelection())
                blocksClipBoard.put(bd, bd.clone(embedding));
            linksClipBoard.addAll(wf.getLinkSelection());
        }

        if (embedding && linkCut(wf.getLinksIterator()))
            throw new LinkCutException(LINKS_CUT_MSG);
    }

    @SuppressWarnings({"unchecked"})
    public static void pasteSelection(final WorkFlow wf, final boolean embedding) {
        for (final BlockDescriptor bd : blocksClipBoard.keySet()) {
            // avoid getting the selection back in the container
            if (bd.getBlock() instanceof WorkFlow)
                ((WorkFlow) blocksClipBoard.get(bd).getBlock()).newSelection();

            wf.addBlock(blocksClipBoard.get(bd));
        }

        for (final Link<?> l : linksClipBoard) {
            @SuppressWarnings("rawtypes")
            final Link clone = linkClone(wf, l);

            if (clone == null) {
                IcyLogger.warn(MainFrame.class, "Cannot copy a link to an exposed variable.");
                continue;
            }

            wf.addLink(clone.srcBlock, clone.srcVar, clone.dstBlock, clone.dstVar);
        }

        // re-copy the first copy
        // avoid pasting an altered state of the block
        blocksClipBoard.replaceAll((b, v) -> blocksClipBoard.get(b).clone(embedding));

        if (embedding) {
            // restore the backup
            // blocksClipBoard = new HashMap<BlockDescriptor,
            // BlockDescriptor>(blocksClipBoardBackup);
            // linksClipBoard = new ArrayList<Link<?>>(linksClipBoardBackup);
            blocksClipBoard.clear();
            blocksClipBoard.putAll(blocksClipBoardBackup);
            linksClipBoard.clear();
            linksClipBoard.addAll(linksClipBoardBackup);
        }
    }

    private static void deleteSelection(final @NotNull WorkFlow wf) {
        if (wf.getBlockSelection().isEmpty()) {
            final ArrayList<Link<?>> tmp = new ArrayList<>();
            for (final Link<?> l : wf.getLinksIterator())
                tmp.add(l); // avoid concurrent modification exception
            for (final Link<?> l : tmp)
                wf.removeLink(l.dstVar);

            for (final int i = 0; i < wf.size(); )
                wf.removeBlock(wf.getBlock(i), false);
        }
        else {
            for (final Link<?> l : wf.getLinkSelection())
                wf.removeLink(l.dstVar);

            // avoid infinite loop when canceling
            final ArrayList<BlockDescriptor> tmp = new ArrayList<>(wf.getBlockSelection());
            for (final BlockDescriptor bd : tmp)
                wf.removeBlock(bd, false);
        }
    }

    private static boolean linkCut(final @NotNull Iterable<Link<?>> links) {
        for (final Link<?> l : links)
            if (blocksClipBoard.containsKey(l.dstBlock) ^ blocksClipBoard.containsKey(l.srcBlock))
                return true;
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static @Nullable Link<?> linkClone(final WorkFlow wf, final @NotNull Link<?> l) {
        Var<?> newSrc, newDst;

        try {
            // get link input from block inputs
            newSrc = blocksClipBoard.get(l.srcBlock).inputVars.get(l.srcBlock.inputVars.getID(l.srcVar));
            newDst = blocksClipBoard.get(l.dstBlock).inputVars.get(l.dstBlock.inputVars.getID(l.dstVar));
        }
        catch (final NoSuchVariableException nsve) {
            // get link input from block outputs
            newSrc = blocksClipBoard.get(l.srcBlock).outputVars.get(l.srcBlock.outputVars.getID(l.srcVar));
            newDst = blocksClipBoard.get(l.dstBlock).inputVars.get(l.dstBlock.inputVars.getID(l.dstVar));
        }

        // if the link contains an exposed variable of a work flow, newSrc or newDst == null
        // (because the ID of the cloned variable is different in the cloned work flow)
        if (newSrc == null || newDst == null) {
            return null;
        }

        return new Link(wf, blocksClipBoard.get(l.srcBlock), newSrc, blocksClipBoard.get(l.dstBlock), newDst);
    }

    public void addProtocolPane(final ProtocolPanel protocol) {
        getProtocolPanels().add(protocol);
        protocol.addPropertyChangeListener(ProtocolPanel.WORKFLOW_MODIFIED, this);
        protocol.addBlockListener(this);

        new FileDrop(protocol, false, this);

        if (protocol.getFile() != null) {
            final File f = protocol.getFile();
            protocol.setName(f.getName());
            closeableTabbedPane.addTab(f.getName(), protocol);
            closeableTabbedPane.setToolTipTextAt(getProtocolPanels().size() - 1, f.getAbsolutePath());
        }
        else {
            protocol.setName(UNTITLED);
            closeableTabbedPane.addTab(UNTITLED, protocol);
        }

        closeableTabbedPane.setSelectedIndex(getProtocolPanels().size() - 1);
    }

    public ProtocolPanel getActiveProtocol() {
        final int index = closeableTabbedPane.getSelectedIndex();
        return (index == -1) ? null : getProtocolPanels().get(index);
    }

    @Override
    public void actionPerformed(final @NotNull ActionEvent e) {
        if (e.getSource() == bNew) {
            addProtocolPane(new ProtocolPanel(this));
        }
        else if (e.getSource() == bLoad) {
            loadWorkFlow(null);
        }
        else if (e.getSource() == bSave) {
            saveWorkFlow(getActiveProtocol(), true);
        }
        else if (e.getSource() == bSaveAs) {
            final ProtocolPanel protocol = getActiveProtocol();

            if (protocol == null)
                return;

            // save the old file to restore it in case a problem occurs
            final File oldFile = protocol.getFile();

            // erase the file to force an open file dialog
            protocol.setFile(null);

            if (!saveWorkFlow(protocol, true)) {
                // work flow was not saved => restore the old file
                protocol.setFile(oldFile);
            }
        }
        else if (e.getSource() == bRun) {
            final ProtocolPanel protocol = getActiveProtocol();

            if (protocol == null)
                return;

            final WorkFlow wf = protocol.getWorkFlow();
            if (wf.getBlockDescriptor().getStatus() == BlockStatus.RUNNING) {
                wf.interrupt();
            }
            else {
                wf.runWorkFlow();
            }
        }
        /*else if (e.getSource() == bEmbed) {
            final ProtocolPanel panel = getActiveProtocol();
            if (panel != null)
                panel.showBlocksEmbedMenu(bEmbed);
        }*/
        else if (e.getSource() == bClean) {
            final ProtocolPanel protocol = getActiveProtocol();

            if (protocol == null)
                return;

            final WorkFlow wf = protocol.getWorkFlow();
            wf.reset();
        }
    }

    @Override
    public void propertyChange(final @NotNull PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ProtocolPanel.WORKFLOW_MODIFIED) && !isFrameClosing) {
            final ProtocolPanel protocolPanel = (ProtocolPanel) evt.getSource();
            final int index = getProtocolPanels().indexOf(protocolPanel);

            if (index == -1) {
                String message = "in method \"icy.adufour.protocols.Protocols.propertyChange(PropertyChangeEvent)\":\n";
                message += "A protocol was modified, but its panel is already closed (index = -1)";
                IcyExceptionHandler.handleException(pluginInstance.getDescriptor(),
                        new ArrayIndexOutOfBoundsException(message), true);
                return;
            }

            if (closeableTabbedPane.getTabCount() <= index)
                return;

            if ((Boolean) evt.getNewValue()) {
                closeableTabbedPane.setTitleAt(index, "* " + closeableTabbedPane.getTitleAt(index));
            }
            else if (protocolPanel.getFile() != null) {
                closeableTabbedPane.setTitleAt(index, protocolPanel.getFile().getName());
            }
        }
        else {
            super.propertyChange(evt);
        }
    }

    @Override
    public void tabClosed(final int index, final String title) {
    }

    @Override
    public boolean tabClosing(final int index, final String title) {
        final ProtocolPanel protocol = getProtocolPanels().get(index);
        if (protocol.isDirty() && !protocol.isEmpty() && !Protocols.isReloading()) {
            final String name = protocol.getFile() == null ? UNTITLED : protocol.getFile().getName();
            final int option = JOptionPane.showConfirmDialog(protocol,
                    name + " has not been saved.\nSave workflow before closing ?", "Confirmation",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (option == JOptionPane.CANCEL_OPTION)
                return false;

            if (option == JOptionPane.YES_OPTION && !saveWorkFlow(protocol, false))
                return false;
        }
        protocol.removePropertyChangeListener(this);
        protocol.removeBlockListener(this);
        protocol.dispose();

        // Remove drag n drop hooks
        FileDrop.remove(protocol);
        getProtocolPanels().remove(protocol);

        return true;
    }

    /**
     * @return true if the saving operation was successful, false if canceled
     * @throws BlocksException if an error occurred while saving to disk
     */
    private boolean saveWorkFlow(final ProtocolPanel protocol, final boolean updateTabName) throws BlocksException {
        if (protocol == null)
            return false;

        final boolean success = protocol.saveToDisk();

        if (!success)
            return false;

        final File savedFile = protocol.getFile();

        if (updateTabName)
            closeableTabbedPane.setTitleAt(closeableTabbedPane.getSelectedIndex(), savedFile.getName());

        return true;
    }

    private @Nullable File getFile() {
        final JFileChooser jfc = new JFileChooser(Protocols.getDefaultProtocolFolder());

        jfc.setFileFilter(BlocksML.XML_FILE_FILTER);
        jfc.setDialogTitle("Load an Icy protocol...");

        if (jfc.showOpenDialog(getFrame()) != JFileChooser.APPROVE_OPTION)
            return null;

        return jfc.getSelectedFile();
    }

    /**
     * @return the protocolPanels
     */
    public ArrayList<ProtocolPanel> getProtocolPanels() {
        return protocolPanels;
    }

    /**
     * Loads the specified file as a new workflow
     *
     * @throws IllegalArgumentException if the file is not a protocol
     */
    public void loadWorkFlow(File file) throws IllegalArgumentException {
        if (file == null)
            file = getFile();

        if (file == null)
            return;

        final File workingFile = file;

        Protocols.setDefaultProtocolFolder(file.getParent());

        // check that the work flow is not already opened
        for (final ProtocolPanel protocol : getProtocolPanels())
            if (file.equals(protocol.getFile()))
                return;

        // make sure the xml is well-formed
        final Document xml = XMLUtil.loadDocument(file);

        boolean isFileValid = false;

        if (xml != null) {
            final String rootName = XMLUtil.getRootElement(xml).getNodeName();
            isFileValid = rootName.equalsIgnoreCase("protocol") || rootName.equalsIgnoreCase("workspace");
        }

        if (!isFileValid)
            throw new IcyHandledException("Error while loading \"" + file.getPath() + "\":\nThe selected file is not an Icy protocol.");

        final ProtocolPanel panel = new ProtocolPanel(this);
        panel.setFile(file);
        addProtocolPane(panel);

        ThreadUtil.bgRun(() -> {
            final AnnounceFrame announce = new AnnounceFrame("Loading protocol " + workingFile.getName() + "...");

            try {
                //PluginInstaller.waitInstall();
                ExtensionLoader.waitWhileLoading();

                final Set<PluginDescriptor> plugins = ExtensionLoader.getPlugins(Block.class, false, false);

                if (plugins.isEmpty())
                    throw new BlocksReloadedException();

                panel.loadWorkFlow(xml, false);
            }
            catch (final BlocksReloadedException e) {
                pluginInstance.reload(xml, workingFile.getAbsolutePath());
            }
            catch (final BlocksException e) {
                ThreadUtil.invokeLater(() -> {
                    String message = e.getMessage() + ".\n\n";
                    message += "A bug was caught while loading the protocol.\n";
                    message += " - Yes: send the report and the protocol (recommended for diagnosis).\n";
                    message += " - No: send the report but not the protocol (recommended for privacy).\n";
                    message += " - Cancel: discard this message (nothing will be sent)";
                    final int answer = JOptionPane.showConfirmDialog(getFrame(), message, "Couldn't load protocol", JOptionPane.YES_NO_CANCEL_OPTION);

                    final StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));

                    message += "\n\n" + sw;

                    switch (answer) {
                        case JOptionPane.YES_OPTION:

                            try {
                                message += "\n\n" + BlocksML.getInstance().toString(xml);
                            }
                            catch (final TransformerFactoryConfigurationError | TransformerException e2) {
                                IcyLogger.error(MainFrame.class, e2, e2.getLocalizedMessage());
                            }

                            //$FALL-THROUGH$ Send the report anyway
                        case JOptionPane.NO_OPTION:
                            IcyExceptionHandler.report(pluginInstance.getDescriptor(), message);
                            break;
                        default:
                            break;
                    }
                });
            }
            finally {
                announce.close();
            }
        });
    }

    @Override
    public void icyFrameOpened(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameClosing(final @NotNull IcyFrameEvent e) {
        if (e.getFrame() == this) {

            isFrameClosing = accept(this);
        }
    }

    @Override
    public void icyFrameClosed(final IcyFrameEvent e) {
        Protocols.setReloading(false);
    }

    @Override
    public void icyFrameIconified(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameDeiconified(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameActivated(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameDeactivated(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameInternalized(final IcyFrameEvent e) {
    }

    @Override
    public void icyFrameExternalized(final IcyFrameEvent e) {
    }

    @Override
    public void filesDropped(final File @NotNull [] files) {
        for (final File file : files) {
            try {
                loadWorkFlow(file);
            }
            catch (final IllegalArgumentException e) {
                if (Loader.isSupportedImageFile(file.getPath()))
                    Loader.load(file.getPath(), true);
            }
        }
    }

    @Override
    public boolean accept(final Object source) {
        while (closeableTabbedPane.getTabCount() > 0) {
            final int tabCount = closeableTabbedPane.getTabCount();

            for (int i = 0; i < tabCount; i++) {
                if (closeableTabbedPane.getSelectedIndex() == i) {
                    final boolean closeAccepted = tabClosing(i, null);

                    // if "cancel" is clicked for either tab, stop the closing process
                    if (!closeAccepted)
                        return false;

                    if (closeableTabbedPane.getTabCount() > i)
                        closeableTabbedPane.removeTabAt(i);

                    break;
                }
            }
        }

        for (final Component component : toolBar.getComponents()) {
            if (component instanceof JButton)
                ((JButton) component).removeActionListener(this);

            toolBar.remove(component);
        }

        getContentPane().removeAll();

        removeFrameListener(this);

        // close manually

        setVisible(false);

        close();

        Icy.getMainInterface().removeCanExitListener(this);

        Protocols.closeProtocols();

        return true;
    }

    @Override
    public void stateChanged(final @NotNull ChangeEvent e) {
        final ProtocolPanel panel = getActiveProtocol();

        if (e.getSource() == closeableTabbedPane && panel != null) {
            // tab was selected, added or removed
            // => update the status of the run button
            updateRunButton(getActiveProtocol().getWorkFlow().getBlockDescriptor().getStatus());
        }
    }

    private void updateRunButton(final @NotNull BlockStatus status) {
        bRun.setSelected(status == BlockStatus.RUNNING);
    }

    @Override
    public void blockCollapsed(final BlockDescriptor block, final boolean collapsed) {

    }

    @Override
    public void blockDimensionChanged(final BlockDescriptor block, final int newWidth, final int newHeight) {
    }

    @Override
    public void blockLocationChanged(final BlockDescriptor block, final int newX, final int newY) {
    }

    @Override
    public void blockStatusChanged(final BlockDescriptor block, final BlockStatus status) {
        updateRunButton(status);
    }

    @Override
    public void blockVariableAdded(final BlockDescriptor block, final Var<?> variable) {
    }

    @Override
    public <T> void blockVariableChanged(final BlockDescriptor block, final Var<T> variable, final T newValue) {
    }

    private static class MySplitPaneUI extends BasicSplitPaneUI {
        private static class MySplitPaneDivider extends BasicSplitPaneDivider {

            public MySplitPaneDivider(final BasicSplitPaneUI ui) {
                super(ui);
            }

            @Override
            protected @NotNull JButton createLeftOneTouchButton() {
                final JButton button = super.createLeftOneTouchButton();
                button.setPreferredSize(new Dimension(20, 40));
                return button;
            }

            @Override
            protected @NotNull JButton createRightOneTouchButton() {
                final JButton button = super.createRightOneTouchButton();
                button.setPreferredSize(new Dimension(20, 40));
                return button;
            }
        }

        @Contract(" -> new")
        @Override
        public @NotNull BasicSplitPaneDivider createDefaultDivider() {
            return new MySplitPaneDivider(this);
        }
    }
}
