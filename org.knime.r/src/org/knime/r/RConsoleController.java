package org.knime.r;

import java.awt.event.ActionEvent;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;

import org.knime.core.node.util.ViewUtils;
import org.knime.core.util.Pair;
import org.knime.r.ui.RConsole;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.JRI.JRIEngine;

public class RConsoleController implements RMainLoopCallbacks {
	
	private RConsole m_pane;
	
	private final Queue<RCommand> m_commands;
	private Lock m_lock = new ReentrantLock();
	private final Condition m_workspaceChanged;
	private final Action m_cancelAction;
	private final Action m_clearAction;
	private boolean m_idle;

	private DocumentListener m_docListener;	

	private AtomicBoolean m_stop = new AtomicBoolean(false);
	
	public RConsoleController() {
		m_commands = new LinkedList<RCommand>();
		m_lock = new ReentrantLock();
		m_workspaceChanged = m_lock.newCondition();
		m_cancelAction = new AbstractAction("Terminate") {
			@Override
			public void actionPerformed(final ActionEvent e) {
				// clear commands in queue
				RController.getDefault().getConsoleQueue().clear();
				// clear commands 
				m_commands.clear();
				
				if (!m_idle) {
					// stop R if not idle
					(RController.getDefault().getJRIEngine()).getRni().rniStop(0);
				}				
			}
		};
		Icon cancelIcon = ViewUtils.loadIcon(this.getClass(), "progress_stop.gif");
		m_cancelAction.putValue(Action.SMALL_ICON, cancelIcon);
		m_cancelAction.putValue(Action.SHORT_DESCRIPTION, "Terminate");
		m_cancelAction.setEnabled(false);
		
		m_clearAction = new AbstractAction("Clear Console") {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (m_pane != null) {
					
					ViewUtils.invokeLaterInEDT(new Runnable() {
						@Override
						public void run() {
							if (m_pane != null) {
								m_pane.setText("");
							}							
						}
					});
					
				}
			}
		};
		Icon clearIcon = ViewUtils.loadIcon(this.getClass(), "clear_co.gif");
		m_clearAction.putValue(Action.SMALL_ICON, clearIcon);		
		m_clearAction.putValue(Action.SHORT_DESCRIPTION, "Clear Console");
		m_clearAction.setEnabled(false);
		m_idle = true;		
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

	public void detach(final JTextPane pane) {
		if (pane == null || m_pane != pane) {
			throw new RuntimeException("Wrong text pane to detach.");
		}
		m_pane.getDocument().removeDocumentListener(m_docListener);
		m_pane = null;
	}	
	
	public boolean isAttached(final JTextPane pane) {
		return m_pane == pane;
	}
	
	@Override
	public void rBusy(final Rengine arg0, final int arg1) {
		if (arg1 == 1) {
			m_cancelAction.setEnabled(true);
		}
		if (arg1 == 0 && m_commands.size() == 0) {
			m_cancelAction.setEnabled(false);
		}	
		m_idle = arg1 == 0;
	}

	@Override
	public String rChooseFile(final Rengine arg0, final int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void rFlushConsole(final Rengine arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rLoadHistory(final Rengine arg0, final String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String rReadConsole(final Rengine rengine, final String prompt, final int addToHistory) {
		if (m_commands.isEmpty()) {
			m_lock.lock();
			m_workspaceChanged.signalAll();
			m_lock.unlock();
			RCommandQueue queue = RController.getDefault().getConsoleQueue();
			while (m_commands.isEmpty() && !m_stop.get()) {
				try {
					// wait on queue, which means that this threads waits until either notifyAll is called
					// on the queue ore 100ms elapsed.
					Collection<RCommand> cmds = queue.poll(100l, TimeUnit.MILLISECONDS);
					if (cmds != null) {
						m_commands.addAll(cmds);
					} else {
						JRIEngine engine = RController.getDefault().getJRIEngine(); 
						if (engine != null) {
							// give R the chance to evaluate other requests
							engine.getRni().rniIdle();
						}
					}
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		if (m_stop.get()) {
			m_commands.clear();
			m_stop.set(true);
			return null;
		}
		final RCommand rCmd = m_commands.poll();
		if (rCmd != null && rCmd.isShowInConsole()) {
			append(prompt + rCmd.getCommand() + "\n", 0);
		} 

		return (rCmd.getCommand() == null || rCmd.getCommand().length() == 0) ? "\n" : rCmd.getCommand() + "\n";
	}

	@Override
	public void rSaveHistory(final Rengine arg0, final String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rShowMessage(final Rengine arg0, final String arg1) {
		// TODO Auto-generated method stub
		
	}
	
	private final AtomicBoolean m_updateScheduled = new AtomicBoolean(false);

	
	private final ReentrantLock m_appendBufferLock = new ReentrantLock(true);
	Deque<Pair<StringBuilder, Integer>> m_buffer = new ArrayDeque<Pair<StringBuilder,Integer>>();
	
	@Override
	public void rWriteConsole(final Rengine rengine, final String text, final int oType) {
		append(text, oType);
		

	}

	private void append(final String text, final int oType) {
		
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
				Runnable doWork = new Runnable() {
					@Override
					public void run() {
						Queue<Pair<StringBuilder, Integer>> buffer = null;
						m_appendBufferLock.lock();						
						try {
							m_updateScheduled.set(false);	
							buffer = m_buffer;
							m_buffer = new ArrayDeque<Pair<StringBuilder,Integer>>();		
						} finally {
							m_appendBufferLock.unlock();
						}
						
					    StyledDocument doc = m_pane.getStyledDocument();
					    while (buffer.size() > 0) {
					    	Pair<StringBuilder, Integer> toWrite = buffer.poll();
							try {
								Style style = toWrite.getSecond() == 0 ? m_pane.getNormalStyle() : m_pane.getErrorStyle();
								doc.insertString(doc.getLength(), toWrite.getFirst().toString(), style);
							} catch (BadLocationException e) {
								// never happens
								throw new RuntimeException(e);
							}
						}				    
					}
				};
			
		     	ViewUtils.runOrInvokeLaterInEDT(doWork);				
			}
		}
	}

	void waitForWorkspaceChange() throws InterruptedException {
		m_lock.lock();
		m_workspaceChanged.await();
		m_lock.unlock();
	}

	public Action getCancelAction() {
		return m_cancelAction;
	}

	public Action getClearAction() {
		return m_clearAction;
	}

	public void stop() {
		m_stop.compareAndSet(false, true);	
	}


}
