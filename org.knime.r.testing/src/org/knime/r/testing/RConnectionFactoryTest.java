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
		Thread.sleep(50); // give the OS some time to react

		final Collection<Process> runningRProcesses = RConnectionFactory.getRunningRProcesses();

		assertTrue("FATAL, please check the code for more information.", runningRProcesses.isEmpty());
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
