/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
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
        LABEL2COMMAND.put("Generic X-Y Plotting",
                "# data(iris)\n# R <- iris\n"
              + "# plot(R[1:2])\n"
              + "plot(R)");
        LABEL2COMMAND.put("Box Plots",
                "# data(iris)\n# R <- iris\n"
              + "# boxplot(R[1:4], range = 1.0, notch = TRUE)\n"
              + "boxplot(R)");
        LABEL2COMMAND.put("Bar Plots",
                "# data(iris)\n# R <- iris\n"
              + "# barplot(R$\"Sepal.Length\", space = 1.0, border=NA, "
              + "ylab=\"sepal length\")");
        LABEL2COMMAND.put("Cleveland Dot Plots",
                "# data(iris)\n# R <- iris\n"
              + "# dotchart(R$\"Sepal.Length\", pch='*', "
              + "xlab=\"sepal length\")");
        LABEL2COMMAND.put("Association Plots",
                "# data(iris)\n# R <- iris\n"
              + "# assocplot(cbind(R$\"Sepal.Length\", R$\"Petal.Length\"), "
              + "space=0.5, col=c(\"black\", \"red\"))");
        LABEL2COMMAND.put("Histograms",
                "# data(iris)\n# R <- iris\n"
              + "# hist(R$\"Sepal.Length\", density=30.0, angle=45, "
              + "xlab=\"sepal length\")");
        LABEL2COMMAND.put("Pie Charts",
                "# data(iris)\n# R <- iris\n"
              + "# t <- R$\"Sepal.Length\"[1:10]\n"
              + "# pie(t, col = rainbow(10), radius = 0.9)\n");
        LABEL2COMMAND.put("Scatterplot Matrices",
                "# data(iris)\n# R <- iris\n"
              + "# pairs(R[1:2], labels = c(\"sepal length\", "
              + "\"sepal width\"))\n"
              + "pairs(R)");
        LABEL2COMMAND.put("Sunflower Scatter Plot",
                "# data(iris)\n# R <- iris\n"
              + "# sunflowerplot(R[1:2], rotate=FALSE, pch='*')\n"
              + "sunflowerplot(R)");
        LABEL2COMMAND.put("Draw Function Plots",
                "# curve(log(x), 0.1, 5)");
        LABEL2COMMAND.put("Mosaic Plots",
                "# mosaicplot(Titanic, color = TRUE)");
        LABEL2COMMAND.put("Plot Columns of Matrices",
                "# data(iris)\n# R <- iris\n"
              + "# matplot(R[1:3])\n");
        LABEL2COMMAND.put("1-D Scatter Plots",
                "# data(iris)\n# R <- iris\n"
              + "# stripchart(R$\"Petal.Width\", xlab=\"petal width\")");
        LABEL2COMMAND.put("Polygon Drawing",
                "# x <- c(1:5,5:1)\n"
              + "# y <- c(1,2*(4:2),1,9,8,3:1)\n"
              + "# plot(1:10, 1:10, type=\"n\", xlab=\"x\", ylab=\"y\", "
              + "main=\"Polygon\")\n"
              + "# polygon(x,y, col=\"blue\", lty=2, lwd=2, border=\"black\")");
        LABEL2COMMAND.put("Conditional Density Plots",
                "# data(iris)\n# R <- iris\n"
              + "# sl <- R$\"Sepal.Length\"\n"
              + "# fac <- factor(R$\"Species\")\n"
              + "# cdplot(fac ~ sl)\n");
        LABEL2COMMAND.put("Conditioning Plots",
                "# data(iris)\n# R <- iris\n"
              + "# coplot(Sepal.Length ~ Petal.Length | Species, R)");
        LABEL2COMMAND.put("Perspective Plots",
                "# x <- seq(0, 50, length= 50)\n"
              + "# y <- x\n"
              + "# f <- function(x, y) { r <- sqrt(2*x+10*y); cos(r)*x/r}\n"
              + "# z <- outer(x, y, f)\n"
              + "# persp(x, y, z, theta = 30, phi = 30, expand = 0.5, "
              + "col = \"lightblue\")");
        LABEL2COMMAND.put("Display Contours",
                "# x <- seq(-2*pi, 2*pi, len = 50)\n"
              + "# y <- x\n"
              + "# f <- function(x, y) "
              + "{ r <- (x^2 + y^2); cos(r)*exp(-r/10*pi) }\n"
              + "# z <- outer(x, y, f)\n"
              + "# contour(z, drawlabels = FALSE, axes = FALSE, frame = TRUE)");
        LABEL2COMMAND.put("Spine Plots and Spinograms",
                "# data(iris)\n# R <- iris\n"
              + "# sl <- R$\"Sepal.Length\"\n"
              + "# fac <- factor(R$\"Species\")\n"
              + "# spineplot(fac ~ sl)\n");
        LABEL2COMMAND.put("Star (Spider/Radar) Plots and Segment Diagrams",
                "# data(iris)\n# R <- iris\n"
              + "# stars(R, main = \"Iris data\", axes = TRUE)\n"
              + "stars(R)");
        LABEL2COMMAND.put("Fourfold Plots",
                "# y <- cbind(c(25, 5), c(15, 15))\n"
              + "# dimnames(y)[[2]] <- c(\"Yes\", \"No\")\n"
              + "# rownames(y) <- c(\"A\", \"B\")\n"
              + "# fourfoldplot(y)");
    }

    private RViewScriptingConstants() {
        // nothing to do here.
    }

    /**
     * Default expression key (the name of the default plot).
     */
    public static final String DFT_EXPRESSION_KEY =
        LABEL2COMMAND.keySet().toArray()[0].toString();

    /**
     * @return the default expression command (the R code of the default
     * plot) as String.
     */
    public static final String[] getDefaultExpressionCommands() {
        return getDefaultExpressionCommand().split("\n");
    }

    /**
     * @return the default expression command (the R code of the default
     * plot) as String array.
     */
    public static final String getDefaultExpressionCommand() {
         return LABEL2COMMAND.get(DFT_EXPRESSION_KEY);
    }
}
