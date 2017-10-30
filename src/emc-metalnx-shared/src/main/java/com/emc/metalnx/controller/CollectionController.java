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
import com.emc.metalnx.core.domain.entity.*;
import com.emc.metalnx.core.domain.exceptions.DataGridConnectionRefusedException;
import com.emc.metalnx.core.domain.exceptions.DataGridException;
import com.emc.metalnx.modelattribute.breadcrumb.DataGridBreadcrumb;
import com.emc.metalnx.modelattribute.collection.CollectionOrDataObjectForm;
import com.emc.metalnx.modelattribute.metadatatemplate.MetadataTemplateForm;
import com.emc.metalnx.services.interfaces.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.irods.jargon.core.exception.JargonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@Scope(WebApplicationContext.SCOPE_SESSION)
@SessionAttributes({ "sourcePaths" })
@RequestMapping(value = "/collections")
public class CollectionController {

    @Autowired
    CollectionService cs;

    @Autowired
    ResourceService resourceService;

    @Autowired
    UserService userService;

    @Autowired
    GroupService groupService;

    @Autowired
    GroupBookmarkService groupBookmarkService;

    @Autowired
    UserBookmarkService userBookmarkService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    GroupBookmarkController groupBookmarkController;

    @Autowired
    PermissionsService permissionsService;

    @Autowired
    IRODSServices irodsServices;

    @Autowired
    FavoritesService favoritesService;

    @Autowired
    LoggedUserUtils loggedUserUtils;

    @Autowired
    RuleDeploymentService ruleDeploymentService;

    // parent path of the current directory in the tree view
    private String parentPath;

    // path to the current directory in the tree view
    private String currentPath;

    // number of pages for current path
    private int totalObjsForCurrentPath;

    // number of pages for current search
    private int totalObjsForCurrentSearch;

    // Auxiliary structure to manage download, upload, copy and move operations
    private List<String> sourcePaths;

    // ui mode that will be shown when the rods user switches mode from admin to user and vice-versa
    public static final String UI_USER_MODE = "user";
    public static final String UI_ADMIN_MODE = "admin";

    public static final int MAX_HISTORY_SIZE = 10;

    private boolean cameFromMetadataSearch;
    private boolean cameFromFilePropertiesSearch;
    private boolean cameFromBookmarks;

    private Stack<String> collectionHistoryBack;
    private Stack<String> collectionHistoryForward;

    // variable to save trash path for the logged user
    private String userTrashPath = "";
    // saves the trash under the zone
    private String zoneTrashPath = "";

    private static final Logger logger = LoggerFactory.getLogger(CollectionController.class);

    @PostConstruct
    public void init() throws DataGridException {
        collectionHistoryBack = new Stack<String>();
        collectionHistoryForward = new Stack<String>();

        cameFromMetadataSearch = false;
        cameFromFilePropertiesSearch = false;
        cameFromBookmarks = false;

        sourcePaths = new ArrayList<>();
        parentPath = "";
        currentPath = "";
    }

    /**
     * Responds the collections/ request
     *
     * @param model
     * @return the collection management template
     * @throws DataGridException
     */
    @RequestMapping(value = "/")
    public String index(Model model, HttpServletRequest request, @RequestParam(value = "uploadNewTab", required = false) boolean uploadNewTab)
            throws DataGridConnectionRefusedException {
        try {
            sourcePaths.clear();

            if (!cs.isPathValid(currentPath)) {
                currentPath = cs.getHomeDirectyForCurrentUser();
                parentPath = currentPath;
            }
            else if (cs.isDataObject(currentPath)) {
                parentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
            }

            DataGridUser loggedUser = loggedUserUtils.getLoggedDataGridUser();
            String uiMode = (String) request.getSession().getAttribute("uiMode");

            if (uiMode == null || uiMode.isEmpty()) {
                boolean isUserAdmin = loggedUser != null && loggedUser.isAdmin();
                uiMode = isUserAdmin ? UI_ADMIN_MODE :UI_USER_MODE;
            }

            if (uiMode.equals(UI_USER_MODE)) {
                model.addAttribute("homePath", cs.getHomeDirectyForCurrentUser());
                model.addAttribute("publicPath", cs.getHomeDirectyForPublic());
            }
            if (uploadNewTab) {
                model.addAttribute("uploadNewTab", uploadNewTab);
            }

            model.addAttribute("cameFromFilePropertiesSearch", cameFromFilePropertiesSearch);
            model.addAttribute("cameFromMetadataSearch", cameFromMetadataSearch);
            model.addAttribute("cameFromBookmarks", cameFromBookmarks);
            model.addAttribute("uiMode", uiMode);
            model.addAttribute("currentPath", currentPath);
            model.addAttribute("parentPath", parentPath);
            model.addAttribute("resources", resourceService.findAll());
            model.addAttribute("overwriteFileOption", loggedUser != null && loggedUser.isForceFileOverwriting());

            cameFromMetadataSearch = false;
            cameFromFilePropertiesSearch = false;
            cameFromBookmarks = false;
        } catch (DataGridException e) {
            logger.error("Could not respond to request for collections: {}", e);
            model.addAttribute("unexpectedError", true);
        }

        return "collections/collectionManagement";
    }

