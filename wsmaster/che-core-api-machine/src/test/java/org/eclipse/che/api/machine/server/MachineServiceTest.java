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
package org.eclipse.che.api.machine.server;

import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineSourceImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeServiceTest;
import org.eclipse.che.api.machine.server.util.RecipeRetriever;
import org.everrest.assured.EverrestJetty;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Mihail Kuznyetsov.
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class MachineServiceTest {

    @SuppressWarnings("unused")
    static final RecipeServiceTest.EnvironmentFilter FILTER        = new RecipeServiceTest.EnvironmentFilter();
    @SuppressWarnings("unused")
    static final ApiExceptionMapper                  MAPPER        = new ApiExceptionMapper();
    static final String                              MACHINE_ID    = "machine123";
    static final String                              RECIPE_SCRIPT = "FROM eclipse/che";
    @Mock
    RecipeRetriever recipeRetriever;
    @Mock
    MachineManager machineManager;

    @InjectMocks
    MachineService machineService;

    @Test
    public void shouldReturnRecipeScript() throws Exception {
        when(recipeRetriever.getRecipe(any(MachineConfig.class))).thenReturn(new RecipeImpl().withScript(RECIPE_SCRIPT));
        when(machineManager.getMachine(eq(MACHINE_ID)))
                .thenReturn(MachineImpl.builder()
                                       .setConfig(MachineConfigImpl.builder()
                                                                   .setSource(new MachineSourceImpl("dockerfile")
                                                                                      .setLocation("http://localhost:8080/myrecipe"))
                                                                   .build()
                                       ).build());

        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .when()
                                         .get(SECURE_PATH + "/machine/" + MACHINE_ID + "/recipe");

        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getBody().print(), RECIPE_SCRIPT);
    }
}
