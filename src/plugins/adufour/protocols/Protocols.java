package plugins.adufour.protocols;

import icy.file.FileUtil;
import icy.main.Icy;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.PluginActionable;
import icy.preferences.PluginsPreferences;
import icy.preferences.XMLPreferences;
import icy.system.thread.ThreadUtil;
import icy.util.XMLUtil;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import plugins.adufour.blocks.lang.WorkFlow;
import plugins.adufour.blocks.util.BlocksML;
import plugins.adufour.blocks.util.BlocksReloadedException;
import plugins.adufour.protocols.gui.MainFrame;
import plugins.adufour.protocols.gui.ProtocolPanel;

/**
 * Protocols plugin
 */
public class Protocols extends PluginActionable
{
    // "command" on mac, "ctrl" on others
    public static final int MENU_SHORTCUT_KEY;

    static
    {
        MENU_SHORTCUT_KEY = GraphicsEnvironment.isHeadless() ? 0 : Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    }

    private static final String PREF_FOLDER = "protocolFolder";

    private static MainFrame mainFrame;

    private static boolean reloading = false;

    public static final String downloadedProtocolFolder = FileUtil.getApplicationDirectory() + File.separator
            + "protocols";

    private static volatile XMLPreferences preferences = PluginsPreferences.getPreferences()
            .node(Protocols.class.getName());

    public static String getDefaultProtocolFolder()
    {
        return preferences.get(PREF_FOLDER, System.getProperty("user.home"));
    }

    public static void setDefaultProtocolFolder(String path)
    {
        preferences.put(PREF_FOLDER, path);
    }

    // public static MainFrame getLastActiveFrame()
    // {
    // return openedFrames.peek();
    // }

    /**
     * @return <i>Protocols</i> main instance and <code>null</code> if Protocols is not started
     */
    public static MainFrame getInstance()
    {
        return mainFrame;
    }

    public static boolean isReloading()
    {
        return reloading;
    }

    public static void setReloading(boolean value)
    {
        reloading = value;
    }

