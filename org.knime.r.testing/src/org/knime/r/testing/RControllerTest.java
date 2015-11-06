package org.knime.r.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.r.RController;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;

/**
 * Test for {@link RController}.
 * 
 * @author Jonathan Hale
 */
public class RControllerTest {

	private static RController m_controller;

	@BeforeClass
	public static void beforeClass() {
		m_controller = new RController();
		assertNotNull(m_controller);
	}

	@AfterClass
	public static void afterClass() {
		m_controller.close();
		// terminate the R process used by the controller, otherwise it will be
		// leaked.
		m_controller.terminateRProcess();
	}

	/**
	 * Test {@link RController#create()} and {@link RController#getREngine()}
	 * return value.
	 */
	@Test
	public void testInitialization() {
		/* Check underlying RConnection */
		RConnection rEngine = (RConnection) m_controller.getREngine();
		assertNotNull("No RConnection exists, most likely cause is that R or Rserve are not installed or found.",
				rEngine);
		// this assertion is theoretically covered by the
		// RConnectionFactoryTest, but you never know what could go wrong.
		assertTrue("Could not connect to Rserve.", rEngine.isConnected());
	}

	/**
	 * Test {@link RController#eval(String)}.
	 */
	@Test
	public void testEvaluation() {

		// similar to evaluation test in RConnectionFactoryTest
		try {
			REXP exp = m_controller.eval("w <- 10; w");

			assertNotNull(exp);
			assertEquals("Returned integer had incorrect value.", 10, exp.asInteger());
		} catch (REXPMismatchException e) {
			fail("Simple evaluation did not return Integer.");
		} catch (REngineException e) {
			fail("Simple expression evaluation failed.");
		}
	}

	/**
	 * Test {@link RController#monitoredEval(String, ExecutionMonitor)}/
	 * {@link RController#monitoredEval(String, ExecutionMonitor, boolean)} and
	 * wether they can be correctly cancelled.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testMonitoredEvaluation() throws InterruptedException {
		Thread t = null;
		try {
			// test if monitored evaluation can be correctly cancelled.
			final DefaultNodeProgressMonitor progress = new DefaultNodeProgressMonitor();
			final ExecutionMonitor exec = new ExecutionMonitor(progress);
			t = new Thread(() -> {
				try {
					m_controller.monitoredEval("Sys.sleep(20)", exec, false);
				} catch (final CanceledExecutionException e) {
					// this is expeced.
				} catch (final Throwable thr) {
					fail("Caught something else than a CanceledExecutionException: " + thr.getClass().getName() + ": "
							+ thr.getMessage());
				}
			});
			t.start();
			// make sure the evaluation can be started on the server.
			progress.setExecuteCanceled();
			// give some time to cancel the evaluation. Cancellation is checked
			// every 200ms.
			t.join(5000); // will return before the 5 seconds are over, if the test succeeds
			assertFalse(t.isAlive());

			// test that evaluation still works (Rserver has been restarted)
			testEvaluation();
		} finally {
			if (t != null && t.isAlive()) {
				// forcefully stop process to not leak it. Probably not enough,
				// since other threads may have been opened by this thread.
				// Only happens on unexpected exceptions, though.
				t.stop();
			}
		}

		/* try running a monitored evaluation which terminates */
		try {
			// test if monitored evaluation can be correctly cancelled.
			final DefaultNodeProgressMonitor progress = new DefaultNodeProgressMonitor();
			final ExecutionMonitor exec = new ExecutionMonitor(progress);
			t = new Thread(() -> {
				try {
					REXP exp;
					exp = m_controller.monitoredEval("y <- 12; y", exec, false);

					assertNotNull(exp);
					assertEquals("Returned integer had incorrect value.", 12, exp.asInteger());
				} catch (REXPMismatchException e) {
					fail("Simple evaluation did not return Integer.");
				} catch (final Throwable thr) {
					fail("Caught something else than a CanceledExecutionException: " + thr.getClass().getName() + ": "
							+ thr.getMessage());
				}
			});
			t.start();
			// give some time to cancel the evaluation. Cancellation is checked
			// every 200ms.
			t.join(1000); // Warning: Test may fail evaluation takes longer
			assertFalse(t.isAlive());
		} finally {
			if (t != null && t.isAlive()) {
				// forcefully stop process to not leak it. Probably not enough,
				// since other threads may have been opened by this thread.
				// Only happens on unexpected exceptions, though.
				t.stop();
			}
		}
	}
}
