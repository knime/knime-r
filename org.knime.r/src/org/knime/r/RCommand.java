package org.knime.r;

public final class RCommand {
	private final String m_command;
	private final int m_lineNumber;
    private final boolean m_showInConsole;
	
	public RCommand(final String command, final int lineNumber, boolean showInConsole) {
		m_command = command;
		m_lineNumber = lineNumber;
        m_showInConsole = showInConsole;
	}
	
	public boolean isShowInConsole() {
        return m_showInConsole;
    }

	public String getCommand() {
		return m_command;
	}

}
