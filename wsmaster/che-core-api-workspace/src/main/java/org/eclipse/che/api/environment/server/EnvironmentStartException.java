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

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;

/**
 * @author Alexander Garagatyi
 */
public class EnvironmentStartException extends ServerException {
    public EnvironmentStartException(String message) {
        super(message);
    }

    public EnvironmentStartException(ServiceError serviceError) {
        super(serviceError);
    }

    public EnvironmentStartException(Throwable cause) {
        super(cause);
    }

    public EnvironmentStartException(String message, Throwable cause) {
        super(message, cause);
    }
}
