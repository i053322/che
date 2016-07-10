/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.user.server.spi.tck;

import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.user.server.spi.PreferenceDao;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.test.tck.TckModuleFactory;
import org.eclipse.che.commons.test.tck.repository.TckRepository;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Anton Korneta
 */
@Guice(moduleFactory = TckModuleFactory.class)
@Test(suiteName = PreferenceDaoTest.SUITE_NAME)
public class PreferenceDaoTest {

    public static final String SUITE_NAME = "PreferenceDaoTck";

    private static final int ENTRY_COUNT = 5;

    private List<Pair<String, Map<String, String>>> userPreferences;

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private TckRepository<Pair<String, Map<String, String>>> tckRepository;

    @BeforeMethod
    private void setUp() throws Exception {
        userPreferences = new ArrayList<>(ENTRY_COUNT);
        for (int index = 0; index < ENTRY_COUNT; index++) {
            final Map<String, String> prefs = new HashMap<>();
            prefs.put("preference1", "val");
            prefs.put("preference2", "val");
            prefs.put("preference3", "val");
            userPreferences.add(Pair.of("userId_" + index, prefs));
        }
        tckRepository.createAll(userPreferences);
    }

    @AfterMethod
    private void cleanUp() throws Exception {
        tckRepository.removeAll();
    }

    @Test
    public void shouldSetUserPreferences() throws Exception {
        final String userId = userPreferences.get(0).first;
        final Map<String, String> prefs = ImmutableMap.of("key", "value");
        preferenceDao.remove(userId);
        preferenceDao.setPreferences(userId, prefs);

        assertEquals(preferenceDao.getPreferences(userId), prefs);
    }

    @Test
    public void shouldOverrideUserPreferences() throws Exception {
        final String userId = userPreferences.get(0).first;
        final Map<String, String> update = ImmutableMap.of("key", "value");
        preferenceDao.setPreferences(userId, update);

        assertEquals(preferenceDao.getPreferences(userId), update);
    }

    @Test
    public void shouldRemoveUserPreferencesWhenUpdateIsEmpty() throws Exception {
        final String userId = userPreferences.get(0).first;
        final Map<String, String> update = Collections.emptyMap();
        preferenceDao.setPreferences(userId, update);

        assertEquals(preferenceDao.getPreferences(userId), update);
    }

    @Test
    public void qwe3() {

    }

    @Test
    public void qwe4() {

    }

    @Test
    public void qwe5() {

    }

    @Test
    public void qwe6() {

    }
}
