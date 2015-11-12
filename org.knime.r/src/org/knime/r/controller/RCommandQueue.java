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
package org.knime.r.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RConsoleController.ExecutionMonitorFactory;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;

/**
 * A {@link LinkedBlockingQueue} which contains {@link RCommand} and as means of
 * starting a thread which will sequentially execute the RCommands in the queue.
 *
 * @author Jonathan Hale
 * @author Heiko Hofer
 */
public class RCommandQueue extends LinkedBlockingQueue<RCommand> {

	/** Generated serialVersionUID */
	private static final long serialVersionUID = 1850919045704831064L;

	private final NodeLogger LOGGER = NodeLogger.getLogger(RController.class);

	private RCommandConsumer m_thread = null;

	private final RController m_controller;

	/**
	 * R Code to capture output and error messages of R code.
	 *
	 * <pre>
	 * {@code
	 * # setup textConnections (which can be compared with Java StringWriters), which
	 * # we will direct output to. The resulting text will be written into knime.stdout
	 * # and knime.stderr varaibles.
	 * knime.stdout.con <- textConnection('knime.stdout', 'w')
	 * knime.stderr.con <- textConnection('knime.stderr', 'w')
	 *
	 * sink(knime.stdout.con) # redirect output
	 * sink(knime.stderr.con, type='message') # redirect errors
	 * }
	 * </pre>
	 */
	public final static String CAPTURE_OUTPUT_PREFIX;
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
	 * # delete variables we don't need anymore.
	 * rm(knime.stdout.con, knime.stderr.con, knime.stdout, knime.stderr)
	 * knime.output.ret # the last value in an r script will be returned by Rserve.
	 * }
	 * </pre>
	 */
	public final static String CAPTURE_OUTPUT_POSTFIX;
	/**
	 * Delete the `knime.output.ret` variable, which couldn't be cleaned up in
	 * {@link #CAPTURE_OUTPUT_POSTFIX}, because we need it as the last value.
	 */
	public final static String CAPTURE_OUTPUT_CLEANUP;

	static {
		CAPTURE_OUTPUT_PREFIX = "knime.stdout.con<-textConnection('knime.stdout','w');knime.stderr.con<-textConnection('knime.stderr','w');sink(knime.stdout.con);sink(knime.stderr.con,type='message')\n";
		CAPTURE_OUTPUT_POSTFIX = "sink();sink(type='message')\n" + //
				"close(knime.stdout.con);close(knime.stderr.con)\n" + //
				"knime.output.ret<-c(paste(knime.stdout,collapse='\\n'), paste(knime.stderr,collapse='\\n'))\n" + //
				"rm(knime.stdout.con,knime.stderr.con,knime.stdout,knime.stderr)\n" + //
				"knime.output.ret";
		CAPTURE_OUTPUT_CLEANUP = "rm(knime.output.ret)";
	}

	/**
	 * Constructor
	 *
	 * @param controller
	 *            {@link RController} for evaluating R code in the execution
	 *            thread.
	 */
	public RCommandQueue(final RController controller) {
		m_controller = controller;
	}

	/**
	 * Inserts the specified script at the tail of this queue, waiting if
	 * necessary for space to become available.
	 *
	 * @param showInConsole
	 *            If the command should be copied into the R console.
	 * @return {@link RCommand} which wraps the added <code>rScript</code>. Use
	 *         {@link RCommand#get()} to wait for execution and fetch the result
	 *         of evaluation if any.
	 * @throws InterruptedException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             {@inheritDoc}
	 */
	public RCommand putRScript(final String rScript, final boolean showInConsole) throws InterruptedException {
		RCommand rCommand = new RCommand(rScript.trim(), showInConsole);
		put(rCommand);
		return rCommand;
	}

	/**
	 * Interface for classes listening to the exection of commands in a
	 * {@link RCommandQueue}.
	 *
	 * @author Jonathan Hale
	 */
	public interface RCommandExecutionListener {

		/**
		 * Called before execution of a command is started.
		 *
		 * @param command
		 *            the command to be executed
		 */
		public void onCommandExecutionStart(RCommand command);

		/**
		 * Called after a command has been completed, even if an error occurred
		 * and {@link #onCommandExecutionError(RException)} was called before.
		 *
		 * @param command
		 *            command which has been completed
		 */
		public void onCommandExecutionEnd(RCommand command, String stdout, String stderr);

		/**
		 * Called when an error occurs during execution of a command.
		 *
		 * @param e
		 *            The exception that occurred.
		 */
		public void onCommandExecutionError(RException e);

		/**
		 * Called when a command is cancelled.
		 */
		public void onCommandExecutionCanceled();

	}

	private final Set<RCommandExecutionListener> m_listeners = new HashSet<>();

	/**
	 * Add a {@link RCommandExecutionListener} to listen to this RCommandQueue.
	 *
	 * @param l
	 *            the Listener
	 */
	public void addRCommandExecutionListener(final RCommandExecutionListener l) {
		m_listeners.add(l);
	}

	/**
	 * Remove a {@link RCommandExecutionListener} from this RCommandQueue.
	 *
	 * @param l
	 *            the Listener
	 */
	public void removeRCommandExecutionListener(final RCommandExecutionListener l) {
		m_listeners.remove(l);
	}

	/**
	 * Thread which executes RCommands from the command queue.
	 *
	 * @author Jonathan Hale
	 */
	protected class RCommandConsumer extends Thread {

		private ExecutionMonitorFactory m_execMonitorFactory;

		public RCommandConsumer(final ExecutionMonitorFactory execMonitorFactory) {
			m_execMonitorFactory = execMonitorFactory;
		}

		@Override
		public void run() {
			ExecutionMonitor progress = null;
			try {
				while (!isInterrupted()) {
					// interrupted flag is checked every 100 milliseconds while
					// command queue is empty.
					RCommand nextCommand = poll(100, TimeUnit.MILLISECONDS);

					if (nextCommand == null) {
						/* queue was empty */
						continue;
					}

					// we fetch the entire contents of the queue to be able to
					// show progress for all commands currently in queue. This
					// is neccessary to prevent flashing of the progressbar in
					// RSnippetNodePanel, which would happen because "invisible"
					// commands are executed before and after user commands are
					// executed.
					final ArrayList<RCommand> commands = new ArrayList<>();
					do {
						commands.add(nextCommand);
					} while ((nextCommand = poll()) != null);

					progress = m_execMonitorFactory.create(1.0);
					int curCommandIndex = 0;
					final int numCommands = commands.size();
					progress.setProgress(0.0, "Executing commands...");
					for (RCommand rCmd : commands) {
						m_listeners.stream().forEach((l) -> l.onCommandExecutionStart(rCmd));
						// setup for capturing output
						if (rCmd.isShowInConsole()) {
							try {
								m_controller.monitoredEval(CAPTURE_OUTPUT_PREFIX, progress);
							} catch (final RException e) {
								m_listeners.stream().forEach((l) -> l.onCommandExecutionError(
										new RException("Could not capture output of command.", e)));
							}
						}

						// execute command
						REXP ret = null;
						try {
							// manage correct printing of command execution and return the produced value.
							ret = m_controller.monitoredEval(makeConsoleLikeCommand(rCmd.getCommand()) + "\nknime.tmp.ret$value", progress);
						} catch (final RException e) {
							m_listeners.stream().forEach((l) -> l.onCommandExecutionError(e));
						}

						// retrieve output
						String out = "";
						String err = "";
						if (rCmd.isShowInConsole()) {
							REXP output = null;
							try {
								output = m_controller.monitoredEval(CAPTURE_OUTPUT_POSTFIX, progress);
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

								// cleanup variables which are not needed anymore
								m_controller.monitoredEval("rm(knime.tmp.ret);" + CAPTURE_OUTPUT_CLEANUP, progress);
							} catch (final RException | REXPMismatchException e) {
								m_listeners.stream().forEach((l) -> l.onCommandExecutionError(
										new RException("Could not capture output of command.", e)));
							}
						}

						// complete Future to notify all threads waiting on it
						rCmd.complete(ret);

						final String stdout = out;
						final String stderr = err;
						m_listeners.stream().forEach((l) -> l.onCommandExecutionEnd(rCmd, stdout, stderr));
						progress.setProgress((float) ++curCommandIndex / numCommands);
					}
					progress.setProgress(1.0, "Done!");
					progress = null;
				}
			} catch (InterruptedException | CanceledExecutionException e) {
				try {
					if (progress != null && progress.getProgressMonitor().getProgress() != 1.0) {
						m_controller.terminateAndRelaunch();
					}
				} catch (final Exception e1) {
					// Could not terminate Rserve properly. Politely ask user to
					// do it manually. Should basically never happen.
					LOGGER.error(
							"Could not properly terminate Rserve process. It may still be running in the background. Please try to terminate it manually.");
				}

				if (progress != null) {
					progress.setProgress(1.0);
				}

				m_listeners.stream().forEach((l) -> l.onCommandExecutionCanceled());
			}
		}

	}

	/**
	 * Wrap the command in R code which handles correctly printing its return
	 * value.
	 *
	 * <pre>
	 * {@code
	 * tryCatch(
	 * # Some return values are wrapped in an `invisible()` call, which
	 * # signals that their return value may not be printed by the console.
	 * # To access the invisible flag which is appended by that function,
	 * # we need to use `withVisible`.
	 * 	knime.tmp.ret<-withVisible({ ... }),
	 *
	 * # when an error occurs, its condition message will be printed.
	 * # This avoids an "Error in withVisible({" prefix in our error
	 * # messages. We prepend our own 'Error:' prefix. A whitespace is
	 * # inserted in between by paste.
	 * 	error=function(e) message(paste('Error:',conditionMessage(e)))
	 * )
	 * if(!is.null(knime.tmp.ret)) {
	 * # Here is where we check the visibility flag to make sure we only
	 * # print to the console if we need to.
	 * 	if(knime.tmp.ret$visible)
	 * 		print(knime.tmp.ret$value)
	 * }
	 * }
	 * </pre>
	 *
	 * @param command Command to wrap.
	 * @return
	 */
	public static String makeConsoleLikeCommand(final String command) {
		return "tryCatch(knime.tmp.ret<-withVisible({" + command
				+ "}),error=function(e) message(paste('Error:',conditionMessage(e))))\n"
				+ "if(!is.null(knime.tmp.ret)) {if(knime.tmp.ret$visible) print(knime.tmp.ret$value)}";
	}

	/**
	 * Start this queues execution thread (Thread which executes the queues
	 * {@link RCommand}s).
	 *
	 * @param controller
	 *            Controller to use for execution of R code
	 * @param factory
	 *            Factory creating {@link ExecutionMonitor}s
	 * @see #startExecutionThread(RController, ExecutionMonitorFactory)
	 * @see #stopExecutionThread()
	 * @see #isExecutionThreadRunning()
	 */
	public void startExecutionThread() {
		startExecutionThread((maxProgress) -> {
			return new ExecutionMonitor();
		});
	}

	/**
	 * @return <code>true</code> if the execution thread is currently running.
	 * @see #startExecutionThread(RController)
	 * @see #startExecutionThread(RController, ExecutionMonitorFactory)
	 * @see #stopExecutionThread()
	 */
	public boolean isExecutionThreadRunning() {
		return m_thread != null && m_thread.isAlive();
	}

	/**
	 * Start this queues execution thread (Thread which executes the queues
	 * {@link RCommand}s).
	 *
	 * @param controller
	 *            Controller to use for execution of R code
	 * @param factory
	 *            Factory creating {@link ExecutionMonitor}s
	 * @see #startExecutionThread(RController)
	 * @see #stopExecutionThread()
	 * @see #isExecutionThreadRunning()
	 */
	public void startExecutionThread(final ExecutionMonitorFactory factory) {
		if (m_thread != null && m_thread.isAlive()) {
			throw new IllegalStateException("Can only launch one R execution thread on a RCommandQueue.");
		}

		m_thread = new RCommandConsumer(factory);
		m_thread.start();
	}

	/**
	 * Stop the queues execution thread. Does nothing if the queue is already
	 * stopped.
	 *
	 * @see #startExecutionThread(RController)
	 * @see #startExecutionThread(RController, ExecutionMonitorFactory)
	 * @see #isExecutionThreadRunning()
	 */
	public void stopExecutionThread() {
		if (m_thread != null && m_thread.isAlive()) {
			m_thread.interrupt();
		}
	}

}
