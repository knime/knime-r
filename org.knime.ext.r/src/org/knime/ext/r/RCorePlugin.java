/*
 * -------------------------------------------------------------------
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * This file is part of the R integration plugin for KNIME.
 *
 * The R integration plugin is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St., Fifth Floor, Boston, MA 02110-1301, USA.
 * Or contact us: contact@knime.org.
 * -------------------------------------------------------------------
 *
 */
package org.knime.ext.r;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeFactory;
import org.knime.ext.r.node.RScriptingNodeFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class RCorePlugin extends AbstractUIPlugin {

    // The shared instance.
    private static RCorePlugin plugin;

    private static File rExecutable;

    /**
     * The constructor.
     */
    public RCorePlugin() {
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation.
     * @param context The bundle context.
     * @throws Exception If cause by super class.
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        NodeFactory.addLoadedFactory(RScriptingNodeFactory.class);

        Bundle bundle = context.getBundle();
        Enumeration<URL> e = bundle.findEntries("/R-Inst/bin", "R.exe", true);
        URL url = null;
        if ((e != null) && e.hasMoreElements()) {
            url = e.nextElement();
        } else {
            e = bundle.findEntries("/R-Inst/bin", "R", true);
            if ((e != null) && e.hasMoreElements()) {
                url = e.nextElement();
            }
        }
        if (url != null) {
            try {
                rExecutable = new File(FileLocator.toFileURL(url).getFile());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * This method is called when the plug-in is stopped.
     * @param context The bundle context.
     * @throws Exception If cause by super class.
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return The shared instance
     */
    public static RCorePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path.
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(final String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin("org.knime.ext.r",
                path);
    }

    /**
     * @return R executable
     */
    public static File getRExecutable() {
        return rExecutable;
    }
}
