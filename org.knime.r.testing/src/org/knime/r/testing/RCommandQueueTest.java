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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RCommandQueue;
import org.knime.r.controller.RConsoleController;
import org.knime.r.controller.RController;
import org.knime.r.ui.RConsole;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.protocol.RTalk;

/**
 * Test for {@link RCommandQueue}.
 *
 * @author Jonathan Hale
 */
public class RCommandQueueTest {

	private RController m_controller;
	private RCommandQueue m_commandQueue;
	private RConsoleController m_consoleController;

	@Before
	public void before() throws RException {
		m_controller = new RController();
		m_commandQueue = new RCommandQueue(m_controller);
		m_consoleController = new RConsoleController(m_controller, m_commandQueue);

		assertNotNull(m_controller);
	}

	@After
	public void after() {
		if (m_controller == null) {
			return;
		}

		// terminate the R process used by the controller, otherwise it will be
		// leaked.
		try {
			m_controller.close();
		} catch (RException e) {
			fail(e.getMessage());
		} finally {
			m_controller.terminateRProcess();
		}
	}

	/**
	 * Test for whether the RController creates a valid
	 * {@link RConsoleController} and {@link RCommandQueue} and correctly
	 * connects to a Rserve server.
	 */
	@Test
	public void testConnection() {
		assertNotNull(m_controller.getREngine());
		assertTrue(m_controller.getREngine().isConnected());
	}

	/**
	 * Test for
	 * <ul>
	 * <li>{@link RCommandQueue#startExecutionThread(RController)}</li>
	 * <li>{@link RCommandQueue#stopExecutionThread()}</li>
	 * <li>Execution of commands in the command queue</li>
	 * <li>Handling of foul commands and other failure states.</li>
	 * </ul>
	 *
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Test
	public void consoleExecution() throws InterruptedException, ExecutionException, TimeoutException {
		final RCommandQueue queue = new RCommandQueue(m_controller);

		final RConsole console = new RConsole();
		final RConsoleController consoleController = new RConsoleController(m_controller, queue);
		consoleController.attachOutput(console);

		/* check startReadConsoleThread() */
		try {
			queue.startExecutionThread();
			queue.startExecutionThread();
			fail("Expected a IllegalStateException for starting the already running thread");
		} catch (IllegalStateException e) {
		}

		/* check that the console read thread survives bad commands */
		queue.putRScript("s<?hello.y!.�a", true).get(1, TimeUnit.SECONDS);

		// wait for console to update
		Thread.sleep(10);

		assertTrue("Execution thread did not survive evaluation of garbage.", queue.isExecutionThreadRunning());

		// clear console for next check.
		consoleController.clear();
		assertTrue("Clearing the console failed.", console.getText().isEmpty());

		/* check multiline/valid R code execution, and it's output */
		queue.putRScript("print(\"Hello World!\")\nprint(\"Hello World, again!\")", true).get(1, TimeUnit.SECONDS);
		// wait for console to update
		Thread.sleep(10);

		assertEquals("Incorrect output for print(\"Hello World!\")",
				// use String.format for platform dependent newlines
				String.format("> print(\"Hello World!\")%n" //
						+ "+ print(\"Hello World, again!\")%n" //
						+ "[1] \"Hello World!\"%n" //
						+ "[1] \"Hello World, again!\"%n"),
				console.getText());

		assertTrue("Execution thread did not survive evaluation of valid R code.", queue.isExecutionThreadRunning());

		/* check if any temporary output capturing variables have leaked */
		try {
			REXP objects = m_controller.eval("objects()", true);

			if (objects != null && objects.isString()) {
				assertArrayEquals("Temporary objects leaked." + Arrays.toString(objects.asStrings()),
						objects.asStrings(), new String[] {});
			} else {
				fail("Expected different return value for evaluation of \"objects()\"");
			}
		} catch (RException | REXPMismatchException e) {
			e.printStackTrace();
			fail("Evaluation of \"objects()\" failed.");
		}

		// clear console for next check.
		consoleController.clear();

		/* make sure values are printed */

		// clear console for next check.
		consoleController.clear();

		queue.putRScript("42", true).get(1, TimeUnit.SECONDS);
		// wait for console to update
		Thread.sleep(10);

		assertEquals("Values are not printed correctly.",
				// use String.format for platform dependent newlines
				String.format("> 42%n" //
						+ "[1] 42%n"),
				console.getText());

		// clear console for next check.
		consoleController.clear();