    @RequestMapping(value = "redirectFromMetadataToCollections/")
    @ResponseStatus(value = HttpStatus.OK)
    public void redirectFromMetadataToCollections(@RequestParam String path) {
        assignNewValuesToCurrentAndParentPath(path);
        cameFromMetadataSearch = true;
    }

    @RequestMapping(value = "redirectFromFavoritesToCollections/")
    @ResponseStatus(value = HttpStatus.OK)
    public void redirectFromFavoritesToCollections(@RequestParam String path) {
        assignNewValuesToCurrentAndParentPath(path);
    }

    @RequestMapping(value = "redirectFromGroupsBookmarksToCollections/")
    @ResponseStatus(value = HttpStatus.OK)
    public void redirectFromGroupsBookmarksToCollections(@RequestParam String path) {
        cameFromBookmarks = true;
        assignNewValuesToCurrentAndParentPath(path);
    }

    @RequestMapping(value = "redirectFromUserBookmarksToCollections/")
    @ResponseStatus(value = HttpStatus.OK)
    public void redirectFromUserBookmarksToCollections(@RequestParam String path) {
        cameFromBookmarks = true;
        assignNewValuesToCurrentAndParentPath(path);
    }

    @RequestMapping(value = "redirectFromFilePropertiesToCollections/")
    @ResponseStatus(value = HttpStatus.OK)
    public void redirectFromFilePropertiesToCollections(@RequestParam String path) {
        assignNewValuesToCurrentAndParentPath(path);
        cameFromFilePropertiesSearch = true;
    }

    /**
     * Get a list of resources in which an object doesn't have replicas
     *
     * @param model
     * @return list of resources in which an object can be replicated
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "getAvailableRescForPath/")
    public String getAvailableRescForPath(Model model, @RequestParam("isUpload") boolean isUpload) throws DataGridConnectionRefusedException {

        Map<DataGridCollectionAndDataObject, DataGridResource> replicasMap = null;
        List<DataGridResource> resources = resourceService.findFirstLevelResources();

        if(!isUpload){
            for (String path : sourcePaths) {
                replicasMap = cs.listReplicasByResource(path);
                for (DataGridResource resc : replicasMap.values()) {
                    if (resources.contains(resc)) {
                        resources.remove(resc);
                    }
                }
            }
        }
        model.addAttribute("resources", resources);
        return "collections/collectionsResourcesForReplica";
    }

    /**
     * Switches an admin from the Rods_Admin UI to the Rods_User UI and vice-versa.
     *
     * @param model
     * @return redirects an admin user from to the new UI view mode (admin view or user view)
     */
    @RequestMapping(value = "/switchMode/")
    @ResponseStatus(value = HttpStatus.OK)
    public void switchMode(Model model, HttpServletRequest request, @RequestParam("currentMode") String currentMode,
            final RedirectAttributes redirectAttributes) {

        // if the admin is currently seeing the Admin UI, we need to switch it
        // over to the USER UI
        if (currentMode.equalsIgnoreCase(UI_ADMIN_MODE)) {
            request.getSession().setAttribute("uiMode", UI_USER_MODE);
        }
        // if the admin is currently seeing the User UI, we need to switch it
        // over to the ADMIN UI
        else if (currentMode.equalsIgnoreCase(UI_USER_MODE)) {
            request.getSession().setAttribute("uiMode", UI_ADMIN_MODE);
        }
    }

    /**
     * Responds the getSubdirectories request finding collections and data objects that exist
     * underneath a certain path
     *
     * @param model
     * @param path path to find all subdirectories and objects
     * @return treeView template that renders all nodes of certain path (parent)
     * @throws DataGridException if Metalnx cannot find collections and objects inside the path
     */
    @RequestMapping(value = "/getSubDirectories/", method = RequestMethod.POST)
    public String getSubDirectories(Model model, @RequestParam("path") String path) throws DataGridException {

        // removes all ocurrences of "/" at the end of the path string
        while (path.endsWith("/") && !"/".equals(path)) {
            path = path.substring(0, path.lastIndexOf("/"));
        }

        logger.info("Get subdirectories of {}", path);

        // put old path in collection history stack
        addPathToHistory(path);

        return getCollBrowserView(model, path);
    }

