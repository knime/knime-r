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

import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.util.Pair;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RCommandQueue.RCommandExecutionListener;
import org.knime.r.ui.RConsole;
import org.knime.r.ui.RProgressPanel;

/**
 * Class managing a console and execution of commands pushed to the command
 * queue.
 *
 * @author Heiko Hofer
 * @author Jonathan Hale
 */
public class RConsoleController implements RCommandExecutionListener {

	private RConsole m_pane;

	private final Action m_cancelAction;
	private final Action m_clearAction;
	private final RController m_controller;
	private final RCommandQueue m_commandQueue;

	private final DocumentListener m_docListener;

	/**
	 * Constructor
	 *
	 * @param controller
	 *            RController to use for executing commands etc.
	 */
	public RConsoleController(final RController controller, final RCommandQueue queue) {
		m_controller = controller;
		queue.addRCommandExecutionListener(this);
		m_commandQueue = queue;
		m_cancelAction = new AbstractAction("Terminate") {
			private static final long serialVersionUID = -8509229734952079641L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				cancel();
			}
		};
		final Icon cancelIcon = ViewUtils.loadIcon(this.getClass(), "progress_stop.gif");
		m_cancelAction.putValue(Action.SMALL_ICON, cancelIcon);
		m_cancelAction.putValue(Action.SHORT_DESCRIPTION, "Terminate");
		m_cancelAction.setEnabled(false);

