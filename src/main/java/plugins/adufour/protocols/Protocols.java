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

package plugins.adufour.protocols;

import fr.icy.Icy;
import fr.icy.common.Version;
import fr.icy.extension.ExtensionLoader;
import fr.icy.extension.plugin.abstract_.PluginActionable;
import fr.icy.extension.plugin.annotation_.IcyPluginName;
import fr.icy.io.FileUtil;
import fr.icy.io.xml.XMLUtil;
import fr.icy.system.preferences.PluginsPreferences;
import fr.icy.system.preferences.XMLPreferences;
import fr.icy.system.thread.ThreadUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.util.BlocksException;
import plugins.adufour.blocks.util.BlocksML;
import plugins.adufour.blocks.util.BlocksReloadedException;
import plugins.adufour.protocols.gui.MainFrame;
import plugins.adufour.protocols.gui.ProtocolPanel;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Protocols plugin
 */
@IcyPluginName("Protocols")
public class Protocols extends PluginActionable {
    private static final Logger LOGGER = Logger.getLogger(Protocols.class.getName());

    public static final Version VERSION = new Version(4, 0, 0, Version.DevelopmentStage.ALPHA, 8);

    // "command" on Mac, "ctrl" on others
    public static final int MENU_SHORTCUT_KEY;

    static {
        MENU_SHORTCUT_KEY = GraphicsEnvironment.isHeadless() ? 0 : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    private static final String PREF_FOLDER = "protocolFolder";

    private static MainFrame mainFrame;

    private static boolean reloading = false;

    public static final String downloadedProtocolFolder = FileUtil.getApplicationDirectory() + File.separator + "protocols";

    private static final XMLPreferences preferences = PluginsPreferences.getPreferences().node(Protocols.class.getName());

    public static String getDefaultProtocolFolder() {
        return preferences.get(PREF_FOLDER, System.getProperty("user.home"));
    }

    public static void setDefaultProtocolFolder(final String path) {
        preferences.put(PREF_FOLDER, path);
    }

    // public static MainFrame getLastActiveFrame()
    // {
    // return openedFrames.peek();
    // }

    /**
     * @return <i>Protocols</i> main instance and <code>null</code> if Protocols is not started
     */
    public static MainFrame getInstance() {
        return mainFrame;
    }

    public static boolean isReloading() {
        return reloading;
    }

    public static void setReloading(final boolean value) {
        reloading = value;
    }

    /**
     * Saves the current state of the Protocols interface and restarts it.
     * <p>
     * This method is useful when plug-ins have been modified (via the plug-in
     * loader) and requires Protocols to restart to take into account the new
     * changes
     */
    public void reload(final Document reloadingXML, final String reloadingPath) {
        // 0) avoid silly situations...
        if (mainFrame == null) return;

        reloading = true;

        // 1) save the current state with all opened protocols into the preferences

        preferences.putInt("Window X", mainFrame.getX());
        preferences.putInt("Window Y", mainFrame.getY());

        int counter = 1;

        // save protocols to the preferences file one by one
        for (final ProtocolPanel protocol : mainFrame.getProtocolPanels()) {
            try {
                final File attachedFile = protocol.getFile();
                final String xmlProtocol;

                if (attachedFile != null && attachedFile.getAbsolutePath().equals(reloadingPath)) {
                    xmlProtocol = BlocksML.getInstance().toString(reloadingXML);
                }
                else {
                    xmlProtocol = BlocksML.getInstance().toString(protocol.getWorkFlow());
                }

                final XMLPreferences node = preferences.node("Protocol #" + counter++);
                node.putBoolean("dirty", protocol.isDirty());
                node.put("xml", xmlProtocol);
                if (attachedFile != null) node.put("fileName", attachedFile.getAbsolutePath());
            }
            catch (final TransformerException e) {
                if (LOGGER.isLoggable(Level.WARNING))
                    LOGGER.log(Level.WARNING, "Couldn't store protocol " + protocol.getName() + ".", e);
            }
        }

        // 2) close the current Protocols instance

        closeProtocols();

        ThreadUtil.invokeLater(() -> {
            // 3) launch a new instance of the Protocols plug-in
            try {
                ((PluginActionable) ExtensionLoader.getPlugin(Protocols.class.getName()).getPluginClass().getDeclaredConstructor().newInstance()).run();
            }
            catch (final Exception e) {
                if (LOGGER.isLoggable(Level.SEVERE))
                    LOGGER.log(Level.SEVERE, "Unable to launch Protocols plug-in.", e);
            }
        });
    }

    private final static LinkedHashMap<String, String> commandLineArguments = new LinkedHashMap<>();

    public static Map<String, String> getCommandLineArguments() {
        return commandLineArguments;
    }

    @Override
    public void run() {
        if (Icy.getMainInterface().isHeadLess()) {
            // Command-line, perhaps?
            runHeadless();
            return;
        }

        if (mainFrame != null) {
            mainFrame.requestFocus();
            mainFrame.toFront();
            return;
        }

        mainFrame = new MainFrame(this);

        try {
            final int x = preferences.getInt("Window X", mainFrame.getX());
            final int y = preferences.getInt("Window Y", mainFrame.getY());
            mainFrame.setLocation(x, y);

            final String protocolPath = parseCommandLineArgs(false);

            // we have a protocol filename specified?
            if (protocolPath != null) {
                final ProtocolPanel panel = new ProtocolPanel(mainFrame);
                final Document xml = XMLUtil.loadDocument(protocolPath);

                // Discard invalid files
                if (xml != null) panel.setFile(new File(protocolPath));

                mainFrame.addProtocolPane(panel);

                if (xml != null) {
                    try {
                        panel.loadWorkFlow(xml, true);
                    }
                    catch (final BlocksReloadedException e) {
                        reload(xml, panel.getFile().getAbsolutePath());
                        return;
                    }
                    catch (final BlocksException e2) {
                        if (LOGGER.isLoggable(Level.SEVERE))
                            LOGGER.log(Level.SEVERE, "Unable to load protocol " + panel.getFile().getAbsolutePath(), e2);
                    }
                }
            }
            else {
                // Reload potential temporary protocols from the preferences
                final ArrayList<XMLPreferences> protocols = preferences.getChildren();

                // no protocol info in XML preference
                if (protocols.isEmpty()) mainFrame.addProtocolPane(new ProtocolPanel(mainFrame));
                else {
                    final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

                    for (final XMLPreferences node : preferences.getChildren()) {
                        final ProtocolPanel panel = new ProtocolPanel(mainFrame);
                        final String fileName = node.get("fileName", null);
                        if (fileName != null) panel.setFile(new File(fileName));
                        mainFrame.addProtocolPane(panel);

                        final Document xml = builder.parse(new InputSource(new StringReader(node.get("xml", null))));

                        try {
                            panel.loadWorkFlow(xml, node.getBoolean("dirty", false));

                            // if the protocol loads correctly, remove it from the preferences
                            preferences.remove(node.name());
                        }
                        catch (final BlocksReloadedException e) {
                            reload(xml, panel.getFile().getAbsolutePath());
                            return;
                        }
                        catch (final BlocksException e2) {
                            if (LOGGER.isLoggable(Level.SEVERE))
                                LOGGER.log(Level.SEVERE, "Unable to load protocol " + panel.getFile().getAbsolutePath(), e2);
                        }
                    }
                }
            }
        }
        catch (final ParserConfigurationException | SAXException | IOException e) {
            if (LOGGER.isLoggable(Level.SEVERE))
                LOGGER.log(Level.SEVERE, "Unable to load protocols.", e);
        }

        mainFrame.addToDesktopPane();
        mainFrame.setVisible(true);
    }

    /**
     * Build the command line arguments map and return protocol path if any is specified
     */
    private static String parseCommandLineArgs(final boolean verify) {
        final String[] clargs = Icy.getCommandLinePluginArgs();
        String result = null;
        boolean ok = false;

        for (final String clarg : clargs) {
            // checking sanity
            if (!clarg.contains("=")) {
                if (verify) throw new IllegalArgumentException("Invalid command line argument: " + clarg);

                // next arg
                continue;
            }

            final String[] keyValuePair = clarg.split("=");
            if (keyValuePair.length != 2) {
                if (verify) throw new IllegalArgumentException("Invalid command line argument: " + clarg);

                // next arg
                continue;
            }

            final String key = keyValuePair[0];
            final String value = keyValuePair[1];

            if (key.isEmpty()) {
                if (verify) throw new IllegalArgumentException("Invalid command line argument (no key): " + clarg);

                // next arg
                continue;
            }

            if (value.isEmpty()) {
                if (verify) throw new IllegalArgumentException("Invalid command line argument (no value): " + clarg);

                // next arg
                continue;
            }

            if (key.equalsIgnoreCase("protocol")) {
                result = value;
                ok = true;
            }
            else commandLineArguments.put(key, value);
        }

        // we correctly parsed arguments? --> clear it so no other plugin can reparse them
        if (ok) Icy.clearCommandLinePluginArgs();

        return result;
    }

    private static void runHeadless() {
        final String protocolFile = parseCommandLineArgs(true);

        final Document xml = XMLUtil.loadDocument(protocolFile);

        // Discard invalid files
        if (xml == null) throw new IllegalArgumentException(protocolFile + " is not a valid protocol file");

        if (LOGGER.isLoggable(Level.INFO))
        LOGGER.log(Level.INFO, "Loading workflow…");
        final WorkFlow workFlow = new WorkFlow(true);
        BlocksML.getInstance().loadWorkFlow(xml, workFlow);

        workFlow.run();
    }

    @SuppressWarnings("resource")
    public static void loadWorkFlow(final File file) {
        if (mainFrame == null) new Protocols().run();

        mainFrame.loadWorkFlow(file);
    }

    public static void closeProtocols() {
        if (mainFrame != null && mainFrame.isVisible()) mainFrame.close();
        mainFrame = null;
    }

    public static void dispatchEvent(final KeyEvent key) {
        if (mainFrame != null) mainFrame.getContentPane().dispatchEvent(key);
    }
}
