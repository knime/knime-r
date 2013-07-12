package org.knime.r;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.ext.r.SystemPathUtil;
import org.knime.ext.r.bin.PackagedPathUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.sun.jna.Platform;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.knime.r"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
     * @return R executable
     */
    public static File getRHOME() {
        try {
            File packagedExecutable = PackagedPathUtil.getPackagedHome();
            if (packagedExecutable != null) {
                return packagedExecutable;
            }
        } catch (NoClassDefFoundError err) {
            // PackagedPathUtil may not exist if the optional plug-in is not installed
        }
        return SystemPathUtil.getSystemHome();
    }
}
