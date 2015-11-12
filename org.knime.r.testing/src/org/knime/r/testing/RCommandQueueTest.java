package org.knime.r.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
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
		// terminate the R process used by the controller, otherwise it will be
		// leaked.
		m_controller.terminateRProcess();
		m_controller.close();
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

		/* check execution of a simple command, and it's output */
		queue.putRScript("print(\"Hello World!\")", true).get(1, TimeUnit.SECONDS);

		// wait for console to update
		Thread.sleep(40);

		assertEquals("Incorrect output for print(\"Hello World!\")",
				// use String.format for platform dependent newlines
				String.format("> print(\"Hello World!\")%n" //
						+ "[1] \"Hello World!\"%n"),
				console.getText());

		assertTrue("Execution thread did not survive evaluation of valid R code.", queue.isExecutionThreadRunning());
		
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
		
		/* multi line execution */
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
		} , "testConcurrency - Command Executor");
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
}