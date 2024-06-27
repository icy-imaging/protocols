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

package plugins.adufour.protocols.livebar;

import org.bioimageanalysis.icy.extension.plugin.PluginLoader;
import org.bioimageanalysis.icy.io.FileUtil;
import org.bioimageanalysis.icy.io.xml.XMLPersistent;
import org.bioimageanalysis.icy.io.xml.XMLUtil;
import org.bioimageanalysis.icy.network.NetworkUtil;
import org.bioimageanalysis.icy.system.logging.IcyLogger;
import org.w3c.dom.Node;
import plugins.adufour.protocols.Protocols;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ProtocolDescriptor implements XMLPersistent {
    private String filePath;

    private String fileDownloadURL;

    private String url;

    private String name;

    private String description;

    private String author;

    public ProtocolDescriptor(final Node node) {
        loadFromXML(node);
    }

    public String getName() {
        return name;
    }

    public ImageIcon getIcon() {
        return PluginLoader.getPlugin(Protocols.class.getName()).getIcon();
    }

    public String getWeb() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    public Image getImage() {
        return null;
    }

    public File getFile() {
        try {
            final FileOutputStream stream = new FileOutputStream(filePath);
            stream.write(NetworkUtil.download(fileDownloadURL, null, false));
            stream.flush();
            stream.close();

            return new File(filePath);
        }
        catch (final IOException e) {
            IcyLogger.error(ProtocolDescriptor.class, e, e.getLocalizedMessage());
            return null;
        }
    }

    @Override
    public boolean loadFromXML(final Node node) {
        this.name = XMLUtil.getValue(XMLUtil.getElement(node, "name"), "No name");

        this.author = XMLUtil.getValue(XMLUtil.getElement(node, "author"), "No author");

        this.url = XMLUtil.getValue(XMLUtil.getElement(node, "url"), "https://icy.bioimageanalysis.org");

        this.description = XMLUtil.getValue(XMLUtil.getElement(node, "shortDescription"), "No description");

        // create the download folder if it doesn't exist yet
        FileUtil.createDir(Protocols.downloadedProtocolFolder);

        this.filePath = Protocols.downloadedProtocolFolder + File.separator + name + " v" + XMLUtil.getValue(XMLUtil.getElement(node, "version"), "0") + ".xml";

        this.fileDownloadURL = XMLUtil.getValue(XMLUtil.getElement(node, "fileurl"), "");

        return true;
    }

    @Override
    public boolean saveToXML(final Node node) {
        return false;
    }
}
