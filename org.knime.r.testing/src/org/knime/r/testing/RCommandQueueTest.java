package org.knime.r.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.r.RCommandQueue;
import org.knime.r.RConsoleController;
import org.knime.r.RController;
import org.knime.r.ui.RConsole;
import org.rosuda.REngine.Rserve.protocol.RTalk;

/**
 * Test for {@link RCommandQueue}.
 * 
 * @author Jonathan Hale
 */
public class RCommandQueueTest {

	private RController m_controller;
	
	@Before
	public void before() {
		m_controller = new RController();
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
	public void testCreation() {
		final RConsoleController consoleController = m_controller.getConsoleController();
		assertNotNull(consoleController);
		assertNotNull(m_controller.getCommandQueue());

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
	 */
	@Test
	public void consoleExecution() throws InterruptedException, ExecutionException {
		final RCommandQueue queue = m_controller.getCommandQueue();

		final RConsole console = new RConsole();
		final RConsoleController consoleController = m_controller.getConsoleController();
		consoleController.attachOutput(console);

		/* check startReadConsoleThread() */
		try {
			queue.startExecutionThread();
			queue.startExecutionThread();
			fail("Expected a IllegalStateException for starting the already running thread");
		} catch (IllegalStateException e) {
		}

		/* check that the console read thread survives bad commands */
		queue.putRScript("<?hello.y!.üa", true).get();

		assertTrue("Execution thread did not survive evaluation of garbage.", queue.isExecutionThreadRunning());

		// clear console for next check.
		consoleController.clear();
		assertTrue("Clearing the console failed.", console.getText().isEmpty());

		/* check execution of a simple command, and it's output */
		m_controller.getCommandQueue().putRScript("print(\"Hello World!\")", true).get();

		// wait for console to update
		Thread.sleep(10);

		assertEquals("Incorrect output for print(\"Hello World!\")",
				// use String.format for platform dependent newlines
				String.format("> print(\"Hello World!\")%n" //
						+ "[1] \"Hello World!\"%n"),
				console.getText());

		assertTrue("Execution thread did not survive evaluation of valid R code.", queue.isExecutionThreadRunning());

		queue.stopExecutionThread();
	}

	/**
	 * Test for a bug which caused multiple commands executed sequentially to
	 * result in a infinitely blocked SocketInputStream inside {@link RTalk}.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test(timeout = 5000L)
	public void testConcurrency() throws InterruptedException, ExecutionException {
		final RCommandQueue queue = m_controller.getCommandQueue();
		final RConsole console = new RConsole();

		final RConsoleController consoleController = m_controller.getConsoleController();
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

		queue.putRScript("print(\"Hey!\")", true);
		final String tempDevOffOutputVar2 = "temp321";
		queue.putRScript(tempDevOffOutputVar2 + "<- dev.off()\nrm(" + tempDevOffOutputVar2 + ")", false).get();
		t.join();

		consoleController.clear();
		assertTrue(queue.isEmpty());

		assertTrue("Execution thread did not survive evaluation of multiple commands.",
				queue.isExecutionThreadRunning());

		queue.stopExecutionThread();

		consoleController.detach(console);

	}
}
