# KNIME® - R Integration

This repository contains the plugins for the

 * KNIME Interactive R Statistics Integration
 * Windows R binaries for the interactive R integration
 * KNIME Microsoft SQL R integration

plugins which contain a set of KNIME nodes for running R code in KNIME.

 _Please note: KNIME Interactive R Statistics Integration and Windows binary plugins
are available through the [KNIME update site](https://www.knime.com/downloads/update)
while the Microsoft SQL R integration is contained in [KNIME Labs](http://tech.knime.org/knime-labs)._

## Overview

### Port Object

The KNIME Interactive R Statistics Integration has its own port type, the R Workspace. It allows
passing an entire R workspace, including all of its data, variables and loaded libraries,
on to a subsequent node.

Nodes that accept an R workspace rather than just modifying it, load a copy of the entire
workspace to ensure the same workspace can be reused by multiple subsequent nodes.

### KNIME Interactive R Statistics Integration Nodes

![KNIME Interactive R Statistics Integration nodes](https://www-cdn.knime.com/sites/default/files/styles/inline_medium/public/nodeguide/example-of-r-snippet.png)

The above image shows the various nodes available in the KNIME Interactive R Statistics Integration
plugin for KNIME.

-   **R Source nodes** allow running an R code snippet to generate or load data
    and output it either as KNIME data table or as an R workspace port.
-   **R Snippet** node allows running an R code snippet on an KNIME data table
    and outputs the result as a KNIME data table.
-   **R To R** allows running an R code snippet on an existing R workspace.
-   **Add Table to R** allows running an R code snippet on *both* an existing R
    workspace *and* a KNIME data table.
-   **R to Table** allows running an R code snippet on an existing workspace
    and converts resulting data into a KNIME table.
-   **R Views** allow running R code that generates a PNG image (e.g. plots)
    and outputs this image through an Image Port.
-   **R Learner** and **R Predictor** allow learning an R model and running
    predictions using this model.
-   **R Model Writer** writes an R model.
-   **R to PMML** serializes an R model in the PMML format.

### KNIME Microsoft R Integration Nodes

Currently this plugin only contains the **Run R Model in Microsoft SQL Server** node,
which runs a SQL query with R code on a SQL table and outputs the result to a SQL table.

The node does not require an R environment, but instead a
[Microsoft SQL Server](https://www.microsoft.com/en-us/sql-server/sql-server-2016)
installation with Microsoft R setup.

## Example Workflows

You can download the example workflows from the KNIME public example
server (07\_Scripting/02\_R/01\_Example\_of\_R\_Snippet - see [here how to
connect...](https://www.knime.org/example-workflows)) or from the [KNIME node guide](https://www.knime.com/nodeguide/scripting/r/example-of-r-snippet).

## Development Notes

You can find instructions on how to work with our code or develop extensions for
KNIME Analytics Platform in the _knime-sdk-setup_ repository
on [BitBucket](https://bitbucket.org/KNIME/knime-sdk-setup)
or [GitHub](http://github.com/knime/knime-sdk-setup).

## Join the Community!

* [KNIME Forum](https://tech.knime.org/forum)



