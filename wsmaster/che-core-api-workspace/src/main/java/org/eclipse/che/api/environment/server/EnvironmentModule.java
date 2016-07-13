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
package org.eclipse.che.api.environment.server;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

/**
 * @author Alexander Garagatyi
 */
public class EnvironmentModule extends AbstractModule {
    @Override
    protected void configure() {
        MapBinder<String, org.eclipse.che.api.environment.server.spi.EnvironmentEngine> engines =
                MapBinder.newMapBinder(binder(), String.class, org.eclipse.che.api.environment.server.spi.EnvironmentEngine.class);
        engines.addBinding(org.eclipse.che.api.environment.impl.che.CheEnvironmentEngine.ENVIRONMENT_TYPE)
               .to(org.eclipse.che.api.environment.impl.che.CheEnvironmentEngine.class);

        MapBinder<String, org.eclipse.che.api.environment.server.spi.EnvironmentValidator> validators =
                MapBinder.newMapBinder(binder(), String.class, org.eclipse.che.api.environment.server.spi.EnvironmentValidator.class);
        validators.addBinding(org.eclipse.che.api.environment.impl.che.CheEnvironmentEngine.ENVIRONMENT_TYPE)
                  .to(org.eclipse.che.api.environment.impl.che.CheEnvironmentValidator.class);
    }
}
