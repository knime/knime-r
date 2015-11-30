package org.knime.r.testing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RController;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;

/**
 * Test for {@link RController}.
 * 
 * @author Jonathan Hale
 */
public class RControllerTest {

	private RController m_controller;

	@Before
	public void before() throws RException {
		m_controller = new RController();
		assertNotNull(m_controller);
	}

	@After
	public void after() {
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
		} catch (RException e) {
			fail("Simple expression evaluation failed.");
		}
	}

	/**
	 * Test {@link RController#monitoredEval(String, ExecutionMonitor)}/
	 * {@link RController#monitoredEval(String, ExecutionMonitor, boolean)} and
	 * whether they can be correctly cancelled.
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
					m_controller.monitoredEval("Sys.sleep(20)", exec);
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
			t.join(5000); // will return before the 5 seconds are over, if the
							// test succeeds
			assertFalse(t.isAlive());

			// test that evaluation still works (Rserver has been restarted)
			testEvaluation();
		} finally {
			if (t != null && t.isAlive()) {
				t.interrupt();
			}

			assertFalse(t.isAlive());
		}

		/* try running a monitored evaluation which terminates */
		try {
			// test if monitored evaluation can be correctly cancelled.
			final DefaultNodeProgressMonitor progress = new DefaultNodeProgressMonitor();
			final ExecutionMonitor exec = new ExecutionMonitor(progress);
			t = new Thread(() -> {
				try {
					REXP exp;
					exp = m_controller.monitoredEval("y <- 12; y", exec);

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
				t.interrupt();
			}

			assertFalse(t.isAlive());
		}
	}
}
