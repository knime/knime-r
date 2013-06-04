package org.knime.r;

import java.util.EventListener;

public interface RListener extends EventListener {
	public void workspaceChanged(REvent e);
}
