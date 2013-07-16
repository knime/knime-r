package org.knime.r;

import java.util.ArrayList;
import java.util.Collection;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;

public class RSnippetNodeConfig {
    /**
     * Get the input port definition
     * @return the input port definition
     */
	protected Collection<PortType> getInPortTypes() {
		Collection<PortType> portTypes = new ArrayList<PortType>(4);
		portTypes.add(BufferedDataTable.TYPE);
		return portTypes;
	}
	
    /**
     * Get the output port definition
     * @return the output port definition
     */
	protected Collection<PortType> getOutPortTypes() {
		Collection<PortType> portTypes = new ArrayList<PortType>(4);
		portTypes.add(BufferedDataTable.TYPE);
		return portTypes;
	}

	/**
	 * Text preceding to the r-script.
	 * @return the r-script prefix
	 */
	protected String getScriptPrefix() {
		return "";
	}  
	
	/**
	 * Text appended to the r-script.
	 * @return the r-script suffix
	 */
	protected String getScriptSuffix() {
		return "";
	}
	
	/**
	 * The default script for this node.
	 * @return the default script
	 */
	protected String getDefaultScript() {
		boolean inHasTable = false;
		for(PortType portType : getInPortTypes()) {
			if (portType.equals(BufferedDataTable.TYPE)) {
				inHasTable = true;
				break;
			}
		}
		boolean outHasTable = false;
		boolean outHasView = false;
		for(PortType portType : getOutPortTypes()) {
			if (portType.equals(BufferedDataTable.TYPE)) {
				outHasTable = true;
				break;
			} else if (portType.equals(ImagePortObject.TYPE)) {
			    outHasView = true;
			}
		}		
		// the source nodes
		if (getInPortTypes().size() <= 0) {
			if (outHasTable) {
				return "knime.out <- data.frame()";
			} else {
				return "R <- data.frame()";
			}
		} else {
		    if (inHasTable && outHasView) {
		        return "plot(knime.in)";
		    } else if (outHasView)  {
		        return "plot(iris)";
		    } else if (inHasTable && outHasTable) {
				return "knime.out <- knime.in";
			} else if (!inHasTable && outHasTable) {
				return "knime.out <- R";
			} else if (inHasTable && !outHasTable) {
				return "R <- knime.in";
			} else {
				return "R <- data.frame()";
			}
		}
	}	
}