    /**
     * Goes back in collection historic stack
     *
     * @param model
     * @param steps
     * @return treeView template that renders all nodes of certain path (parent)
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "/goBackHistory/", method = RequestMethod.POST)
    public String goBackHistory(Model model, @RequestParam("steps") int steps) throws DataGridException, JargonException {
        if (collectionHistoryBack.size() < steps || steps < 1) {
            model.addAttribute("invalidStepsBackwards", steps);
            logger.info("It is not possible to go back {} steps, current stack size is {}", steps, collectionHistoryBack.size());
            return getCollBrowserView(model, currentPath);
        }

        logger.info("Going back {} steps in collection history", steps);

        // pop paths from collectionHistoryBack and push them to collectionHistoryForward
        while (collectionHistoryForward.size() >= MAX_HISTORY_SIZE) {
            collectionHistoryForward.remove(0);
        }
        collectionHistoryForward.push(currentPath);
        for (int i = 0; i < steps - 1; i++) {
            String elementHistory = collectionHistoryBack.pop();
            while (collectionHistoryForward.size() >= MAX_HISTORY_SIZE) {
                collectionHistoryForward.remove(0);
            }
            collectionHistoryForward.push(elementHistory);
        }

        return getCollBrowserView(model, collectionHistoryBack.pop());
    }

    /**
     * Goes forward in collection historic stack
     *
     * @param model
     * @param steps
     * @return treeView template that renders all nodes of certain path (parent)
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "/goForwardHistory/", method = RequestMethod.POST)
    public String goForwardHistory(Model model, @RequestParam("steps") int steps) throws DataGridException, JargonException {
        if (collectionHistoryForward.size() < steps || steps < 1) {
            model.addAttribute("invalidStepsForward", steps);
            return getCollBrowserView(model, currentPath);
        }

        logger.info("Going {} steps forward in collection history", steps);

        // pop paths from collectionHistoryBack and push them to collectionHistoryForward
        while (collectionHistoryBack.size() >= MAX_HISTORY_SIZE) {
            collectionHistoryBack.remove(0);
        }
        collectionHistoryBack.push(currentPath);
        for (int i = 0; i < steps - 1; i++) {
            String elementHistory = collectionHistoryForward.pop();
            while (collectionHistoryBack.size() >= MAX_HISTORY_SIZE) {
                collectionHistoryBack.remove(0);
            }
            collectionHistoryBack.push(elementHistory);
        }

        return getCollBrowserView(model, collectionHistoryForward.pop());
    }

    /**
     * Responds the getSubdirectories request finding collections and data objects that exist
     * underneath a certain path
     *
     * @param model
     * @param path
     * @return treeView template that renders all nodes of certain path (parent)
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "/getSubDirectoriesOldTree/")
    public String getSubDirectoriesOldTree(Model model, @RequestParam("path") String path) throws DataGridConnectionRefusedException {

        if (path.isEmpty()) {
            path = "/";
        }
        else {
            if (path.endsWith("/") && path.compareTo("/") != 0) {
                path = path.substring(0, path.length() - 1);
            }
        }

        // The line below was modified so that only collection would be retrieved
        model.addAttribute("dataGridCollectionAndDataObjectList", cs.getSubCollectionsUnderPath(path));

        return "collections/oldTreeView :: oldTreeView";
    }

    /**
     * Gets checksum, total number of replicas and where each replica lives in the data grid for a
     * specific data object
     *
     * @param model
     * @param path
     *            path to the data object to get checksum and replica information
     * @return the template that shows the data object information
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "/info/", method = RequestMethod.POST)
    public String getFileInfo(Model model, @RequestParam("path") String path) throws DataGridConnectionRefusedException {

        DataGridCollectionAndDataObject dataGridObj = null;
        Map<DataGridCollectionAndDataObject, DataGridResource> replicasMap = null;

        try {
            dataGridObj = cs.findByName(path);

            if (dataGridObj != null && !dataGridObj.isCollection()) {
                replicasMap = cs.listReplicasByResource(path);
                dataGridObj.setChecksum(cs.getChecksum(path));
                dataGridObj.setNumberOfReplicas(cs.getTotalNumberOfReplsForDataObject(path));
                dataGridObj.setReplicaNumber(String.valueOf(cs.getReplicationNumber(path)));
                permissionsService.resolveMostPermissiveAccessForUser(dataGridObj, loggedUserUtils.getLoggedDataGridUser());
            }

        }
        catch (DataGridConnectionRefusedException e) {
            logger.error("Could not connect to the data grid", e);
            throw e;
        }
        catch (DataGridException e) {
            logger.error("Could not get file info for {}", path, e);
        }

        model.addAttribute("collectionAndDataObject", dataGridObj);
        model.addAttribute("currentCollection", dataGridObj);
        model.addAttribute("replicasMap", replicasMap);

        return "collections/collectionInfo";
    }

    /**
     * Finds all collections and files existing under a certain path for a given group name.
     *
     * @param model
     * @param path
     *            start point to get collections and files
     * @param groupName
     *            group that all collections and files permissions will be listed
     * @return
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "/getDirectoriesAndFilesForGroupForm")
    public String getDirectoriesAndFilesForGroupForm(Model model, @RequestParam("path") String path, @RequestParam("groupName") String groupName,
            @RequestParam("retrievePermissions") boolean retrievePermissions) throws DataGridConnectionRefusedException {
        if (path == null || path == "") {
            path = "/";
        }

        List<DataGridCollectionAndDataObject> list = null;
        list = cs.getSubCollectionsAndDataObjetsUnderPath(path);

        Set<String> readPermissions = null;
        Set<String> writePermissions = null;
        Set<String> ownershipPermissions = null;
        Set<String> inheritPermissions = null;

        if (retrievePermissions) {
            readPermissions = cs.listReadPermissionsForPathAndGroup(path, groupName);
            writePermissions = cs.listWritePermissionsForPathAndGroup(path, groupName);
            ownershipPermissions = cs.listOwnershipForPathAndGroup(path, groupName);
            inheritPermissions = cs.listInheritanceForPath(path);
        }
        else {
            readPermissions = new HashSet<String>();
            writePermissions = new HashSet<String>();
            ownershipPermissions = new HashSet<String>();
            inheritPermissions = new HashSet<String>();
        }

        List<String> groupBookmarks = new ArrayList<String>();
        if (groupName.length() > 0) {
            DataGridGroup group = groupService.findByGroupname(groupName).get(0);
            groupBookmarks = groupBookmarkService.findBookmarksForGroupAsString(group);
        }

        model.addAttribute("dataGridCollectionAndDataObjectList", list);
        model.addAttribute("currentPath", path);
        model.addAttribute("readPermissions", readPermissions);
        model.addAttribute("writePermissions", writePermissions);
        model.addAttribute("ownershipPermissions", ownershipPermissions);
        model.addAttribute("inheritPermissions", inheritPermissions);
        model.addAttribute("addBookmark", groupBookmarkController.getAddBookmark());
        model.addAttribute("removeBookmark", groupBookmarkController.getRemoveBookmark());
        model.addAttribute("groupBookmarks", groupBookmarks);

        return "collections/treeViewForGroupForm :: treeView";
    }

    /**
     * Finds all collections existing under a certain path.
     *
     * @param model
     * @param path
     *            start point to get collections and files
     * @param username
     *            user who all collections and files permissions will be listed
     * @return the template that will render the tree
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "/getDirectoriesAndFilesForUser")
    public String getDirectoriesAndFilesForUser(Model model, @RequestParam("path") String path, @RequestParam("username") String username,
            @RequestParam("retrievePermissions") boolean retrievePermissions) throws DataGridConnectionRefusedException {
        List<DataGridCollectionAndDataObject> list = new ArrayList<DataGridCollectionAndDataObject>();
        Set<String> readPermissions = new HashSet<String>();
        Set<String> writePermissions = new HashSet<String>();
        Set<String> ownershipPermissions = new HashSet<String>();
        Set<String> inheritPermissions = new HashSet<String>();
        List<String> userBookmarks = new ArrayList<String>();

        // If a string is null, empty or contains only white spaces, StringUtils
        // returns true
        boolean isPathEmpty = StringUtils.isEmptyOrWhitespace(path);
        boolean isUsernameEmpty = StringUtils.isEmptyOrWhitespace(username);

        if (!isPathEmpty) {
            // When adding a user (there is no username), we still need to be
            // able to walk through the iRODS tree
            list = cs.getSubCollectionsAndDataObjetsUnderPath(path);

            if (!isUsernameEmpty) {
                if (retrievePermissions) {
                    readPermissions = cs.listReadPermissionsForPathAndUser(path, username);
                    writePermissions = cs.listWritePermissionsForPathAndUser(path, username);
                    ownershipPermissions = cs.listOwnershipForPathAndUser(path, username);
                    inheritPermissions = cs.listInheritanceForPath(path);
                }

                List<DataGridUser> users = userService.findByUsername(username);
                if (users != null && !users.isEmpty()) {
                    userBookmarks = userBookmarkService.findBookmarksForUserAsString(users.get(0));
                }
            }
        }

        model.addAttribute("dataGridCollectionAndDataObjectList", list);
        model.addAttribute("currentPath", path);
        model.addAttribute("readPermissions", readPermissions);
        model.addAttribute("writePermissions", writePermissions);
        model.addAttribute("ownershipPermissions", ownershipPermissions);
        model.addAttribute("inheritPermissions", inheritPermissions);
        model.addAttribute("addBookmark", new ArrayList<String>());
        model.addAttribute("removeBookmark", new ArrayList<String>());
        model.addAttribute("userBookmarks", userBookmarks);

        return "collections/treeViewForUserForm :: treeView";
    }

    /**
     * Looks for collections or data objects that match the parameter string
     *
     * @param model
     * @param name
     *            collection name that will be searched in the data grid
     * @return the template that renders all collections and data objects matching the parameter
     *         string
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "/find/{name}")
    public String listCollectionsAndDataObjects(Model model, @PathVariable String name) throws DataGridConnectionRefusedException {
        logger.info("Finding collections or data objects that match " + name);

        // Find collections and data objects
        List<DataGridCollectionAndDataObject> dataGridCollectionAndDataObjects = cs.searchCollectionAndDataObjectsByName(name + "%");
        model.addAttribute("dataGridCollectionAndDataObjects", dataGridCollectionAndDataObjects);
        return "collections/collectionsBrowser :: treeView";
    }

    /**
     * Performs the action of actually creating a collection in iRODS
     *
     * @param model
     * @param collection
     * @return if the creation of collection was successful, it returns the collection management
     *         template, and returns the add collection template,
     *         otherwise.
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "add/action/", method = RequestMethod.POST)
    public String addCollection(Model model, @ModelAttribute CollectionOrDataObjectForm collection, final RedirectAttributes redirectAttributes)
            throws DataGridConnectionRefusedException {
        DataGridCollectionAndDataObject newCollection = new DataGridCollectionAndDataObject(currentPath + '/' + collection.getCollectionName(),
                collection.getCollectionName(), currentPath, true);

        newCollection.setParentPath(currentPath);
        newCollection.setCreatedAt(new Date());
        newCollection.setModifiedAt(newCollection.getCreatedAt());
        newCollection.setInheritanceOption(collection.getInheritOption());

        boolean creationSucessful;
        try {
            creationSucessful = cs.createCollection(newCollection);

            if (creationSucessful) {
                redirectAttributes.addFlashAttribute("collectionAddedSuccessfully", collection.getCollectionName());
            }
        }
        catch (DataGridConnectionRefusedException e) {
            throw e;
        }
        catch (DataGridException e) {
            logger.error("Could not create collection/data object (lack of permission): ", e.getMessage());
            redirectAttributes.addFlashAttribute("missingPermissionError", true);
        }

        return "redirect:/collections/";
    }

    /**
     * Performs the action of modifying a collection
     *
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "modify/action", method = RequestMethod.POST)
    public String modifyAction(@ModelAttribute CollectionOrDataObjectForm collForm,
                               RedirectAttributes redirectAttributes)
            throws DataGridException {
        String previousPath = collForm.getPath();
        String parentPath = previousPath.substring(0, previousPath.lastIndexOf("/"));
        String newPath = String.format("%s/%s", parentPath, collForm.getCollectionName());

        logger.info("Modify action for " + previousPath + "/" + newPath);
        boolean modificationSuccessful = cs.modifyCollectionAndDataObject(previousPath, newPath, collForm.getInheritOption());

        if (modificationSuccessful) {
            logger.debug("Collection/Data Object {} modified to {}", previousPath, newPath);

        	userBookmarkService.updateBookmark(previousPath, newPath);
        	groupBookmarkService.updateBookmark(previousPath, newPath);

            redirectAttributes.addFlashAttribute("collectionModifiedSuccessfully", collForm.getCollectionName());
        }

        return "redirect:/collections/";
    }

    @RequestMapping(value = "applyTemplatesToCollections/", method = RequestMethod.POST)
    public String applyTemplatesToCollections(RedirectAttributes redirectAttributes,
                                              @ModelAttribute MetadataTemplateForm template)
            throws DataGridConnectionRefusedException {
        boolean templatesAppliedSuccessfully = applyTemplatesToPath(template);
        redirectAttributes.addFlashAttribute("templatesAppliedSuccessfully", templatesAppliedSuccessfully);
        return "redirect:/collections/";
    }

    private boolean applyTemplatesToPath(MetadataTemplateForm template) throws DataGridConnectionRefusedException {
        boolean allMetadataAdded = true;
        List<String> attributes = template.getAvuAttributes();
        List<String> values = template.getAvuValues();
        List<String> units = template.getAvuUnits();

        if (attributes == null || values == null || units == null) {
            return false;
        }

        for (int i = 0; i < attributes.size(); i++) {
            String attr = attributes.isEmpty() ? "" : attributes.get(i);
            String val = values.isEmpty() ? "" : values.get(i);
            String unit = units.isEmpty() ? "" : units.get(i);
            for (String path : template.getPaths()) {
                boolean isMetadadaAdded = metadataService.addMetadataToPath(path, attr, val, unit);
                if (!isMetadadaAdded) {
                    allMetadataAdded = false;
                }
            }
        }

        return allMetadataAdded;
    }

    /*
     * ****************************************************************************
     * ************************ USER COLLECTION CONTROLLER ************************
     * ****************************************************************************
     */