    /**
     * Saves the current state of the Protocols interface and restarts it.<br/>
     * This method is useful when plug-ins have been modified (via the plug-in loader) and requires
     * Protocols to restart to take into account the new changes
     * 
     * @param reloadingNode
     *        the node that caused the reload operation (should be reloaded last)
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     */
    public void reload(Document reloadingXML, String reloadingPath)
    {
        // 0) avoid silly situations...
        if (mainFrame == null)
            return;

        reloading = true;

        // 1) save the current state with all opened protocols into the preferences

        preferences.putInt("Window X", mainFrame.getX());
        preferences.putInt("Window Y", mainFrame.getY());

        int counter = 1;

        // save protocols to the preferences file one by one
        for (ProtocolPanel protocol : mainFrame.getProtocolPanels())
        {
            try
            {
                File attachedFile = protocol.getFile();
                String xmlProtocol;

                if (attachedFile != null && attachedFile.getAbsolutePath().equals(reloadingPath))
                {
                    xmlProtocol = BlocksML.getInstance().toString(reloadingXML);
                }
                else
                {
                    xmlProtocol = BlocksML.getInstance().toString(protocol.getWorkFlow());
                }

                XMLPreferences node = preferences.node("Protocol #" + counter++);
                node.putBoolean("dirty", protocol.isDirty());
                node.put("xml", xmlProtocol);
                if (attachedFile != null)
                    node.put("fileName", attachedFile.getAbsolutePath());
            }
            catch (TransformerException e)
            {
                System.err.println("Warning: couldn't store protocol " + protocol.getName() + ":");
                e.printStackTrace();
                System.err.println("---");
            }
        }

        // 2) close the current Protocols instance

        close();

        ThreadUtil.invokeLater(new Runnable()
        {
            public void run()
            {
                // 3) launch a new instance of the Protocols plug-in
                try
                {
                    ((PluginActionable) PluginLoader.getPluginClass(Protocols.class.getName()).newInstance()).run();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    private final static LinkedHashMap<String, String> commandLineArguments = new LinkedHashMap<String, String>();

    public static Map<String, String> getCommandLineArguments()
    {
        return commandLineArguments;
    }

    @Override
    public void run()
    {
        if (Icy.getMainInterface().isHeadLess())
        {
            // Command line perhaps?
            runHeadless();
            return;
        }

        if (mainFrame != null)
        {
            mainFrame.requestFocus();
            mainFrame.toFront();
            return;
        }

        mainFrame = new MainFrame(this);

        DocumentBuilder builder;
        try
        {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            // Reload potential temporary protocols from the preferences

            int x = preferences.getInt("Window X", mainFrame.getX());
            int y = preferences.getInt("Window Y", mainFrame.getY());
            mainFrame.setLocation(x, y);

            ArrayList<XMLPreferences> protocols = preferences.getChildren();

            if (protocols.size() == 0)
            {
                mainFrame.addProtocolPane(new ProtocolPanel(mainFrame));
            }
            else
                for (XMLPreferences node : preferences.getChildren())
                {
                    ProtocolPanel panel = new ProtocolPanel(mainFrame);
                    String fileName = node.get("fileName", null);
                    if (fileName != null)
                        panel.setFile(new File(fileName));
                    mainFrame.addProtocolPane(panel);

                    Document xml = builder.parse(new InputSource(new StringReader(node.get("xml", null))));

                    try
                    {
                        panel.loadWorkFlow(xml, node.getBoolean("dirty", false));

                        // if the protocol loads correctly, remove it from the preferences
                        preferences.remove(node.name());
                    }
                    catch (BlocksReloadedException e)
                    {
                        reload(xml, panel.getFile().getAbsolutePath());
                        return;
                    }
                }

        }
        catch (ParserConfigurationException e)
        {
            e.printStackTrace();
        }
        catch (SAXException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        mainFrame.addToDesktopPane();
        mainFrame.setVisible(true);
    }

    private static void runHeadless()
    {
        String[] clargs = Icy.getCommandLinePluginArgs();

        String protocolFile = null;

        for (String clarg : clargs)
        {
            // Sanity check
            if (!clarg.contains("="))
            {
                throw new IllegalArgumentException("Invalid command line argument: " + clarg);
            }

            String[] keyValuePair = clarg.split("=");
            if (keyValuePair.length != 2)
            {
                throw new IllegalArgumentException("Invalid command line argument: " + clarg);
            }

            String key = keyValuePair[0];
            String value = keyValuePair[1];

            if (key.isEmpty())
            {
                throw new IllegalArgumentException("Invalid command line argument (no key): " + clarg);
            }

            if (value.isEmpty())
            {
                throw new IllegalArgumentException("Invalid command line argument (no value): " + clarg);
            }

            if (key.equalsIgnoreCase("protocol"))
            {
                protocolFile = value;
            }
            else
            {
                commandLineArguments.put(key, value);
            }
        }

        Document xml = XMLUtil.loadDocument(protocolFile);

        // Discard invalid files
        if (xml == null)
        {
            throw new IllegalArgumentException(protocolFile + " is not a valid protocol file");
        }

        System.out.println("Loading workflow...");
        WorkFlow workFlow = new WorkFlow();
        BlocksML.getInstance().loadWorkFlow(xml, workFlow);

        workFlow.run();
    }

    public static void loadWorkFlow(File file)
    {
        if (mainFrame == null)
            new Protocols().run();

        mainFrame.loadWorkFlow(file);
    }

    public static void close()
    {
        if (mainFrame != null && mainFrame.isVisible())
            mainFrame.close();
        mainFrame = null;
    }

    public static void dispatchEvent(KeyEvent key)
    {
        if (mainFrame != null)
            mainFrame.getContentPane().dispatchEvent(key);
    }
}
