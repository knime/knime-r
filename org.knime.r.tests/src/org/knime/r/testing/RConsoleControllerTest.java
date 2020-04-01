/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 */
package org.knime.r.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RCommandQueue;
import org.knime.r.controller.RConsoleController;
import org.knime.r.controller.RController;
import org.knime.r.ui.RConsole;

/**
 * Test for {@link RConsoleController}.
 *
 * @author Jonathan Hale
 */
public class RConsoleControllerTest {

	private RController m_controller;

	@Before
	public void before() throws RException {
		m_controller = new RController();
		assertNotNull(m_controller);
	}

	@After
	public void after() throws RException {
		if (m_controller == null) {
			return;
		}
		// terminate the R process used by the controller, otherwise it will be
		// leaked.
		try {
			m_controller.close();
		} finally {
			m_controller.terminateRProcess();
		}
	}

	/**
	 * Test for
	 * <ul>
	 * <li>{@link RConsoleController#attachOutput(RConsole)}</li>
	 * <li>{@link RConsoleController#isAttached(RConsole)}</li>
	 * <li>{@link RConsoleController#detach(RConsole)}</li>
	 * <li>{@link RConsoleController#append(String, int)}</li>
	 * <li>{@link RConsoleController#getClearAction()} and its execution</li>
	 * <li>{@link RConsoleController#readCommands()}</li>
	 * <li>{@link RConsoleController#getCancelAction()} and whether it clears
	 * the command queue correctly</li>
	 * </ul>
	 *
	 * @throws BadLocationException
	 *             Should never happen.
	 * @throws InterruptedException
	 */
	@Test
	public void testConsole() throws BadLocationException, InterruptedException {
		final RCommandQueue queue = new RCommandQueue(m_controller);
		final RConsole console = new RConsole();

		final RConsoleController consoleController = new RConsoleController(m_controller, queue);
		consoleController.attachOutput(console);

		/* test isAttached(...) */
		{
			assertTrue(consoleController.isAttached(console));

			final RConsole goblin = new RConsole();
			assertFalse(consoleController.isAttached(goblin));
		}

		/* test append(...) */
		consoleController.append("Hello World!", 0);
		Thread.sleep(100); // append is executed in separate EDT thread

		final StyledDocument doc = console.getStyledDocument();
		assertEquals("Appending to the console failed.", "Hello World!", console.getText());
		assertEquals("Appending to the console document failed.", "Hello World!", doc.getText(0, doc.getLength()));

		/*
		 * check the clear action. It doesn't use the action event, so passing
		 * null as ActionEvent is okay.
		 */
		assertNotNull(consoleController.getClearAction());
		consoleController.getClearAction().actionPerformed(null);
		Thread.sleep(10); // clear is executed in separate EDT thread
		assertEquals("Clearing the console failed.", "", console.getText());

		/*
		 * check the cancel action. It doesn't use the action event, so passing
		 * null as ActionEvent is okay.
		 */
		assertNotNull(consoleController.getCancelAction());
		consoleController.append("Sys.sleep(1000);print(\"failed\")", 0);
		consoleController.append("Sys.sleep(1000);print(\"failed again\")", 0);
		Thread.sleep(60); // wait for command to be taken from queue
		consoleController.getCancelAction().actionPerformed(null);
		assertTrue("Cancel action should clear command queue.", queue.isEmpty());
		assertFalse(queue.isExecutionThreadRunning());

		/* test detach(...) */
		consoleController.detach(console);
		assertFalse(consoleController.isAttached(console));
	}
}
