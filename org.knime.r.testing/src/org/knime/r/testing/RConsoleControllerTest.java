package org.knime.r.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.knime.r.RConsoleController;
import org.knime.r.RController;
import org.knime.r.ui.RConsole;

/**
 * Test for {@link RConsoleController}.
 * 
 * @author Jonathan Hale
 */
public class RConsoleControllerTest {

	private RController m_controller;

	@Before
	public void beforeClass() {
		m_controller = new RController();
		assertNotNull(m_controller);
	}

	@After
	public void afterClass() {
		// terminate the R process used by the controller, otherwise it will be
		// leaked.
		m_controller.terminateRProcess();
		m_controller.close();
	}

	@Test
	public void testCreation() {
		RConsoleController consoleController = m_controller.getConsoleController();
		assertNotNull(consoleController);
		assertNotNull(m_controller.getCommandQueue());
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
		final RConsole console = new RConsole();

		final RConsoleController consoleController = m_controller.getConsoleController();
		consoleController.attachOutput(console);

		/* test isAttached(...) */
		{
			assertTrue(consoleController.isAttached(console));

			final RConsole goblin = new RConsole();
			assertFalse(consoleController.isAttached(goblin));
		}

		/* test append(...) */
		consoleController.append("Hello World!", 0);
		Thread.sleep(10); // append is executed in separate EDT thread

		final StyledDocument doc = console.getStyledDocument();
		assertEquals("Appending to the console failed.", "Hello World!", console.getText());
		assertEquals("Appending to the console document failed.", "Hello World!", doc.getText(0, doc.getLength()));

		/*
		 * check the clear action. It doesn't check the action event, so passing
		 * null as ActionEvent is okay.
		 */
		assertNotNull(consoleController.getClearAction());
		consoleController.getClearAction().actionPerformed(null);
		Thread.sleep(10); // clear is executed in separate EDT thread
		assertEquals("Clearing the console failed.", "", console.getText());
		// doc is not cleared! TODO: Is this intentional?

		/* test detach(...) */
		consoleController.detach(console);
		assertFalse(consoleController.isAttached(console));
	}
}