    /**
     * Responds the collections/home request
     *
     * @param model
     * @return the collection management template
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "/home/")
    public String homeCollection(Model model) throws DataGridException {
        // cleaning session variables
        sourcePaths.clear();
        currentPath = cs.getHomeDirectyForCurrentUser();
        parentPath = currentPath;
        return "redirect:/collections/";
    }

    /**
     * Responds the collections/public request
     *
     * @param model
     * @return the collection management template
     */
    @RequestMapping(value = "/public/")
    public String publicCollection(Model model) throws DataGridException {
        // cleaning session variables
        sourcePaths.clear();

        currentPath = cs.getHomeDirectyForPublic();
        parentPath = currentPath;

        model.addAttribute("publicPath", currentPath);
        model.addAttribute("currentPath", currentPath);
        model.addAttribute("parentPath", parentPath);
        model.addAttribute("homePath", cs.getHomeDirectyForCurrentUser());
        model.addAttribute("resources", resourceService.findAll());

        return "collections/collectionManagement";
    }

    /**
     * Responds the collections/trash request
     *
     * @param model
     * @return the collection management template
     * @throws DataGridException
     */
    @RequestMapping(value = "/trash/")
    public String trashCollection(Model model) throws DataGridException {
        // cleaning session variables
        sourcePaths.clear();

        if(userTrashPath == null || userTrashPath.equals("")){
            userTrashPath = String.format("/%s/trash/home/%s", irodsServices.getCurrentUserZone(), irodsServices.getCurrentUser());
        }
        currentPath = userTrashPath;
        parentPath = currentPath;

        model.addAttribute("currentPath", currentPath);
        model.addAttribute("parentPath", parentPath);
        model.addAttribute("publicPath", cs.getHomeDirectyForPublic());
        model.addAttribute("homePath", cs.getHomeDirectyForCurrentUser());
        model.addAttribute("resources", resourceService.findAll());

        return "collections/collectionManagement";
    }

