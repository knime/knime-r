package org.knime.r;

import java.io.File;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.ext.r.bin.RPathUtil;
import org.knime.r.preferences.RPreferenceInitializer;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin implements IPropertyChangeListener {

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
	@Override
    public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		getPreferenceStore().addPropertyChangeListener(this);
		R_HOME = new File(getPreferenceStore().getString(RPreferenceInitializer.PREF_R_HOME));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(final BundleContext context) throws Exception {
		getPreferenceStore().removePropertyChangeListener(this);
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
	 * @return default R_HOME either package bundle or system default
	 */
    public static File getDefaultRHOME() {
        File packagedExecutable = RPathUtil.getPackagedRHome();
        if (packagedExecutable != null) {
            return packagedExecutable;
        }
        return RPathUtil.getSystemRHome();
    }
    
    private static File R_HOME = getDefaultRHOME();
    
    /**
     * @return R executable
     */
    public static File getRHOME() {
    	return R_HOME;
    }
    
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
    	if (RPreferenceInitializer.PREF_R_HOME.equals(event.getProperty())) {
    		R_HOME = new File(event.getNewValue().toString());
    	}
    }
}
