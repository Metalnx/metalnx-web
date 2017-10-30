/*
 * Copyright (c) 2015-2017, Dell EMC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.emc.metalnx.controller;

import com.emc.metalnx.controller.utils.LoggedUserUtils;
import com.emc.metalnx.core.domain.entity.DataGridCollectionAndDataObject;
import com.emc.metalnx.core.domain.entity.DataGridUser;
import com.emc.metalnx.core.domain.entity.enums.DataGridPermType;
import com.emc.metalnx.core.domain.exceptions.DataGridConnectionRefusedException;
import com.emc.metalnx.core.domain.exceptions.DataGridException;
import com.emc.metalnx.core.domain.exceptions.DataGridReplicateException;
import com.emc.metalnx.modelattribute.collection.CollectionOrDataObjectForm;
import com.emc.metalnx.services.interfaces.*;
import org.irods.jargon.core.exception.JargonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@Scope(WebApplicationContext.SCOPE_SESSION)
@RequestMapping(value = "/fileOperation")
public class FileOperationsController {

    private static final Logger logger = LoggerFactory.getLogger(FileOperationsController.class);
    private static String TRASH_PATH;

    @Autowired
    private CollectionController collectionController;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private IRODSServices irodsServices;

    @Autowired
    private UserService userService;

    @Autowired
    private FileOperationService fileOperationService;

    @Autowired
    private LoggedUserUtils loggedUserUtils;

    @Autowired
    private UploadService us;

    // contains the path to the file that will be downloaded
    private String filePathToDownload;

    // checks if it's necessary to remove any temporary collections created for downloading
    private boolean removeTempCollection;

    @PostConstruct
    public void init() {
        TRASH_PATH = String.format("/%s/trash/home/%s", irodsServices.getCurrentUserZone(), irodsServices.getCurrentUser());
    }

    @RequestMapping(value = "/move", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public String move(Model model, @RequestParam("targetPath") String targetPath,
                       @RequestParam("paths[]") String[] paths)
            throws DataGridException, JargonException {

        List<String> failedMoves = new ArrayList<>();
        String fileMoved = "";

        try {
            for (String p : paths) {
                String item = p.substring(p.lastIndexOf("/") + 1, p.length());
                if (!fileOperationService.move(p, targetPath)) {
                    failedMoves.add(item);
                } else if (paths.length == 1) {
                    fileMoved = item;
                }
            }
        } catch (DataGridException e) {
            logger.error("Could not move item to {}: {}", targetPath, e.getMessage());
        }

        if (!fileMoved.isEmpty()) model.addAttribute("fileMoved", fileMoved);

        model.addAttribute("failedMoves", failedMoves);

        return collectionController.getSubDirectories(model, targetPath);
    }

    @RequestMapping(value = "/copy", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public String copy(Model model, @RequestParam("targetPath") String targetPath,
                       @RequestParam("copyWithMetadata") boolean copyWithMetadata,
                       @RequestParam("paths[]") String[] paths)
            throws DataGridException, JargonException {

        List<String> failedCopies = new ArrayList<>();
        String fileCopied = "";

        for (String p : paths) {
            String item = p.substring(p.lastIndexOf("/") + 1, p.length());
            if (!fileOperationService.copy(p, targetPath, copyWithMetadata)) {
                failedCopies.add(item);
            } else if (paths.length == 1) {
                fileCopied = item;
            }
        }

        if (!fileCopied.isEmpty()) model.addAttribute("fileCopied", fileCopied);

        model.addAttribute("failedCopies", failedCopies);

        return collectionController.getSubDirectories(model, targetPath);
    }

    /**
     * Delete a replica of a data object
     *
     * @param model MVC model
     * @param path path to the parent of the data object to be deleted
     * @param fileName name of the data object to be deleted
     * @param replicaNumber number of the replica that is going to be deleted
     * @return the template that shows the data object information with the replica table refreshed
     * @throws DataGridConnectionRefusedException if Metalnx cannot connect to the data grid
     */
    @RequestMapping(value = "deleteReplica", method = RequestMethod.POST)
    public String deleteReplica(Model model, @RequestParam("path") String path, @RequestParam("fileName") String fileName,
                                @RequestParam String replicaNumber) throws DataGridConnectionRefusedException {

        boolean inAdminMode = loggedUserUtils.getLoggedDataGridUser().isAdmin();

        if (fileOperationService.deleteReplica(path, fileName, Integer.parseInt(replicaNumber), inAdminMode)) {
            model.addAttribute("delReplReturn", "success");
        } else {
            model.addAttribute("delReplReturn", "failure");
        }
        return collectionController.getFileInfo(model, path);
    }

    @RequestMapping(value = "/replicate", method = RequestMethod.POST)
    public String replicate(Model model, HttpServletRequest request) throws DataGridConnectionRefusedException {

        List<String> sourcePaths = collectionController.getSourcePaths();
        String[] resources = (String[]) request.getParameterMap().get("resourcesForReplication");
        List<String> failedReplicas = new ArrayList<>();

        if (resources != null) {
            String targetResource = resources[0];
            boolean inAdminMode = loggedUserUtils.getLoggedDataGridUser().isAdmin();
            for (String sourcePathItem : sourcePaths) {
                try {
                    fileOperationService.replicateDataObject(sourcePathItem, targetResource, inAdminMode);
                } catch (DataGridReplicateException e) {
                    String item = sourcePathItem.substring(sourcePathItem.lastIndexOf("/") + 1, sourcePathItem.length());
                    failedReplicas.add(item);
                }
            }
        }

        if (!sourcePaths.isEmpty()) {
            model.addAttribute("failedReplicas", failedReplicas);
            sourcePaths.clear();
        }

        return collectionController.index(model, request, false);
    }

    @RequestMapping(value = "/prepareFilesForDownload/", method = RequestMethod.GET)
    public void prepareFilesForDownload(HttpServletResponse response, @RequestParam("paths[]") String[] paths)
            throws DataGridConnectionRefusedException {

        try {
            if (paths.length > 1 || !collectionService.isDataObject(paths[0])) {
                filePathToDownload = collectionService.prepareFilesForDownload(paths);
                removeTempCollection = true;
            } else {
                // if a single file was selected, it will be transferred directly through the HTTP response
                removeTempCollection = false;
                filePathToDownload = paths[0];
                String permissionType = collectionService.getPermissionsForPath(filePathToDownload);
                if (permissionType.equalsIgnoreCase(DataGridPermType.NONE.name())) {
                    throw new DataGridException("Lack of permission to download file " + filePathToDownload);
                }
            }
        } catch (DataGridConnectionRefusedException e) {
            throw e;
        } catch (DataGridException | IOException e) {
            logger.error("Could not download selected items: ", e.getMessage());
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    @RequestMapping(value = "/download/", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void download(HttpServletResponse response) throws DataGridConnectionRefusedException {

        try {
            fileOperationService.download(filePathToDownload, response, removeTempCollection);
            filePathToDownload = "";
            removeTempCollection = false;
        } catch (DataGridException | IOException e) {
            logger.error("Could not download selected items: ", e.getMessage());
        }
    }

    @RequestMapping(value = "/delete/", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public String deleteCollectionAndDataObject(Model model, @RequestParam("paths[]") String[] paths)
            throws DataGridException, JargonException {
        boolean forceRemove;
        List<String> failedDeletions = new ArrayList<>();
        String fileDeleted = null;

        for (String path : paths) {
            forceRemove = path.startsWith(TRASH_PATH);
            
            if (!fileOperationService.deleteItem(path, forceRemove)) {
                String item = path.substring(path.lastIndexOf("/") + 1, path.length());
                failedDeletions.add(item);
            } 
            else if (paths.length == 1) {
                fileDeleted = path.substring(path.lastIndexOf("/") + 1, path.length());
            }
            
            collectionController.removePathFromHistory(path);
        }

        if (fileDeleted != null) {
            model.addAttribute("fileDeleted", fileDeleted);
        }

        model.addAttribute("failedDeletions", failedDeletions);
        model.addAttribute("currentPath", collectionController.getCurrentPath());
        model.addAttribute("parentPath", collectionController.getParentPath());
        
        return collectionController.getSubDirectories(model, collectionController.getCurrentPath());
    }

    @RequestMapping(value = "emptyTrash/", method = RequestMethod.POST)
    public ResponseEntity<String> emptyTrash() throws DataGridConnectionRefusedException {
        String trashForCurrentPath = collectionService.getTrashForPath(collectionController.getCurrentPath());
        DataGridUser loggedUser = loggedUserUtils.getLoggedDataGridUser();
        if (fileOperationService.emptyTrash(loggedUser, trashForCurrentPath)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Displays the modify user form with all fields set to the selected collection
     *
     * @param model MVC model
     * @return collectionForm with fields set
     * @throws DataGridException if item cannot be modified
     */
    @RequestMapping(value = "modify/", method = RequestMethod.GET)
    public String showModifyForm(Model model, @RequestParam("path") String path) throws DataGridException {
        String currentPath = collectionController.getCurrentPath();
        String parentPath = collectionController.getParentPath();

        String formType = "editDataObjectForm";
        CollectionOrDataObjectForm targetForm = new CollectionOrDataObjectForm();
        DataGridCollectionAndDataObject dataGridCollectionAndDataObject = collectionService.findByName(path);

        logger.info("Modify form for {}", path);

        targetForm.setCollectionName(dataGridCollectionAndDataObject.getName());
        targetForm.setPath(dataGridCollectionAndDataObject.getPath());
        targetForm.setParentPath(currentPath);

        if (dataGridCollectionAndDataObject.isCollection()) {
            formType = "editCollectionForm";
            targetForm.setCollection(true);
            logger.info("Setting inheritance for {}", path);
            targetForm.setInheritOption(collectionService.getInheritanceOptionForCollection(targetForm.getPath()));
        }

        model.addAttribute("currentPath", currentPath);
        model.addAttribute("parentPath", parentPath);
        model.addAttribute("collection", targetForm);
        model.addAttribute("requestMapping", "/collections/modify/action/");

        return String.format("collections/%s", formType);
    }
}