    @RequestMapping(value = "/getBreadCrumbForObject/")
    public String getBreadCrumbForObject(Model model, @RequestParam("path") String path) throws DataGridConnectionRefusedException {
        if (path.isEmpty()) {
            path = currentPath;
        }
        else {
            if (path.endsWith("/") && path.compareTo("/") != 0) {
                path = path.substring(0, path.length() - 1);
            }
            if (!path.equals(currentPath) && (collectionHistoryBack.isEmpty() || !currentPath.equals(collectionHistoryBack.peek()))) {
                while (collectionHistoryBack.size() >= MAX_HISTORY_SIZE) {
                    collectionHistoryBack.remove(0);
                }
                collectionHistoryBack.push(currentPath);
                if (!collectionHistoryForward.isEmpty()) {
                    collectionHistoryForward.clear();
                }
            }
            currentPath = path;
        }

        setBreadcrumbToModel(model, path);
        return "collections/collectionsBreadCrumb";
    }

    /*
     * *****************************************************************************
     * ******************************** VALIDATION *********************************
     * *****************************************************************************
     */

    /**
     * Validates a collection name in iRODS
     *
     * @return True, if the collection name can be used. False, otherwise.
     * @throws DataGridConnectionRefusedException
     */
    @ResponseBody
    @RequestMapping(value = "isValidCollectionName/{newObjectName}/", method = RequestMethod.GET, produces = { "text/plain" })
    public String isValidCollectionName(@PathVariable String newObjectName) throws DataGridException {
        String rc = "true";
        String newPath = String.format("%s/%s", currentPath, newObjectName);

        try {
            cs.findByName(newPath);
            rc = "false";
        }
        catch (DataGridException e) {
            logger.debug("Path {} does not exist. Executing modification", newPath, e);
        }
        return rc;
    }