		m_clearAction = new AbstractAction("Clear Console") {
			private static final long serialVersionUID = 3103558058055836569L;

			@Override
			public void actionPerformed(final ActionEvent e) {
				ViewUtils.invokeLaterInEDT(() -> {
					clear();
				});
			}
		};
		final Icon clearIcon = ViewUtils.loadIcon(this.getClass(), "clear_co.gif");
		m_clearAction.putValue(Action.SMALL_ICON, clearIcon);
		m_clearAction.putValue(Action.SHORT_DESCRIPTION, "Clear Console");
		m_clearAction.setEnabled(false);
		m_docListener = new DocumentListener() {

			@Override
			public void removeUpdate(final DocumentEvent e) {
				updateClearAction();
			}

			@Override
			public void insertUpdate(final DocumentEvent e) {
				updateClearAction();
			}

			@Override
			public void changedUpdate(final DocumentEvent e) {
				updateClearAction();
			}

			private void updateClearAction() {
				m_clearAction.setEnabled(m_pane.getDocument().getLength() > 0);
			}
		};
	}

	/**
	 * Cancel the currently executingCommand.
	 *
	 * @see RConsoleController#getCancelAction()
	 */
	public void cancel() {
		// clear commands in queue
		m_commandQueue.clear();

		m_commandQueue.stopExecutionThread();

		try {
			m_controller.terminateAndRelaunch();
		} catch (final Exception e1) {
			append("Could not properly terminate current command.", 1);
		}
	}

	/**
	 * Clear the console pane
	 *
	 * @see RConsoleController#getClearAction()
	 */
	public void clear() {
		if (m_pane != null) {
			m_pane.setText("");
		}
	}

	/**
	 * Attach a RConsole to output any commands and their output to.s
	 *
	 * @param pane
	 */
	public void attachOutput(final RConsole pane) {
		if (m_pane != null) {
			detach(m_pane);
		}

		m_pane = pane;
		if (m_pane != null) {
			m_pane.getDocument().addDocumentListener(m_docListener);
		}
		m_clearAction.setEnabled(false);
	}

	/**
	 * Detach a RConsole. There will be no output to it by this
	 * RConsoleController anymore.
	 *
	 * @param pane
	 */
	public void detach(final RConsole pane) {
		if (pane == null || m_pane != pane) {
			throw new RuntimeException("Wrong text pane to detach.");
		}
		m_pane.getDocument().removeDocumentListener(m_docListener);
		m_pane = null;
	}

	/**
	 * Whether the given RConsole is currently attached to this
	 * RConsoleController.
	 *
	 * @param pane
	 * @return <code>true</code> if the given pane is the currently attached
	 *         one.
	 */
	public boolean isAttached(final RConsole pane) {
		return m_pane == pane;
	}

	/**
	 * Update the state of R engine (currently executing command or not
	 * executing). This updates the enabled state of the terminate action.
	 *
	 * @param busy
	 *            Whether currently executing.
	 */
	public void updateBusyState(final boolean busy) {
		m_cancelAction.setEnabled(busy);
	}

	/**
	 * Interface for creating custom ExecutionMonitors to use for execution of
	 * commands for example. Allows the user to for example attach a
	 * {@link RProgressPanel} for example.
	 *
	 * @author Jonathan Hale
	 */
	public static interface ExecutionMonitorFactory {
		/**
		 * Create a new ExecutionMonitor.
		 *
		 * @return The created {@link ExecutionMonitor}, never <code>null</code>
		 */
		ExecutionMonitor create();
	}

	private final AtomicBoolean m_updateScheduled = new AtomicBoolean(false);

	private final ReentrantLock m_appendBufferLock = new ReentrantLock(true);
	private Deque<Pair<StringBuilder, Integer>> m_buffer = new ArrayDeque<Pair<StringBuilder, Integer>>();

	public void append(final String text, final int oType) {

		if (m_pane != null) {
			// update is scheduled contribute to this update
			m_appendBufferLock.lock();
			try {
				if (m_buffer.size() > 0 && m_buffer.peekLast().getSecond() == oType) {
					m_buffer.peekLast().getFirst().append(text);
				} else {
					m_buffer.offer(new Pair<StringBuilder, Integer>(new StringBuilder(text), oType));
				}
			} finally {
				m_appendBufferLock.unlock();
			}
			// if update is not scheduled
			if (m_updateScheduled.compareAndSet(false, true)) {
				final Runnable doWork = () -> {
					Queue<Pair<StringBuilder, Integer>> buffer = null;
					m_appendBufferLock.lock();
					try {
						m_updateScheduled.set(false);
						buffer = m_buffer;
						m_buffer = new ArrayDeque<Pair<StringBuilder, Integer>>();
					} finally {
						m_appendBufferLock.unlock();
					}

					final StyledDocument doc = m_pane.getStyledDocument();
					while (buffer.size() > 0) {
						final Pair<StringBuilder, Integer> toWrite = buffer.poll();
						try {
							final Style style = toWrite.getSecond() == 0 ? m_pane.getNormalStyle()
									: m_pane.getErrorStyle();
							doc.insertString(doc.getLength(), toWrite.getFirst().toString(), style);
							final int maxDocLength = 20 * 1024 * 1024 / 2; // 20MB
							if (doc.getLength() > maxDocLength) {
								// TODO: Cut by whole line
								doc.remove(0, doc.getLength() - maxDocLength);
							}

						} catch (final BadLocationException e) {
							// never happens
							throw new RuntimeException(e);
						}
					}
				};

				ViewUtils.runOrInvokeLaterInEDT(doWork);
			}
		}
	}

	/**
	 * @return Action for canceling current R execution
	 */
	public Action getCancelAction() {
		return m_cancelAction;
	}

	/**
	 * @return Action for clearing the console.
	 */
	public Action getClearAction() {
		return m_clearAction;
	}

	// --- RCommandExecutionListener methods ---

	@Override
	public void onCommandExecutionStart(final RCommand command) {
		updateBusyState(true);

		if (!command.isShowInConsole()) {
			return;
		}

		final StringTokenizer tokenizer = new StringTokenizer(command.getCommand(), "\n");

		boolean first = true;
		while (tokenizer.hasMoreTokens()) {
			final String line = tokenizer.nextToken();
			if (first) {
				append("> " + line + "\n", 0);
				first = false;
			} else {
				append("+ " + line + "\n", 0);
			}
		}
	}

	@Override
	public void onCommandExecutionEnd(final RCommand command, final String stdout, final String stderr) {
		updateBusyState(false);

		if (!command.isShowInConsole()) {
			// we do not want the output to appear in the console.
			return;
		}

		append(stdout, 0);
		append(stderr, 1);
	}

	@Override
	public void onCommandExecutionCanceled() {
		updateBusyState(false);
	}

	@Override
	public void onCommandExecutionError(final RException e) {
		Throwable exception = e;

		do {
			append("ERROR: " + exception.getMessage(), 1);
			if (exception == exception.getCause()) {
				// avoid infinite loops for exceptions which set themselves as
				// their cause.
				break;
			}
			exception = exception.getCause();
		} while (exception != null);
	}

}
