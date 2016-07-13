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
package org.eclipse.che.api.environment.impl.che;

import com.google.common.base.Joiner;

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.model.machine.ServerConf;
import org.eclipse.che.api.core.model.workspace.Environment;
import org.eclipse.che.api.environment.server.spi.EnvironmentValidator;
import org.eclipse.che.api.machine.server.MachineInstanceProviders;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

/**
 * @author Alexander Garagatyi
 */
public class CheEnvironmentValidator implements EnvironmentValidator {
    private static final Pattern SERVER_PORT     = Pattern.compile("[1-9]+[0-9]*/(?:tcp|udp)");
    private static final Pattern SERVER_PROTOCOL = Pattern.compile("[a-z][a-z0-9-+.]*");

    private final MachineInstanceProviders machineInstanceProviders;

    @Inject
    public CheEnvironmentValidator(MachineInstanceProviders machineInstanceProviders) {
        this.machineInstanceProviders = machineInstanceProviders;
    }

    @Override
    public String getType() {
        return CheEnvironmentEngine.ENVIRONMENT_TYPE;
    }

    // todo validate depends on in the same way as machine name
    // todo validate that env contains machine with name equal to dependency
    // todo use strategy to check if order is valid, there is no cyclic dependency
    // todo should throw another exception in case it is not possible to download recipe
    @Override
    public void validate(Environment env) throws IllegalArgumentException {
        List<MachineConfigImpl> machines = env.getMachineConfigs()
                                              .stream()
                                              .map(MachineConfigImpl::new)
                                              .collect(Collectors.toList());

        checkArgument(!machines.isEmpty(), "Environment should contain at least 1 machine");

        final long devCount = machines.stream()
                                      .filter(MachineConfig::isDev)
                                      .count();
        checkArgument(devCount == 1,
                      "Environment should contain exactly 1 dev machine, but contains '%d'",
                      devCount);
        for (MachineConfig machineCfg : machines) {
            validateMachine(machineCfg);
        }
    }

    private void validateMachine(MachineConfig machineCfg) throws IllegalArgumentException {
        checkArgument(!isNullOrEmpty(machineCfg.getName()), "Environment contains machine with null or empty name");
        checkNotNull(machineCfg.getSource(), "Environment contains machine without source");
        checkArgument(!(machineCfg.getSource().getContent() == null && machineCfg.getSource().getLocation() == null),
                      "Environment contains machine with source but this source doesn't define a location or content");
        checkArgument(machineInstanceProviders.hasProvider(machineCfg.getType()),
                      "Type %s of machine %s is not supported. Supported values: %s.",
                      machineCfg.getType(),
                      machineCfg.getName(),
                      Joiner.on(", ").join(machineInstanceProviders.getProviderTypes()));

        for (ServerConf serverConf : machineCfg.getServers()) {
            checkArgument(serverConf.getPort() != null && SERVER_PORT.matcher(serverConf.getPort()).matches(),
                          "Machine %s contains server conf with invalid port %s",
                          machineCfg.getName(),
                          serverConf.getPort());
            checkArgument(serverConf.getProtocol() == null || SERVER_PROTOCOL.matcher(serverConf.getProtocol()).matches(),
                          "Machine %s contains server conf with invalid protocol %s",
                          machineCfg.getName(),
                          serverConf.getProtocol());
        }
        for (Map.Entry<String, String> envVariable : machineCfg.getEnvVariables().entrySet()) {
            checkArgument(!isNullOrEmpty(envVariable.getKey()), "Machine %s contains environment variable with null or empty name");
            checkNotNull(envVariable.getValue(), "Machine %s contains environment variable with null value");
        }
    }

    /**
     * Checks that object reference is not null, throws {@link IllegalArgumentException}
     * in the case of null {@code object} with given {@code message}.
     */
    private static void checkNotNull(Object object, String message) throws IllegalArgumentException {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Checks that expression is true, throws {@link IllegalArgumentException} otherwise.
     *
     * <p>Exception uses error message built from error message template and error message parameters.
     */
    private static void checkArgument(boolean expression, String errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Checks that expression is true, throws {@link IllegalArgumentException} otherwise.
     *
     * <p>Exception uses error message built from error message template and error message parameters.
     */
    private static void checkArgument(boolean expression, String errorMessageTemplate, Object... errorMessageParams)
            throws IllegalArgumentException {
        if (!expression) {
            throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageParams));
        }
    }
}
