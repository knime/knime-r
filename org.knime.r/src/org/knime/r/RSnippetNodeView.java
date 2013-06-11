/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * Created on 16.04.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.r;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.interactive.DefaultReexecutionCallback;
import org.knime.core.node.interactive.InteractiveClientNodeView;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.r.template.DefaultTemplateController;
import org.knime.r.template.TemplatesPanel;

/**
 *
 * @author Heiko Hofer
 */
public class RSnippetNodeView extends InteractiveClientNodeView<RSnippetNodeModel, RSnippetViewContent> {
	private static final NodeLogger LOGGER = NodeLogger.getLogger(
	        "R Snippet");
	
    private RSnippetNodePanel m_panel;
	private JTabbedPane m_tabbedPane;
	private Class m_templateMetaCategory;
	private DefaultTemplateController m_templatesController;
    
	/**
     * @param nodeModel
	 * @param templateMetaCategory
	 *            the meta category used in the templates tab or to create
	 *            templates
     */
    protected RSnippetNodeView(final RSnippetNodeModel nodeModel, final Class templateMetaCategory) {
        super(nodeModel);
        m_templateMetaCategory = templateMetaCategory;
        
        m_panel = new RSnippetNodePanel(templateMetaCategory, nodeModel.getRSnippetNodeConfig(), false, true) {

        	@Override
			public void applyTemplate(final RSnippetTemplate template,
                    final DataTableSpec spec,
                    final Map<String, FlowVariable> flowVariables) {
        		super.applyTemplate(template, spec, flowVariables);
        		m_tabbedPane.setSelectedIndex(0);
        	}	
        }; 
        m_panel.getRSnippet().getSettings().loadSettings(nodeModel.getSettings());
            
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        m_tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        m_tabbedPane.addTab("R Snippet", m_panel);
        m_tabbedPane.addTab("Templates", createTemplatesPanel());
        mainPanel.add(m_tabbedPane, BorderLayout.CENTER);        
        
        
        
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 15, 30));
        final JButton tryRun = new JButton("Re-Execute");
        final JButton setAsNewDefault = new JButton("Set as new default");
        
        tryRun.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(final ActionEvent e) {
				triggerReExecution(new RSnippetViewContent(m_panel.getRSnippet().getSettings()), new DefaultReexecutionCallback());
				setAsNewDefault.setEnabled(true);
			}
		});
        tryRun.setEnabled(false);
        buttonsPanel.add(tryRun);
        
        
        setAsNewDefault.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				getNodeModel().loadSettings(m_panel.getRSnippet().getSettings());
				setNewDefaultConfiguration(new DefaultReexecutionCallback());		
			}
		});
        setAsNewDefault.setEnabled(false);
        buttonsPanel.add(setAsNewDefault);
        JButton close = new JButton("Close");
        close.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (!m_panel.getRSnippet().getSettings().getScript().equals(getNodeModel().getSettings().getScript())) {
					int answer = JOptionPane.showOptionDialog(null, 
							"Do you want to discard your recent changes?", 
							"Confirm Cancel", 
							JOptionPane.OK_CANCEL_OPTION, 
							JOptionPane.QUESTION_MESSAGE, 
							null, 
							new Object[] {"Ok", "Set as new default", "Cancel"}, JOptionPane.OK_OPTION);						
			        if (answer == 0) {
			        	closeView();
			        } else if(answer == 1) {
			        	synchronized(getNodeModel()) {
							triggerReExecution(new RSnippetViewContent(m_panel.getRSnippet().getSettings()), new DefaultReexecutionCallback());
							getNodeModel().loadSettings(m_panel.getRSnippet().getSettings());
							setNewDefaultConfiguration(new DefaultReexecutionCallback());
				        	closeView();
			        	}
			        }
				} else {
					closeView();
				}

			}
		});
        buttonsPanel.add(close);   
        
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        RSnippet snippet = m_panel.getRSnippet();
        snippet.getDocument().addDocumentListener(new DocumentListener() {
			
        	private void documentChanged(final boolean changed) {
        		tryRun.setEnabled(changed);
				setAsNewDefault.setEnabled(!changed);
        	}
        	
			@Override
			public void removeUpdate(final DocumentEvent e) {
				documentChanged(true);
			}
			
			@Override
			public void insertUpdate(final DocumentEvent e) {
				documentChanged(true);
			}
			
			@Override
			public void changedUpdate(final DocumentEvent e) {
				documentChanged(true);
			}
		});
        
        setComponent(mainPanel);
    }
    
    /** Create the templates tab. */
    private JPanel createTemplatesPanel() {
        RSnippetNodePanel preview = new RSnippetNodePanel(m_templateMetaCategory, 
        		getNodeModel().getRSnippetNodeConfig(), true, false);
        m_templatesController = new DefaultTemplateController(
                m_panel, preview);
        TemplatesPanel templatesPanel = new TemplatesPanel(
                Collections.singleton(m_templateMetaCategory),
                m_templatesController);
        return templatesPanel;
    }    

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
    	RSnippetNodeModel model = getNodeModel();
    	// if script has been updated    	
    	DataTableSpec spec = model.getInputSpec() != null
    			? model.getInputSpec()
    			: new DataTableSpec(); 
    	if (model.getInputData() != null) {
    		m_panel.updateData(m_panel.getRSnippet().getSettings(), model.getInputData(),
    				model.getAvailableInputFlowVariables().values());
    	} else {   		
	    	m_panel.updateData(m_panel.getRSnippet().getSettings(), spec,
	    			model.getAvailableInputFlowVariables().values());
    	}
    	
    		
		m_templatesController.setDataTableSpec(spec);
        m_templatesController.setFlowVariables(getNodeModel().getAvailableInputFlowVariables());			
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateModel(final Object arg) {
        modelChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        m_panel.onClose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        m_panel.onOpen();
    }
    
    

}
