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
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.machine.appliance.recipe;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;

import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.machine.MachineServiceClient;
import org.eclipse.che.ide.api.machine.RecipeServiceClient;
import org.eclipse.che.ide.extension.machine.client.machine.Machine;
import org.eclipse.che.ide.extension.machine.client.perspective.widgets.tab.content.TabPresenter;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * The class contains business logic which allows update a recipe for current machine. The class is a tab presenter and
 * shows current machine recipe.
 *
 * @author Valeriy Svydenko
 */
public class RecipeTabPresenter implements TabPresenter {

    private final RecipeView           view;
    private final MachineServiceClient machineServiceClient;
    @Inject
    public RecipeTabPresenter(RecipeView view, MachineServiceClient machineServiceClient) {
        this.view = view;
        this.machineServiceClient = machineServiceClient;
    }

    /**
     * Calls special method on view which updates recipe of current machine.
     *
     * @param machine
     *         machine for which need update information
     */
    public void updateInfo(@NotNull final Machine machine) {
        machineServiceClient.getRecipeScript(machine.getId()).then(new Operation<String>() {
            @Override
            public void apply(String recipe) throws OperationException {
                view.setScript(recipe);
            }
        }).catchError(new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                Log.error(RecipeTabPresenter.class,
                          "Failed to get recipe script for machine " + machine.getId() + ": " + error.getMessage());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public IsWidget getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }
}
