package org.knime.r.ui;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

public class RConsole extends JTextPane  {
	private Style m_errorStyle;
	private Style m_normalStyle;
	private Style m_commandStyle;
	private Style m_resultStyle;
	
	public RConsole() {
        m_errorStyle = addStyle("Error Style", null);
        StyleConstants.setForeground(m_errorStyle, Color.red);
        m_normalStyle = addStyle("Normal Style", null);
        StyleConstants.setForeground(m_normalStyle, Color.black);
        m_commandStyle = addStyle("Command Style", null);
        StyleConstants.setForeground(m_commandStyle, Color.darkGray);
        m_resultStyle = addStyle("Result Style", null);
        StyleConstants.setForeground(m_resultStyle, Color.black);   
        
        setEditable(false);
        setDragEnabled(true);
	}

	/**
	 * @return the error style
	 */
	public Style getErrorStyle() {
		return m_errorStyle;
	}

	/**
	 * @return the normal style
	 */
	public Style getNormalStyle() {
		return m_normalStyle;
	}

}
