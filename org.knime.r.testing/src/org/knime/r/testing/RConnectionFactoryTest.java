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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.knime.r.controller.IRController.RException;
import org.knime.r.rserve.RConnectionFactory;
import org.knime.r.rserve.RConnectionFactory.RConnectionResource;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class RConnectionFactoryTest {

	// connection is initialized in all tests
	private RConnection m_connection;

	/**
	 * Test result of {@link RConnectionFactory#createConnection()}, if the
	 * underlying Rserve server was started correctly.
	 *
	 * @throws RserveException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testConnection() throws InterruptedException, RserveException, IOException {
		RConnectionResource resource = null;
		try {
			resource = RConnectionFactory.createConnection();
			assertNotNull(resource);

			m_connection = resource.get();
			// connection was created in before()
			assertNotNull(m_connection);
			assertTrue(m_connection.isConnected());

			// simple test to make sure the server is actually responding
			// correctly
			try {
				REXP exp = m_connection.eval("x<-10;x");

				assertNotNull(exp);
				assertEquals("Returned integer had incorrect value.", 10, exp.asInteger());
			} catch (RserveException e) {
				fail("Simple expression evalutation failed.");
			} catch (REXPMismatchException e) {
				fail("Simple evaluation did not return Integer.");
			}

			// test to make sure the server is running with the correct
			// environment
			try {
				// R.Version() is part of the base package in R, which
				// should have been loaded by default
				REXP exp = m_connection.eval("R.Version()");

				// We expect some return value.
				assertNotNull(exp);
			} catch (RserveException e) {
				fail("\"R.Version\" failed. This suggests R_HOME is not set for the Rserve process.");
			}
		} finally {
			// Rserve process may not have been terminated in the test, make
			// sure it is.
			if (resource != null) {
				resource.destroy(true);
			}
		}
	}

	/**
	 * Test whether the Rserve process created for a connection can be correctly
	 * terminated.
	 *
	 * <p>
	 * <b>IMPORTANT:</b> When this test fails, one or more Rserve processes have
	 * been leaked in this <b>and/or</b> in one of the other tests. The Rserve
	 * processes have to be terminated manually and the org.knime.r tests should
	 * be deactivated until this is fixed.
	 * </p>
	 *
	 * @throws RserveException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testRserveTermination() throws InterruptedException, RserveException, IOException {
		final RConnectionResource resource = RConnectionFactory.createConnection();
		m_connection = resource.get();
		resource.destroy(true);
		Thread.sleep(100); // give the OS some time to react

		final Collection<Process> runningRProcesses = RConnectionFactory.getRunningRProcesses();

		assertTrue("FATAL, the R process is listed running but it has been destroyed", runningRProcesses.isEmpty());
		assertFalse("Connection has not been closed on resource destruction", m_connection.isConnected());
	}

	/**
	 * Test whether the Rserve process created for a connection creates a new
	 * workspace, therefore no variables are leaked.
	 *
	 * @throws RserveException
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws REXPMismatchException
	 * @throws RException
	 */
	@Test
	public void ensureNoWorkspaceLeaking()
			throws InterruptedException, RserveException, IOException, REXPMismatchException, RException {
		RConnectionResource resource = RConnectionFactory.createConnection();
		m_connection = resource.get();
		m_connection.eval("x<-42");
		resource.release();

		// reacquire resource
		resource = RConnectionFactory.createConnection();
		m_connection = resource.get();

		REXP result = m_connection.eval("objects()");
		assertArrayEquals("Workspace objects leaked! " + Arrays.toString(result.asStrings()), result.asStrings(),
				new String[] {});

		resource.destroy(true);
	}

}
