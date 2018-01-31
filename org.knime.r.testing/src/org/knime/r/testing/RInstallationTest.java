package org.knime.r.testing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import javax.sound.midi.ControllerEventListener;

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
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.protocol.RTalk;

/**
 * Test which checks the current R installation.
 * 
 * @author Jonathan Hale
 */
public class RInstallationTest {

	private RController m_controller;

	@Before
	public void before() throws RException {
		m_controller = new RController();
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

	@Test
	public void checkVersion() throws REXPMismatchException, RException {
		final REXP versionRexp = m_controller.eval("R.version", true);
		final RList version = versionRexp.asList();

		final String versionString = ((REXP) version.get("version.string")).asString();

		final String versionMajor = ((REXP) version.get("major")).asString();
		assertTrue("Expected R major version to be at least 3, but was " + versionString,
				Float.parseFloat(versionMajor) >= 3.0);
	}

	@Test
	public void checkImportantPackages() throws REXPMismatchException, RException {
		final ArrayList<String> packages = new ArrayList<>(Arrays.asList("Rserve", "ggplot2", "data.table"));

		final REXP isInstalled = m_controller
				.eval("c(" + (String.join(",", packages.stream().map(s -> "'" + s + "'").collect(Collectors.toList())))
						+ ") %in% installed.packages()", true);

		assertTrue("Bad test: R code should have returned vector of logicals.", isInstalled.isLogical());
		final byte[] installed = isInstalled.asBytes();

		final ArrayList<String> missingPackages = new ArrayList<>();
		for (int i = 0; i < packages.size(); ++i) {
			if (installed[i] != REXPLogical.TRUE) {
				missingPackages.add(packages.get(i));
			}
		}

		assertTrue("R binaries are missing some important packages: " + String.join(", ", missingPackages),
				missingPackages.isEmpty());
	}
}