    /*
     * *************************************************************************
     * ******************************** UTILS **********************************
     * *************************************************************************
     */

    /**
     * Finds all collections and data objects existing under a certain path
     *
     * @param request contains all parameters in a map, we can use it to get all parameters passed in request
     * @return json with collections and data objects
     * @throws DataGridConnectionRefusedException
     */
    @RequestMapping(value = "getPaginatedJSONObjs/")
    @ResponseBody
    public String getPaginatedJSONObjs(HttpServletRequest request) throws DataGridConnectionRefusedException {
        List<DataGridCollectionAndDataObject> dataGridCollectionAndDataObjects;

        int draw = Integer.parseInt(request.getParameter("draw"));
        int start = Integer.parseInt(request.getParameter("start"));
        int length = Integer.parseInt(request.getParameter("length"));
        String searchString = request.getParameter("search[value]");
        int orderColumn = Integer.parseInt(request.getParameter("order[0][column]"));
        String orderDir = request.getParameter("order[0][dir]");
        boolean deployRule = request.getParameter("rulesdeployment") != null;

        // Pagination context to get the sequence number for the listed items
        DataGridPageContext pageContext = new DataGridPageContext();

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> jsonResponse = new HashMap<String, Object>();
        jsonResponse.put("draw", String.valueOf(draw));
        jsonResponse.put("recordsTotal", String.valueOf(1));
        jsonResponse.put("recordsFiltered", String.valueOf(0));
        jsonResponse.put("data", new ArrayList<String>());
        String jsonString = "";


        try {
            String path = currentPath;
            if(deployRule) {
                path = ruleDeploymentService.getRuleCachePath();
            }

            Double startPage = Math.floor(start / length) + 1;
            dataGridCollectionAndDataObjects =
                    cs.getSubCollectionsAndDataObjetsUnderPathThatMatchSearchTextPaginated(path, searchString,
                            startPage.intValue(), length, orderColumn, orderDir, pageContext);
            totalObjsForCurrentSearch = pageContext.getTotalNumberOfItems();
            totalObjsForCurrentPath = pageContext.getTotalNumberOfItems();

            jsonResponse.put("recordsTotal", String.valueOf(totalObjsForCurrentPath));
            jsonResponse.put("recordsFiltered", String.valueOf(totalObjsForCurrentSearch));
            jsonResponse.put("data", dataGridCollectionAndDataObjects);
        }
        catch (DataGridConnectionRefusedException e) {
            throw e;
        }
        catch (Exception e) {
            logger.error("Could not get collections/data objs under path {}: {}", currentPath, e.getMessage());
        }

        try {
            jsonString = mapper.writeValueAsString(jsonResponse);
        }
        catch (JsonProcessingException e) {
            logger.error("Could not parse hashmap in collections to json: {}", e.getMessage());
        }

        return jsonString;
    }

