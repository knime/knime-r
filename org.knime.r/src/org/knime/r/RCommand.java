package org.knime.r;

public class RCommand {
	private String m_command;
	private int m_lineNumber;
	
	public RCommand(final String command) {
		this(command, -1);
	}
	
	public RCommand(final String command, final int lineNumber) {
		m_command = command;
		m_lineNumber = lineNumber;
	}	

	public String getCommand() {
		return m_command;
	}

}
