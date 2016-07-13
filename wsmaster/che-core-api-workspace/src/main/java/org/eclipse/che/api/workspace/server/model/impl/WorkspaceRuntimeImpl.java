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
package org.eclipse.che.api.workspace.server.model.impl;

import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.model.workspace.WorkspaceRuntime;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

/**
 * Data object for {@link WorkspaceRuntime}.
 *
 * @author Yevhenii Voevodin
 */
public class WorkspaceRuntimeImpl implements WorkspaceRuntime {

    private final String activeEnv;
    private final String envType;

    private String            rootFolder;
    private MachineImpl       devMachine;
    private List<MachineImpl> machines;

    public WorkspaceRuntimeImpl(String activeEnv, String envType) {
        this.activeEnv = activeEnv;
        this.envType = envType;
    }

    public WorkspaceRuntimeImpl(String activeEnv,
                                String rootFolder,
                                Collection<? extends Machine> machines,
                                Machine devMachine,
                                String envType) {
        this.activeEnv = activeEnv;
        this.rootFolder = rootFolder;
        this.envType = envType;
        if (devMachine != null) {
            this.devMachine = new MachineImpl(devMachine);
        }
        this.machines = machines.stream()
                                .map(MachineImpl::new)
                                .collect(toList());
    }

    public WorkspaceRuntimeImpl(WorkspaceRuntime runtime) {
        this(runtime.getActiveEnv(),
             runtime.getRootFolder(),
             runtime.getMachines(),
             runtime.getDevMachine(),
             runtime.getEnvType());
    }

    @Override
    public String getActiveEnv() {
        return activeEnv;
    }

    @Override
    public String getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    @Override
    public MachineImpl getDevMachine() {
        return devMachine;
    }

    public void setDevMachine(MachineImpl devMachine) {
        this.devMachine = devMachine;
    }

    @Override
    public List<MachineImpl> getMachines() {
        if (machines == null) {
            machines = new ArrayList<>();
        }
        return machines;
    }

    public void setMachines(List<MachineImpl> machines) {
        this.machines = machines;
    }

    @Override
    public String getEnvType() {
        return envType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkspaceRuntimeImpl)) return false;
        WorkspaceRuntimeImpl that = (WorkspaceRuntimeImpl)o;
        return Objects.equals(activeEnv, that.activeEnv) &&
               Objects.equals(rootFolder, that.rootFolder) &&
               Objects.equals(devMachine, that.devMachine) &&
               Objects.equals(machines, that.machines) &&
               Objects.equals(envType, that.envType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activeEnv,
                            rootFolder,
                            devMachine,
                            machines,
                            envType);
    }

    @Override
    public String toString() {
        return "WorkspaceRuntimeImpl{" +
               "activeEnv='" + activeEnv + '\'' +
               ", rootFolder='" + rootFolder + '\'' +
               ", devMachine=" + devMachine +
               ", machines=" + machines +
               ", envType='" + envType + '\'' +
               '}';
    }
}
