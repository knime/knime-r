package org.knime.r;

import java.awt.Color;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;
import org.rosuda.REngine.JRI.JRIEngine;

public class RConsoleController implements RMainLoopCallbacks {
	private Style m_errorStyle;
	private Style m_normalStyle;
	private Style m_commandStyle;
	private Style m_resultStyle;
	
	private JTextPane m_pane;
	
	private Queue<RCommand> m_commands;
	private Lock m_lock = new ReentrantLock();
	private Condition m_workspaceChanged;

	public RConsoleController() {
		m_commands = new LinkedList<RCommand>();
		m_lock = new ReentrantLock();
		m_workspaceChanged = m_lock.newCondition();
	}
	
	public void attachOutput(final JTextPane pane) {
		if (m_pane != null) {
			detach(m_pane);
		}
		
		m_pane = pane;
		
        m_errorStyle = m_pane.addStyle("Error Style", null);
        StyleConstants.setForeground(m_errorStyle, Color.red);
        m_normalStyle = m_pane.addStyle("Normal Style", null);
        StyleConstants.setForeground(m_normalStyle, Color.black);
        m_commandStyle = m_pane.addStyle("Command Style", null);
        StyleConstants.setForeground(m_commandStyle, Color.darkGray);
        m_resultStyle = m_pane.addStyle("Result Style", null);
        StyleConstants.setForeground(m_resultStyle, Color.black);   
        
        m_pane.setEditable(false);
        m_pane.setDragEnabled(true);
	}

	public void detach(final JTextPane pane) {
		if (pane == null || m_pane != pane) {
			throw new RuntimeException("Wrong text pane to detach.");
		}
		m_pane.removeStyle("Error Style");
		m_pane.removeStyle("Normal Style");
		m_pane.removeStyle("Command Style");
		m_pane.removeStyle("Result Style");
		m_pane = null;
	}	
	
	/**
	 * Append text to pane.
	 * 
	 * @param str text to be appended
	 * @param a attribute-set for insertion
	 */
	public void append(final String str, final AttributeSet a) {
		if (m_pane != null) {
			Document doc = m_pane.getDocument();
			if (doc != null) {
				try {
					doc.insertString(doc.getLength(), str, a);
				} catch (BadLocationException e) {
					// never happens
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	@Override
	public void rBusy(final Rengine arg0, final int arg1) {
		// TODO Auto-generated method stub
		
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
//			if (m_commands.isEmpty()) {
				m_lock.lock();
				m_workspaceChanged.signalAll();
				m_lock.unlock();
//			}			
			RCommandQueue queue = RController.getDefault().getConsoleQueue();
			while (m_commands.isEmpty()) {
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
		final RCommand rCmd = m_commands.poll();
		if (rCmd != null) {
			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
						append(prompt + rCmd.getCommand() + "\n", m_commandStyle);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};
			SwingUtilities.invokeLater(runnable);
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

	@Override
	public void rWriteConsole(final Rengine rengine, final String text, final int oType) {
		if (m_pane != null) {
			Runnable doWork = new Runnable() {
				@Override
				public void run() {
					StyledDocument doc = m_pane.getStyledDocument();
					try {
						Style style = oType == 0 ? m_normalStyle : m_errorStyle;
						doc.insertString(doc.getLength(), text, style);
					} catch (BadLocationException e) {
						// never happens
						throw new RuntimeException(e);
					}
				}
			};
		
	     	SwingUtilities.invokeLater(doWork);
		}
	}

	void waitForWorkspaceChange() throws InterruptedException {
		m_lock.lock();
		m_workspaceChanged.await();
		m_lock.unlock();
	}


}
