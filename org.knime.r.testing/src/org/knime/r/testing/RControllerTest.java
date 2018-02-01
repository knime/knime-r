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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.r.controller.IRController.RControllerNotInitializedException;
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
	public void testInitialization() throws RException {
		/* Check underlying RConnection */
		RConnection rEngine = m_controller.getREngine();
		assertNotNull("No RConnection exists, most likely cause is that R or Rserve are not installed or found.",
				rEngine);
		// this assertion is theoretically covered by the
		// RConnectionFactoryTest, but you never know what could go wrong.
		assertTrue("Could not connect to Rserve.", rEngine.isConnected());

		RController uninitialized = new RController(false);
		assertFalse(uninitialized.isInitialized());

		try {
			uninitialized.eval("42", false);
			fail("Expected a RControllerNotInitializedException.");
		} catch (RControllerNotInitializedException e) {
			// Should throw.
		} finally {
			uninitialized.close();
		}
	}

	/**
	 * Test {@link RController#eval(String)}.
	 */
	@Test
	public void testEvaluation() {

		// similar to evaluation test in RConnectionFactoryTest
		try {
			REXP exp = m_controller.eval("w <- 10; w", true);

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
					exp = m_controller.monitoredEval("y <- 12; y", exec, true);

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
			t.join(1000); // Warning: Test may fail if evaluation takes longer on other machines
			assertFalse(t.isAlive());
		} finally {
			if (t != null && t.isAlive()) {
				t.interrupt();
			}

			assertFalse(t.isAlive());
		}
	}
}
