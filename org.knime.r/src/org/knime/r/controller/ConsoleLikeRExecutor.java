/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   19.11.2015 (Jonathan Hale): created
 */
package org.knime.r.controller;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.r.RSnippetNodeModel;
import org.knime.r.controller.IRController.RException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REXPString;

/**
 * Class which wraps all R and Java code necessary to execute R code with
 * correct errors (inkl. syntax errors), output capturing and printing of the
 * result.
 *
 * @author Jonathan Hale
 * @see RCommandQueue
 * @see RSnippetNodeModel
 */
public class ConsoleLikeRExecutor {

	/** Prefix to prepend to errors in R */
	public static final String ERROR_PREFIX = "Error:";

	/**
	 * R Code to capture output and error messages of R code.
	 *
	 * <pre>
	 * {@code
	 * # setup textConnections (which can be compared with Java StringWriters), which
	 * # we will direct output to. The resulting text will be written into knime.stdout
	 * # and knime.stderr variables.
	 * knime.stdout.con <- textConnection('knime.stdout', 'w')
	 * knime.stderr.con <- textConnection('knime.stderr', 'w')
	 *
	 * sink(knime.stdout.con) # redirect output
	 * sink(knime.stderr.con, type='message') # redirect errors
	 * }
	 * </pre>
	 */
	public static final String CAPTURE_OUTPUT_PREFIX = //
	"knime.stdout.con<-textConnection('knime.stdout','w');knime.stderr.con<-textConnection('knime.stderr','w');sink(knime.stdout.con);sink(knime.stderr.con,type='message')";

	/**
	 * R code for executing a script, catching errors (including syntax errors),
	 * and handling printing the value correctly.
	 *
	 * <pre>
	 * <code>
	 * knime.tmp.ret <- NULL # avoids "knime.tmp.ret not found" in cleanup, if an error occurred in execution.
	 *
	 * # e would be something like "Error in withVisible(...", which does not look nice.
	 * # By only printing the condition message, we can avoid that prefix (and the RException thrown by Rserve otherwise).
	 * printError <- function(e) message(paste('Error:',conditionMessage(e)))
	 *
	 * # we need to be able to print the results of every R command individually.
	 * for(exp in tryCatch(parse(text=knime.tmp.script), error=printError)) {
	 * 	tryCatch( # for custom error output
	 * 		# returns the result with an visibility flag which signals the R console whether the value should be printed.
	 * 		knime.tmp.ret <- withVisible(
	 * 			# parsing the script ourselves enables us to catch syntax errors
	 * 			eval(exp)
	 * 		),
	 * 		error=printError
	 * 	)
	 * 	# $visible is only useful, if there is actually a return value
	 * 	if(!is.null(knime.tmp.ret)) {
	 * 		# print for example would return an invisible value, which would not be printed again.
	 * 		if(knime.tmp.ret$visible) print(knime.tmp.ret$value)
	 * 	}
	 * }
	 * rm(knime.tmp.script,exp,printError) # remove temporary script variable
	 * knime.tmp.ret$value # return the value of the evaluation
	 * </code><pre>
	 */
	public static final String CODE_EXECUTION = //
	"knime.tmp.ret<-NULL;printError<-function(e) message(paste('" + ERROR_PREFIX + "',conditionMessage(e)));for(exp in tryCatch(parse(text=knime.tmp.script),error=printError)){tryCatch(knime.tmp.ret<-withVisible(eval(exp)),error=printError)\n"
			+ "if(!is.null(knime.tmp.ret)) {if(knime.tmp.ret$visible) print(knime.tmp.ret$value)}};rm(knime.tmp.script,exp,printError);knime.tmp.ret$value";

	/**
	 * R Code to finish up capturing output and error messages of R code after
	 * execution of the code to capture output from has finished.
	 *
	 * <pre>
	 * {@code
	 * # return output to normal/stop redirecting output and errors
	 * sink()
	 * sink(type='message')
	 * # close the writers for accessing the result variables
	 * close(knime.stdout.con)
	 * close(knime.stderr.con)
	 * # concatenate the lines with paste(), appending '\n' to every line
	 * # and combine output and error to a vector, to return the combined
	 * # value back to java.
	 * knime.output.ret <- c(
	 *  paste(knime.stdout, collapse='\\n'),
	 *  paste(knime.stderr, collapse='\\n')
	 * )
	 * knime.output.ret # the last value in an r script will be returned by Rserve.
	 * }
	 * </pre>
	 */
	public static final String CAPTURE_OUTPUT_POSTFIX = //
	"sink();sink(type='message')\n" + //
			"close(knime.stdout.con);close(knime.stderr.con)\n" + //
			"knime.output.ret<-c(paste(knime.stdout,collapse='\\n'), paste(knime.stderr,collapse='\\n'))\n" + //
			"knime.output.ret";

