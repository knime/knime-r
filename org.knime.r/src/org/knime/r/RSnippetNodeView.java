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
import java.util.Collections;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeLogger;
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
        
        m_panel = new RSnippetNodePanel(templateMetaCategory, false) {

        	@Override
			public void applyTemplate(final RSnippetTemplate template,
                    final DataTableSpec spec,
                    final Map<String, FlowVariable> flowVariables) {
        		super.applyTemplate(template, spec, flowVariables);
        		m_tabbedPane.setSelectedIndex(0);
        	}	
        }; 
        
        setShowNODATALabel(false);       
        
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        m_tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        m_tabbedPane.addTab("R Snippet", m_panel);
        m_tabbedPane.addTab("Templates", createTemplatesPanel());
        mainPanel.add(m_tabbedPane, BorderLayout.CENTER);
        setComponent(mainPanel);
        
        
        //super.setShowNODATALabel(false);
//        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
//        p.setBorder(BorderFactory.createTitledBorder(" Append Annotation "));
//
//        final JCheckBox checkBox = new JCheckBox("New Column");
//
//        final JTextField textField = new JTextField();
//        textField.setPreferredSize(new Dimension(150,
//                Math.max(20, textField.getHeight())));
//        textField.addKeyListener(new KeyAdapter() {
//            @Override
//            public void keyPressed(final java.awt.event.KeyEvent e) {
//                if (e == null) {
//                    return;
//                }
//                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
//                    appendAnnotation(
//                            textField.getText(), checkBox.isSelected());
//                }
//            }
//        });
//        p.add(textField);
//
//        JButton button = new JButton("Apply");
//        button.setPreferredSize(new Dimension(100,
//                Math.max(25, button.getHeight())));
//        button.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(final ActionEvent e) {
//                if (e == null) {
//                    return;
//                }
//                appendAnnotation(textField.getText(), checkBox.isSelected());
//            }
//        });
//        p.add(button);
//        p.add(checkBox);
//
//        JPanel panel = new JPanel(new BorderLayout());
//        panel.add(p, BorderLayout.NORTH);
//        TableContentModel cview = new TableContentModel() {
//            /**
//             * {@inheritDoc}
//             */
//            @Override
//            public void hiLite(final KeyEvent e) {
//                modelChanged();
//            }
//            /**
//             * {@inheritDoc}
//             */
//            @Override
//            public void unHiLite(final KeyEvent e) {
//                modelChanged();
//            }
//            /**
//             * {@inheritDoc}
//             */
//            @Override
//            public void unHiLiteAll(final KeyEvent e) {
//                modelChanged();
//            }
//        };
//        m_table = new TableView(cview);
//        super.getJMenuBar().add(m_table.createHiLiteMenu());
//        m_table.setPreferredSize(new Dimension(425, 250));
//        m_table.setShowColorInfo(false);
//        panel.add(m_table, BorderLayout.CENTER);
//        super.setComponent(panel);
    }

//    private void appendAnnotation(final String anno, final boolean newColumn) {
//        if (anno != null && !anno.isEmpty()) {
//            getNodeModel().appendAnnotation(anno, newColumn);
//            //FIXME: Put annotation map in view content
//            triggerReExecution(new RSnippetViewContent(), new DefaultReexecutionCallback());
//        }
//    }
    
    /** Create the templates tab. */
    private JPanel createTemplatesPanel() {
        RSnippetNodePanel preview = new RSnippetNodePanel(m_templateMetaCategory, true);
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
//        DataTable data = super.getNodeModel().getHiLiteAnnotationsTable();
//        m_table.setDataTable(data);
//        HiLiteHandler hdl = super.getNodeModel().getInHiLiteHandler(0);
//        m_table.setHiLiteHandler(hdl);
//        m_table.setColumnWidth(50);
    	RSnippetNodeModel model = getNodeModel();
    	DataTableSpec spec = model.getInputSpec() != null
    			? model.getInputSpec()
    			: new DataTableSpec(); 
    	if (model.getInputData() != null) {
    		m_panel.updateData(model.getSettings(), model.getInputData(),
    				model.getAvailableInputFlowVariables().values());
    	} else {   		
	    	m_panel.updateData(model.getSettings(), spec,
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