    /**
     * @return the sourcePaths
     */
    public List<String> getSourcePaths() {
        return sourcePaths;
    }

    /**
     * @return the currentPath
     */
    public String getCurrentPath() {
        return currentPath;
    }

    public String getParentPath() {
        return parentPath;
    }
    
    /**
     * Removes a path from the user's navigation history
     * @param path
     * 			path to be removed
     */
    public void removePathFromHistory(String path) {
    	if (path == null || path.isEmpty()) {
    		return;
    	}
    	
    	collectionHistoryBack.remove(path);
    	collectionHistoryForward.remove(path);
    }

    /*
     * **************************************************************************
     * **************************** PRIVATE METHODS *****************************
     * **************************************************************************
     */

    /**
     * Sets the current path and parent path based on a given path.
     *
     * @param path
     *            new path to update current path and parent path
     */
    private void assignNewValuesToCurrentAndParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        currentPath = path;
        parentPath = currentPath.substring(0, currentPath.lastIndexOf("/") + 1);
    }

    /**
     * Creates the breadcrumb based on a given path.
     *
     * @param model Model attribute to set variables to be used in the view
     * @param path path that will be displayed in the breadcrumb
     */
    private void setBreadcrumbToModel(Model model, String path) {
        DataGridCollectionAndDataObject obj;
        try {
            obj = cs.findByName(path);
        }
        catch (DataGridException e) {
            obj = new DataGridCollectionAndDataObject();
            obj.setPath(path);
            obj.setCollection(false);
            obj.setParentPath(path.substring(0, path.lastIndexOf("/") + 1));
            obj.setName(path.substring(path.lastIndexOf("/") + 1, path.length()));
            logger.error("Could not find DataGridCollectionAndDataObject by path: {}", e.getMessage());
        }

        setBreadcrumbToModel(model, obj);
    }

    /**
     * Creates the breadcrumb based on a given path.
     *
     * @param model Model attribute to set variables to be used in the view
     * @param obj {@code DataGridCollectionAndDataObject} object
     */
    private void setBreadcrumbToModel(Model model, DataGridCollectionAndDataObject obj) {
        ArrayList<String> listHistoryBack = new ArrayList<String>(collectionHistoryBack);
        Collections.reverse(listHistoryBack);

        DataGridUser user = loggedUserUtils.getLoggedDataGridUser();
        boolean isPathFavorite = favoritesService.isPathFavoriteForUser(user, obj.getPath());

        model.addAttribute("starredPath", isPathFavorite);
        model.addAttribute("collectionPastHistory", listHistoryBack);
        model.addAttribute("collectionPastHistoryEmpty", collectionHistoryBack.isEmpty());
        model.addAttribute("collectionForwardHistory", collectionHistoryForward);
        model.addAttribute("collectionForwardHistoryEmpty", collectionHistoryForward.isEmpty());
        model.addAttribute("collectionForwardHistory", collectionHistoryForward);
        model.addAttribute("collectionAndDataObject", obj);
        model.addAttribute("breadcrumb", new DataGridBreadcrumb(obj.getPath()));
        model.addAttribute("homeCollectionName", irodsServices.getCurrentUser());
    }

    /**
     * Finds all collections and data objects existing under a certain path
     *
     * @param model
     * @param path path to get all directories and data objects from
     * @return collections browser template that renders all items of certain path (parent)
     * @throws DataGridConnectionRefusedException if Metalnx cannot connect to the grid.
     */
    private String getCollBrowserView(Model model, String path) throws DataGridException {
        if(cs.isPathValid(path)) {
            if (path.endsWith("/") && path.compareTo("/") != 0) {
                path = path.substring(0, path.length() - 1);
            }
            currentPath = path;
        }
        else {
            model.addAttribute("invalidPath", path);
            path = currentPath;
        }

        DataGridUser user = loggedUserUtils.getLoggedDataGridUser();
        DataGridCollectionAndDataObject dataGridObj = cs.findByName(path);

        if (dataGridObj.isDataObject()) {
            dataGridObj.setChecksum(cs.getChecksum(path));
            dataGridObj.setNumberOfReplicas(cs.getTotalNumberOfReplsForDataObject(path));
            dataGridObj.setReplicaNumber(String.valueOf(cs.getReplicationNumber(path)));
        }

        permissionsService.resolveMostPermissiveAccessForUser(dataGridObj, user);

        if(zoneTrashPath == null || zoneTrashPath.isEmpty()){
            zoneTrashPath = String.format("/%s/trash", irodsServices.getCurrentUserZone());
        }

        CollectionOrDataObjectForm collectionForm = new CollectionOrDataObjectForm();
        collectionForm.setInheritOption(cs.getInheritanceOptionForCollection(currentPath));

        String permissionType = cs.getPermissionsForPath(path);
        boolean isPermissionOwn = "own".equals(permissionType);
        boolean isTrash = path.contains(zoneTrashPath) && (isPermissionOwn || user.isAdmin());
        boolean inheritanceDisabled = !isPermissionOwn && collectionForm.getInheritOption();

        model.addAttribute("collectionAndDataObject", dataGridObj);
        model.addAttribute("isTrash", isTrash);
        model.addAttribute("permissionType", permissionType);
        model.addAttribute("currentPath", currentPath);
        model.addAttribute("isCurrentPathCollection", cs.isCollection(path));
        model.addAttribute("user", user);
        model.addAttribute("trashColl", cs.getTrashForPath(currentPath));
        model.addAttribute("collection", collectionForm);
        model.addAttribute("inheritanceDisabled", inheritanceDisabled);
        model.addAttribute("requestMapping", "/collections/add/action/");
        model.addAttribute("parentPath", parentPath);

        setBreadcrumbToModel(model, dataGridObj);

        return "collections/collectionsBrowser";
    }

    /**
     * Adds a given path to the list of paths visited by the user
     * @param path path to a collection or data object to be added to history
     */
    private void addPathToHistory(String path) {
        if (path.equals(currentPath)) return;

        while (collectionHistoryBack.size() >= MAX_HISTORY_SIZE) {
            collectionHistoryBack.remove(0);
        }

        collectionHistoryBack.push(currentPath);

        if (!collectionHistoryForward.isEmpty()) collectionHistoryForward.clear();
    }
}