	/**
	 * R code to delete temporary variables used for output capturing etc.
	 *
	 * <pre>
	 * {@code
	 * rm(knime.tmp.ret,knime.tmp.script,knime.output.ret,knime.stdout.con,knime.stderr.con,knime.stdout,knime.stderr)
	 * }
	 * </pre>
	 */
	public static final String CAPTURE_OUTPUT_CLEANUP = //
	"rm(knime.tmp.ret,knime.output.ret,knime.stdout.con,knime.stderr.con,knime.stdout,knime.stderr)";

	private final RController m_controller;
	private String stdout = "";
	private String stderr = "";

	/**
	 * Constructor
	 *
	 * @param controller
	 *            to use for evaluating R code
	 */
	public ConsoleLikeRExecutor(final RController controller) {
		m_controller = controller;
	}

	/**
	 * Run R code necessary for starting output capturing.
	 *
	 * @param progress
	 * @throws RException
	 * @throws CanceledExecutionException
	 */
	public void setupOutputCapturing(final ExecutionMonitor progress) throws RException, CanceledExecutionException {
		m_controller.monitoredEval(CAPTURE_OUTPUT_PREFIX, progress, false);
	}

	/**
     * Execute and R script and handle printing of the result aswell as correctly printing errors.
     *
     * <b>Performance notes:</b> If the result is not needed, use {@link #executeIgnoreResult(String, ExecutionMonitor)}
     * instead.
     *
     * @param script The script to execute
     * @param progress For monitoring progress.
     * @return The result of the evaluation
     * @throws RException
     * @throws CanceledExecutionException
     */
	public REXP execute(final String script, final ExecutionMonitor progress)
			throws RException, CanceledExecutionException {

		// execute command
		REXP ret = null;
		// manage correct printing of command execution and
		// return the produced value.
		try {
			m_controller.assign("knime.tmp.script", new REXPString(script));
		} catch (RException e) {
			throw new RException("Transferring the R script to R failed.", e);
		}
		ret = m_controller.monitoredEval(CODE_EXECUTION, progress, true);

		return ret;
	}

    /**
     * Execute and R script and handle correctly printing errors, but prevent result from being transferred.
     *
     * @param script The script to execute
     * @param progress For monitoring progress.
     * @throws RException
     * @throws CanceledExecutionException
     */
	public void executeIgnoreResult(final String script, final ExecutionMonitor progress)
			throws RException, CanceledExecutionException {
		// manage correct printing of command execution
		try {
			m_controller.assign("knime.tmp.script", new REXPString(script));
		} catch (RException e) {
			throw new RException("Transferring the R script to R failed.", e);
		}
		m_controller.monitoredEval(CODE_EXECUTION, progress, false);
	}

	/**
	 * Retrieve captured output from R.
	 *
	 * @param progress Execution monitor
	 * @throws RException
	 * @throws CanceledExecutionException
	 */
	public void finishOutputCapturing(final ExecutionMonitor progress) throws RException, CanceledExecutionException {
		String err = "", out = "";
		REXP output = null;
		try {
			output = m_controller.monitoredEval(CAPTURE_OUTPUT_POSTFIX, progress, true);
			if (output != null && output.isString() && output.asStrings().length == 2) {
				out = output.asStrings()[0];
				if (!out.isEmpty()) {
					out += "\n";
				}
				err = output.asStrings()[1];
				if (!err.isEmpty()) {
					err += "\n";
				}
			}

		} catch (REXPMismatchException e) {
			throw new RException("Tried to parse a non-string as string.", e);
		}

		stdout = out;
		stderr = err;
	}

	/**
	 * @return The output generated by the last
	 *         {@link #execute(String, ExecutionMonitor)} call.
	 */
	public String getStdOut() {
		return stdout;
	}

	/**
	 * @return The error output generated by the last
	 *         {@link #execute(String, ExecutionMonitor)} call.
	 */
	public String getStdErr() {
		return stderr;
	}

	/**
	 * Cleanup temporary variables, which were created during output capturing
	 * and execute.
	 *
	 * @param progress Execution monitor
	 * @throws CanceledExecutionException
	 * @throws RException
	 */
	public void cleanup(final ExecutionMonitor progress) throws RException, CanceledExecutionException {
		// cleanup variables which are not needed anymore
		m_controller.monitoredEval(CAPTURE_OUTPUT_CLEANUP, progress, false);
	}

}
