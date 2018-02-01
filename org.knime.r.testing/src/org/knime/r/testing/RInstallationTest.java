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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.r.controller.IRController.RException;
import org.knime.r.controller.RController;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.RList;

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
