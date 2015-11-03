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
package org.knime.r;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.r.RConsoleController.ExecutionMonitorFactory;
import org.rosuda.REngine.REXP;

/**
 * A {@link LinkedBlockingQueue} which contains {@link RCommand} and as means of
 * starting a thread which will sequentially execute the RCommands in the queue.
 * 
 * @author Jonathan Hale
 * @author Heiko Hofer
 */
public class RCommandQueue extends LinkedBlockingQueue<Collection<RCommand>> {

	/** Generated serialVersionUID */
	private static final long serialVersionUID = 1850919045704831064L;

	private final NodeLogger LOGGER = NodeLogger.getLogger(RController.class);

	private RCommandConsumer m_thread = null;

	private final RController m_controller;

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
	 * @return {@link RCommand} describing the last command of
	 *         <code>rScript</code>. Since the commands are executed stricly
	 *         sequentially, all commands will have completed once this returned
	 *         command has. Use {@link RCommand#get()} to wait for execution and
	 *         fetch the result of evaluation if any.
	 * @throws InterruptedException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             {@inheritDoc}
	 */
	public RCommand putRScript(final String rScript, final boolean showInConsole) throws InterruptedException {
		final ArrayList<RCommand> cmds = new ArrayList<>();
		final StringTokenizer tokenizer = new StringTokenizer(rScript, "\n");

		while (tokenizer.hasMoreTokens()) {
			final String cmd = tokenizer.nextToken();
			cmds.add(new RCommand(cmd.trim(), showInConsole));
		}
		put(cmds);

		// return last element
		return cmds.get(cmds.size() - 1);
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
		 * Called after a command has been completed.
		 * 
		 * @param command
		 *            command which has been completed
		 */
		public void onCommandExecutionEnd(RCommand command);

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
					Collection<RCommand> nextCommands = poll(100, TimeUnit.MILLISECONDS);

					if (nextCommands == null) {
						/* queue was empty */
						continue;
					}

					// we fetch the entire contents of the queue to be able to
					// show progress for all commands currently in queue
					final ArrayList<RCommand> commands = new ArrayList<>(nextCommands);
					while ((nextCommands = poll()) != null) {
						commands.addAll(nextCommands);
					}

					progress = m_execMonitorFactory.create(1.0);
					int curCommandIndex = 0;
					final int numCommands = commands.size();
					progress.setProgress(0.0, "Executing commands...");
					for (RCommand rCmd : commands) {
						m_listeners.stream().forEach((l) -> l.onCommandExecutionStart(rCmd));
						// catch textual output from command with
						// paste/capture
						final String cmd = rCmd.isShowInConsole()
								? "paste(capture.output(" + rCmd.getCommand() + "),collapse='\\n')\n"
								// textual output not needed:
								: rCmd.getCommand();

						// execute command
						final REXP ret = m_controller.monitoredEval(cmd, progress, false);

						// complete Future to notify all threads waiting on it
						rCmd.complete(ret);

						m_listeners.stream().forEach((l) -> l.onCommandExecutionEnd(rCmd));
						progress.setProgress((float)++curCommandIndex / numCommands);
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
