package org.knime.r;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
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
	
	private static File rHome;
	 
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
		
		
//		Bundle bundle = context.getBundle();
//        Enumeration<URL> e = bundle.findEntries("/R-Inst/bin", "R.exe", true);
//        URL url = null;
//        if ((e != null) && e.hasMoreElements()) {
//            url = e.nextElement();
//        } else {
//            e = bundle.findEntries("/R-Inst/bin", "R", true);
//            if ((e != null) && e.hasMoreElements()) {
//                url = e.nextElement();
//            }
//        }
		
		if (Platform.isWindows()) {
			File base = new File("C:\\Program Files\\R");
			if (base.exists() && base.isDirectory()) {
				File[] files = base.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.startsWith("R-2");
					}
				});
				if (files != null && files.length > 0) {
					rHome = files[0];
				}
			}
		} else {
	        // default path on linux systems and mac (no R binary plugin available)
			rHome = new File("/usr/lib/R");
		}
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
        return rHome;
    }
}
