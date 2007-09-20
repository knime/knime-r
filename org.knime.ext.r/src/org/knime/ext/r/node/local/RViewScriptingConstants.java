/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   19.09.2007 (gabriel): created
 */
package org.knime.ext.r.node.local;

import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Provides a set of names of R plots as well as dummy code templates of these
 * plots.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * @author Kilian Thiel, University of Konstanz
 */
public final class RViewScriptingConstants {
    
    /**
     * List of view function along with their R command structure.
     */
    public static final HashMap<String, String> LABEL2COMMAND
        = new LinkedHashMap<String, String>();
    
    static {
        LABEL2COMMAND.put("Generic X-Y Plotting", "plot(x, y, ...)");
        LABEL2COMMAND.put("Box Plots", "boxplot(x, ...)");
        LABEL2COMMAND.put("Bar Plots", "barplot(height, ...)");
        LABEL2COMMAND.put("Cleveland Dot Plots", "dotchart(x, ...)");
        LABEL2COMMAND.put("Association Plots", "assocplot(x, ...)");
        LABEL2COMMAND.put("Conditional Density Plots", "cdplot(x, ...)");
        LABEL2COMMAND.put("Display Contours", "contour(x, ...)");
        LABEL2COMMAND.put("Conditioning Plots", "coplot(formula, data, ...)");
        LABEL2COMMAND.put("Draw Function Plots", "curve(expr, from, to, ...)");
        LABEL2COMMAND.put("Fourfold Plots", "fourfoldplot(x, ...)");
        LABEL2COMMAND.put("Histograms", "hist(x, ...)");
        LABEL2COMMAND.put("Plot Columns of Matrices", "matplot(x, y, ...)");
        LABEL2COMMAND.put("Mosaic Plots", "mosaicplot(x, ...)");
        LABEL2COMMAND.put("Scatterplot Matrices", "pairs(x, ...)");
        LABEL2COMMAND.put("Perspective Plots", "persp(x, ...)");
        LABEL2COMMAND.put("Pie Charts", "pie(x, ...)");
        LABEL2COMMAND.put("Polygon Drawing", "polygon(x, ...)");
        LABEL2COMMAND.put("Spine Plots and Spinograms", "spineplot(x, ...)");
        LABEL2COMMAND.put("Star (Spider/Radar) Plots and Segment Diagrams", 
                "stars(x, ...)");
        LABEL2COMMAND.put("Sunflower Scatter Plot", "sunflowerplot(x, ...)");
        LABEL2COMMAND.put("1-D Scatter Plots", "stripchart(x, ...)");
    }

    private RViewScriptingConstants() {
        
    }
    
}
