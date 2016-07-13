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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.google.common.io.CharStreams;

import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.machine.Machine;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.SnapshotImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.util.RecipeRetriever;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.machine.shared.dto.MachineDto;
import org.eclipse.che.api.machine.shared.dto.MachineProcessDto;
import org.eclipse.che.api.machine.shared.dto.NewSnapshotDescriptor;
import org.eclipse.che.api.machine.shared.dto.SnapshotDto;
import org.eclipse.che.commons.env.EnvironmentContext;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Machine API
 *
 * @author Alexander Garagatyi
 * @author Anton Korneta
 */
@Api(value = "/machine", description = "Machine REST API")
@Path("/machine")
public class MachineService extends Service {
    private MachineManager machineManager;

    private final MachineServiceLinksInjector linksInjector;
    private final RecipeRetriever recipeRetriever;

    @Inject
    public MachineService(MachineManager machineManager, RecipeRetriever recipeRetriever, MachineServiceLinksInjector linksInjector) {
        this.machineManager = machineManager;
        this.recipeRetriever = recipeRetriever;
        this.linksInjector = linksInjector;
    }

    @GET
    @Path("/{machineId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get machine by ID")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested machine entity"),
                   @ApiResponse(code = 404, message = "Machine with specified id does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public MachineDto getMachineById(@ApiParam(value = "Machine ID")
                                     @PathParam("machineId")
                                     String machineId)
            throws ServerException,
                   ForbiddenException,
                   NotFoundException {

        final Machine machine = machineManager.getMachine(machineId);
        return linksInjector.injectLinks(DtoConverter.asDto(machine), getServiceContext());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all machines of workspace with specified ID",
                  response = MachineDto.class,
                  responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested list of machine entities"),
                   @ApiResponse(code = 400, message = "Workspace ID is not specified"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<MachineDto> getMachines(@ApiParam(value = "Workspace ID", required = true)
                                        @QueryParam("workspace")
                                        String workspaceId)
            throws ServerException,
                   BadRequestException {

        requiredNotNull(workspaceId, "Parameter workspace");

        return machineManager.getMachines(workspaceId)
                             .stream()
                             .map(DtoConverter::asDto)
                             .map(machineDto -> linksInjector.injectLinks(machineDto, getServiceContext()))
                             .collect(Collectors.toList());
    }

    @DELETE
    @Path("/{machineId}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Destroy machine")
    @ApiResponses({@ApiResponse(code = 204, message = "Machine was successfully destroyed"),
                   @ApiResponse(code = 404, message = "Machine with specified id does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void destroyMachine(@ApiParam(value = "Machine ID")
                               @PathParam("machineId")
                               String machineId)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException {

        machineManager.destroy(machineId, true);
    }

    @GET
    @Path("/snapshot")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get all snapshots of machines in workspace",
                  response = SnapshotDto.class,
                  responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested list of snapshot entities"),
                   @ApiResponse(code = 400, message = "Workspace ID is not specified"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<SnapshotDto> getSnapshots(@ApiParam(value = "Workspace ID", required = true)
                                          @QueryParam("workspace")
                                          String workspaceId)
            throws ServerException,
                   BadRequestException {

        requiredNotNull(workspaceId, "Parameter workspace");

        final List<SnapshotImpl> snapshots = machineManager.getSnapshots(EnvironmentContext.getCurrent().getSubject().getUserId(), workspaceId);

        return snapshots.stream()
                        .map(DtoConverter::asDto)
                        .map(snapshotDto -> linksInjector.injectLinks(snapshotDto, getServiceContext()))
                        .collect(Collectors.toList());
    }

    @POST
    @Path("/{machineId}/snapshot")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Save snapshot of machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains requested snapshot entity"),
                   @ApiResponse(code = 400, message = "Snapshot description is not specified"),
                   @ApiResponse(code = 404, message = "Snapshot with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public SnapshotDto saveSnapshot(@ApiParam(value = "Machine ID")
                                    @PathParam("machineId")
                                    String machineId,
                                    @ApiParam(value = "Snapshot description", required = true)
                                    NewSnapshotDescriptor newSnapshotDescriptor)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException,
                   BadRequestException {

        requiredNotNull(newSnapshotDescriptor, "Snapshot description");
        return linksInjector.injectLinks(DtoConverter.asDto(machineManager.save(machineId,
                                                                                EnvironmentContext.getCurrent()
                                                                                                  .getSubject()
                                                                                                  .getUserId(),
                                                                                newSnapshotDescriptor.getDescription())),
                                         getServiceContext());
    }

    @DELETE
    @Path("/snapshot/{snapshotId}")
    @ApiOperation(value = "Remove snapshot of machine")
    @ApiResponses({@ApiResponse(code = 204, message = "Snapshot was successfully removed"),
                   @ApiResponse(code = 404, message = "Snapshot with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void removeSnapshot(@ApiParam(value = "Snapshot ID")
                               @PathParam("snapshotId")
                               String snapshotId)
            throws ForbiddenException,
                   NotFoundException,
                   ServerException {

        machineManager.removeSnapshot(snapshotId);
    }

    @POST
    @Path("/{machineId}/command")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Start specified command in machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains entity of created machine process"),
                   @ApiResponse(code = 400, message = "Command entity is invalid"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public MachineProcessDto executeCommandInMachine(@ApiParam(value = "Machine ID")
                                                     @PathParam("machineId")
                                                     String machineId,
                                                     @ApiParam(value = "Command to execute", required = true)
                                                     final CommandDto command,
                                                     @ApiParam(value = "Channel for command output", required = false)
                                                     @QueryParam("outputChannel")
                                                     String outputChannel)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException,
                   BadRequestException {

        requiredNotNull(command, "Command description");
        requiredNotNull(command.getCommandLine(), "Commandline");
        return linksInjector.injectLinks(DtoConverter.asDto(machineManager.exec(machineId, command, outputChannel)),
                                         machineId,
                                         getServiceContext());
    }

    @GET
    @Path("/{machineId}/process")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get processes of machine",
                  response = MachineProcessDto.class,
                  responseContainer = "List")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains machine process entities"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public List<MachineProcessDto> getProcesses(@ApiParam(value = "Machine ID")
                                                @PathParam("machineId")
                                                String machineId)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException {

        return machineManager.getProcesses(machineId)
                             .stream()
                             .map(DtoConverter::asDto)
                             .map(machineProcess -> linksInjector.injectLinks(machineProcess,
                                                                              machineId,
                                                                              getServiceContext()))
                             .collect(Collectors.toList());
    }

    @GET
    @Path("/{machineId}/recipe")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Retrieve machine recipe script")
    @ApiResponses({@ApiResponse(code = 200, message = "Recipe content retrieved"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public String getRecipeScript(@ApiParam(value = "Machine ID") @PathParam("machineId") String id)
            throws NotFoundException, ServerException {
        return recipeRetriever.getRecipe(machineManager.getMachine(id).getConfig()).getScript();
    }

    @DELETE
    @Path("/{machineId}/process/{processId}")
    @ApiOperation(value = "Stop process in machine")
    @ApiResponses({@ApiResponse(code = 204, message = "Process was successfully stopped"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void stopProcess(@ApiParam(value = "Machine ID")
                            @PathParam("machineId")
                            String machineId,
                            @ApiParam(value = "Process ID")
                            @PathParam("processId")
                            int processId)
            throws NotFoundException,
                   ForbiddenException,
                   ServerException {

        machineManager.stopProcess(machineId, processId);
    }

    @GET
    @Path("/{machineId}/logs")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get logs of machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains logs"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void getMachineLogs(@ApiParam(value = "Machine ID")
                               @PathParam("machineId")
                               String machineId,
                               @Context
                               HttpServletResponse httpServletResponse)
            throws NotFoundException,
                   ForbiddenException,
                   ServerException,
                   IOException {

        addLogsToResponse(machineManager.getMachineLogReader(machineId), httpServletResponse);
    }

    @GET
    @Path("/{machineId}/process/{pid}/logs")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get logs of machine process")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains logs"),
                   @ApiResponse(code = 404, message = "Machine or process with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void getProcessLogs(@ApiParam(value = "Machine ID")
                               @PathParam("machineId")
                               String machineId,
                               @ApiParam(value = "Process ID")
                               @PathParam("pid")
                               int pid,
                               @Context
                               HttpServletResponse httpServletResponse)
            throws NotFoundException,
                   ForbiddenException,
                   ServerException,
                   IOException {

        addLogsToResponse(machineManager.getProcessLogReader(machineId, pid), httpServletResponse);
    }

    /**
     * Reads file content by specified file path.
     *
     * @param path
     *         path to file on machine instance
     * @param startFrom
     *         line number to start reading from
     * @param limit
     *         limitation on line if not specified will used 2000 lines
     * @return file content.
     * @throws MachineException
     *         if any error occurs with file reading
     */
    @GET
    @Path("/{machineId}/filepath/{path:.*}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get content of file in machine")
    @ApiResponses({@ApiResponse(code = 200, message = "The response contains file content"),
                   @ApiResponse(code = 404, message = "Machine with specified ID does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public String getFileContent(@ApiParam(value = "Machine ID")
                                 @PathParam("machineId")
                                 String machineId,
                                 @ApiParam(value = "Path of file")
                                 @PathParam("path")
                                 String path,
                                 @ApiParam(value = "From line")
                                 @QueryParam("startFrom")
                                 @DefaultValue("1")
                                 Integer startFrom,
                                 @ApiParam(value = "Number of lines")
                                 @QueryParam("limit")
                                 @DefaultValue("2000")
                                 Integer limit)
            throws NotFoundException,
                   ForbiddenException,
                   ServerException {

        final Instance machine = machineManager.getInstance(machineId);
        return machine.readFileContent(path, startFrom, limit);
    }

    /**
     * Copies files from specified machine into current machine.
     *
     * @param sourceMachineId
     *         source machine id
     * @param targetMachineId
     *         target machine id
     * @param sourcePath
     *         path to file or directory inside specified machine
     * @param targetPath
     *         path to destination file or directory inside machine
     * @param overwrite
     *         If "false" then it will be an error if unpacking the given content would cause
     *         an existing directory to be replaced with a non-directory and vice versa.
     * @throws MachineException
     *         if any error occurs when files are being copied
     * @throws NotFoundException
     *         if any error occurs with getting source machine
     */
    @POST
    @Path("/copy")
    @ApiOperation(value = "Copy files from one machine to another")
    @ApiResponses({@ApiResponse(code = 200, message = "Files were copied successfully"),
                   @ApiResponse(code = 400, message = "Machine ID or path is not specified"),
                   @ApiResponse(code = 404, message = "Source machine does not exist"),
                   @ApiResponse(code = 404, message = "Target machine does not exist"),
                   @ApiResponse(code = 500, message = "Internal server error occurred")})
    public void copyFilesBetweenMachines(@ApiParam(value = "Source machine ID", required = true)
                                         @QueryParam("sourceMachineId")
                                         String sourceMachineId,
                                         @ApiParam(value = "Target machine ID", required = true)
                                         @QueryParam("targetMachineId")
                                         String targetMachineId,
                                         @ApiParam(value = "Source path", required = true)
                                         @QueryParam("sourcePath")
                                         String sourcePath,
                                         @ApiParam(value = "Target path", required = true)
                                         @QueryParam("targetPath")
                                         String targetPath,
                                         @ApiParam(value = "Is files overwriting allowed")
                                         @QueryParam("overwrite")
                                         @DefaultValue("false")
                                         Boolean overwrite)
            throws NotFoundException,
                   ServerException,
                   ForbiddenException,
                   BadRequestException {

        requiredNotNull(sourceMachineId, "Source machine id");
        requiredNotNull(targetMachineId, "Target machine id");
        requiredNotNull(sourcePath, "Source path");
        requiredNotNull(targetPath, "Target path");

        final Instance sourceMachine = machineManager.getInstance(sourceMachineId);
        final Instance targetMachine = machineManager.getInstance(targetMachineId);
        targetMachine.copy(sourceMachine, sourcePath, targetPath, overwrite);
    }

    private void addLogsToResponse(Reader logsReader, HttpServletResponse httpServletResponse) throws IOException {
        // Response is written directly to the servlet request stream
        httpServletResponse.setContentType("text/plain");
        CharStreams.copy(logsReader, httpServletResponse.getWriter());
        httpServletResponse.getWriter().flush();
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws BadRequestException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws BadRequestException {
        if (object == null) {
            throw new BadRequestException(subject + " required");
        }
    }
}
