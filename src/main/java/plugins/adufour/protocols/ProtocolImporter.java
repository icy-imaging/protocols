/*
 * Copyright (c) 2010-2023. Institut Pasteur.
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

import icy.file.FileImporter;
import icy.gui.frame.progress.FileFrame;
import icy.plugin.abstract_.Plugin;
import icy.system.thread.ThreadUtil;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.List;

/**
 * Importer class that will automatically load a protocol file dropped inside Icy
 *
 * @author Alexandre Dufour
 */
public class ProtocolImporter extends Plugin implements FileImporter {
    @Override
    public boolean acceptFile(final String path) {
        return path.toLowerCase().endsWith(".protocol") && new File(path).isFile();
    }

    @Override
    public List<FileFilter> getFileFilters() {
        final FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File f) {
                return acceptFile(f.getPath());
            }

            @Override
            public String getDescription() {
                return "Icy protocols (.protocol)";
            }
        };

        return List.of(filter);
    }

    @Override
    public boolean load(final String path, final FileFrame loadingFrame) {
        try {
            return ThreadUtil.invokeNow(() -> {
                Protocols.loadWorkFlow(new File(path));

                return true;
            });

        }
        catch (final Exception e) {
            return false;
        }
    }
}