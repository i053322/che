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
package org.eclipse.che.api.user.server.jpa;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.spi.PreferenceDao;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link PreferenceDao}.
 *
 * @author Anton Korneta
 */
public class JpaPreferenceDao implements PreferenceDao {

    @Inject
    private EntityManagerFactory factory;

    @Override
    public void setPreferences(String userId, Map<String, String> preferences) throws ServerException {
        requireNonNull(userId);
        requireNonNull(preferences);
        final PreferenceEntity prefs = new PreferenceEntity(userId, preferences);
        if (preferences.isEmpty()) {
            remove(userId);
        } else {
            final EntityManager manager = factory.createEntityManager();
            try {
                final PreferenceEntity existing = manager.find(PreferenceEntity.class, userId);
                manager.getTransaction().begin();
                if (existing != null) {
                    manager.merge(prefs);
                } else {
                    manager.persist(prefs);
                }
                manager.getTransaction().commit();
            } catch (RuntimeException ex) {
                throw new ServerException(ex.getLocalizedMessage(), ex);
            } finally {
                if (manager.getTransaction().isActive()) {
                    manager.getTransaction().rollback();
                }
                manager.close();
            }
        }
    }

    @Override
    public Map<String, String> getPreferences(String userId) throws ServerException {
        final EntityManager manager = factory.createEntityManager();
        try {
            final PreferenceEntity prefs = manager.find(PreferenceEntity.class, userId);
            return prefs == null ? new HashMap<>()
                                 : prefs.getPreferences();
        } catch (RuntimeException ex) {
            throw new ServerException(ex.getLocalizedMessage(), ex);
        } finally {
            manager.close();
        }
    }

    @Override
    public Map<String, String> getPreferences(String userId, String filter) throws ServerException {
        final EntityManager manager = factory.createEntityManager();
        try {
            final PreferenceEntity prefs = manager.find(PreferenceEntity.class, userId);
            if (prefs == null) {
                return new HashMap<>();
            }
            final Map<String, String> preferences = prefs.getPreferences();
            if (filter != null && !filter.isEmpty()) {
                final Map<String, String> filtered = new HashMap<>();
                final Pattern pattern = Pattern.compile(filter);
                for (Map.Entry<String, String> entry : preferences.entrySet()) {
                    if (pattern.matcher(entry.getKey()).matches()) {
                        filtered.put(entry.getKey(), entry.getValue());
                    }
                }
                return filtered;
            } else {
                return preferences;
            }
        } catch (RuntimeException ex) {
            throw new ServerException(ex.getLocalizedMessage(), ex);
        } finally {
            manager.close();
        }
    }

    @Override
    public void remove(String userId) throws ServerException {
        final EntityManager manager = factory.createEntityManager();
        try {
            final PreferenceEntity prefs = manager.find(PreferenceEntity.class, userId);
            if (prefs != null) {
                manager.getTransaction().begin();
                manager.remove(prefs);
                manager.getTransaction().commit();
            }
        } catch (RuntimeException ex) {
            throw new ServerException(ex);
        } finally {
            if (manager.getTransaction().isActive()) {
                manager.getTransaction().rollback();
            }
            manager.close();
        }
    }
}