		queue.stopExecutionThread();
	}

	/**
	 * Test for a bug which caused multiple commands executed sequentially to
	 * result in a infinitely blocked SocketInputStream inside {@link RTalk}.
	 *
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Test
	public void testConcurrency() throws InterruptedException, ExecutionException, TimeoutException {
		final RCommandQueue queue = m_commandQueue;
		final RConsole console = new RConsole();

		final RConsoleController consoleController = m_consoleController;
		consoleController.attachOutput(console);
		queue.startExecutionThread();

		queue.putRScript("knime.in <- data.frame(c(1, 2, 3, 4))", true);
		final Thread t = new Thread(() -> {
			try {
				queue.putRScript("knime.in <- data.frame(c(1, 2, 3, 4))\nprint(knime.in)", true);

				final String tempDevOffOutputVar = "temp123";
				queue.putRScript(tempDevOffOutputVar + "<- dev.off()\nrm(" + tempDevOffOutputVar + ")", false).get();
			} catch (Exception e) {
			}
		}, "testConcurrency - Command Executor");
		t.start();

		queue.putRScript("print(\"Hey!\")", true).get(10, TimeUnit.SECONDS); // should never take that long
		t.join(10000); // should also never take that long

		consoleController.clear();
		assertTrue(queue.isEmpty());

		assertTrue("Execution thread did not survive evaluation of multiple commands.",
				queue.isExecutionThreadRunning());

		queue.stopExecutionThread();

		consoleController.detach(console);
	}

	/**
	 * Test for a bug which caused the R Console Execution thread to block until
	 * the current R script has finished executing.
	 *
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 */
	@Test
	public void testShutdown() throws InterruptedException, ExecutionException, TimeoutException {
		final RCommandQueue queue = m_commandQueue;

		queue.startExecutionThread();
		queue.putRScript("Sys.sleep(1000)", true);
		Thread.sleep(250);
		queue.stopExecutionThread();

		assertTrue("R Execution thread did not terminate.", !queue.isExecutionThreadRunning());
	}

	/**
	 * Test whether the RCommandQueue handles invalid R code correctly.
	 *
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws TimeoutException
	 * @throws REXPMismatchException
	 */
	@Test
	public void testHandleInvalidR()
			throws InterruptedException, ExecutionException, TimeoutException, REXPMismatchException {
		final RCommandQueue queue = m_commandQueue;
		final RConsole console = new RConsole();

		final RConsoleController consoleController = m_consoleController;
		consoleController.attachOutput(console);

		queue.startExecutionThread();
		assertTrue(queue.isExecutionThreadRunning());
		queue.putRScript("print(\"sanity check\")", true).get(1, TimeUnit.SECONDS);
		Thread.sleep(10); // wait for console update

		assertEquals("Sanity check for whether execution test is running failed.",
				String.format("> print(\"sanity check\")%n[1] \"sanity check\"%n"), console.getText());
		consoleController.clear();

		/*
		 * Errors are language dependent, so we set the language to English to
		 * be able to assertEquals on the output. Not saved, so no need to
		 * reset.
		 * LANGUAGE environment variable has precedence over the LC_MESSAGES locale
		 */
		final REXP ret = queue.putRScript("Sys.setenv(LANGUAGE='en')", false).get(500, TimeUnit.MILLISECONDS);
		assertTrue("Failed to set language of R errors.", ret.isLogical() && ret.asBytes()[0] == REXPLogical.TRUE);

		// Name not found
		queue.putRScript("IdoNotExistVar", true).get(1, TimeUnit.SECONDS);
		Thread.sleep(10); // wait for console update
		assertEquals("Expected and name not found error.",
				String.format("> IdoNotExistVar%nError: object 'IdoNotExistVar' not found%n"), console.getText());
		consoleController.clear();

		// Bad syntax may lead to bad behavior
		queue.putRScript("} print(\"Hello\")", true).get(1, TimeUnit.SECONDS);
		Thread.sleep(10); // wait for console update
		assertEquals("Syntactically incorrect R code should print syntax error messages.",
				String.format( //
						"> } print(\"Hello\")%n" + //
						"Error: <text>:1:1: unexpected '}'%n" + //
						"1: }%n" + //
						"    ^%n"),
				console.getText());
		consoleController.clear();

		queue.putRScript(") print(\"Hello\")", true).get(1, TimeUnit.SECONDS);
		Thread.sleep(10); // wait for console update
		assertEquals("Syntactically incorrect R code should print syntax error messages.",
				String.format( //
						"> ) print(\"Hello\")%n" + //
						"Error: <text>:1:1: unexpected ')'%n" + //
						"1: )%n" + //
						"    ^%n"),
				console.getText());
		consoleController.clear();

		queue.putRScript("\" print(\"Hello\")", true).get(1, TimeUnit.SECONDS);
		Thread.sleep(10); // wait for console update
		assertEquals("Syntactically incorrect R code should print syntax error messages.",
				String.format( //
						"> \" print(\"Hello\")%n" + //
						"Error: <text>:1:10: unexpected symbol%n" + //
						"1: \" print(\"Hello%n" + //
						"             ^%n"),
				console.getText());

		queue.stopExecutionThread();

		consoleController.detach(console);
	}
}
