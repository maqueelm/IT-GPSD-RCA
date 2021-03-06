
package com.gps.web.controller;


import com.gps.dao.RcaEmailNotificationSettingDao;
import com.gps.service.*;
import com.gps.util.BluePages;
import com.gps.util.CommonUtil;
import com.gps.util.ConstantDataManager;
import com.gps.util.UserSession;
import com.gps.vo.*;
import com.gps.vo.helper.ActionForm;
import com.gps.vo.helper.ActionHelper;
import com.gps.vo.helper.RcaForm;
import com.gps.web.validator.ActionFormValidator;
import com.gps.web.validator.FileValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Controller
@SessionAttributes({"userSession"})
public class RcaActionController {
    private static final Logger log = Logger.getLogger(RcaActionController.class);
    public static final String SAVE = "save";
    public static final String CLOSE = "close";

    @Autowired
    RcaActionService rcaActionService;

    @Autowired
    GpsUserService gpsUserService;

    @Autowired
    FileValidator fileValidator;

    @Autowired
    ActionFormValidator actionFormValidator;

    @Autowired
    RcaActionNotificationService rcaActionNotificationService;

    @Autowired
    RcaEmailNotificationSettingDao rcaEmailNotificationSettingsDao;

    @Autowired
    UserRoleService userRoleService;

    @Autowired
    RcaUtilService rcaUtilService;


    @RequestMapping(value = "/showRcaAction.htm", method = RequestMethod.GET)
    public String showRcaAction(@RequestParam String actionNumber, Map<Object, Object> model, HttpSession session, HttpServletRequest request) {
        ActionForm actionForm = new ActionForm();
        ActionHelper actionHelper = new ActionHelper();
        try {
            RcaAction rcaAction = rcaActionService.getRcaActionByActionNumber(actionNumber);
            if (StringUtils.isNotBlank(rcaAction.getAssignedTo()) && !BluePages.isNotesID(rcaAction.getAssignedTo())) {
                rcaAction.setAssignedTo(BluePages.getNotesIdFromIntranetId(rcaAction.getAssignedTo()));
            }
            loadSupportingFiles(rcaAction, actionHelper);
            loadRcaActionHelper(rcaAction, actionHelper, session);
            actionForm.setRcaAction(rcaAction);
            actionForm.setActionHelper(actionHelper);

            // load the successfully saved message if exist.
            if (session.getAttribute("savedMessage") != null) {
                String savedMessage = (String) session.getAttribute("savedMessage");
                if (StringUtils.isNotBlank(savedMessage)) {
                    actionForm.setFormSavedMessage(savedMessage);
                }
                session.removeAttribute("savedMessage");
            }

            model.put("actionForm", actionForm);
            model.put("rcaAction", rcaAction);
            setUpDataInForm(actionForm, model, request);


        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "showRcaAction";
    }


    @RequestMapping(value = "/getRcaActionFile.htm", method = RequestMethod.GET)
    @ResponseBody
    public void getRcaActionFile(@RequestParam("rcaActionFileId") Integer rcaActionFileId, HttpServletResponse response) {
        RcaActionSupportingFile actionSupportingFile = rcaActionService.getSupportingFileById(rcaActionFileId);
        if (actionSupportingFile != null) {
            try {
                response.setContentType(actionSupportingFile.getMime());
                response.setContentLength((new Long(actionSupportingFile.getSize()).intValue()));
                response.setHeader("content-Disposition", "attachment; filename=" + actionSupportingFile.getName());// "attachment;filename=test.xls"
                response.getOutputStream().write(actionSupportingFile.getContents());
            } catch (IOException ex) {
                log.info("Error writing file to output stream. Filename was '" + actionSupportingFile.getName() + "'");
                log.error(ex.getMessage(), ex);
            }
        }
    }

    @RequestMapping(value = "/showRcaAction.htm", method = RequestMethod.POST)
    public String saveRcaAction(@RequestParam("file") MultipartFile file, @ModelAttribute ActionForm actionForm, BindingResult result, Map<Object, Object> model, HttpSession session, HttpServletRequest request) {
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        GpsUser loggedInUser = gpsUserService.getUserByIntranetId(userSession.getGpsUser().getEmail());

        // RCA Email Notification Setting
        RcaAction dbRcaAction = rcaActionService.getRcaActionByActionNumber(actionForm.getRcaAction().getActionNumber());
        RcaEmailNotificationSetting rcaEmailSetting = rcaEmailNotificationSettingsDao.getRcaEmailNotificationSettingByContractId(dbRcaAction.getRca().getContract().getContractId());
        if (actionForm.getFormAction().equalsIgnoreCase(SAVE)) {
            // process deleted files
            processDeletedFiles(actionForm);
            if (!processSupportingFile(file, actionForm, result, loggedInUser)) {
                setUpDataInForm(actionForm, model, request);
                return "showRcaAction";
            }
            if (actionForm.getActionHelper().getTargetDateUpdated()) {
                actionFormValidator.validateOnSave(actionForm, result);
                if (result.hasErrors()) {
                    setUpDataInForm(actionForm, model, request);
                    return "showRcaAction";
                }
            }
            String roles = getLoggedInUserRole(actionForm.getRcaAction().getRca(), loggedInUser);
            rcaActionService.saveAction(actionForm.getRcaAction(), actionForm.getActionHelper(), loggedInUser, roles);

            // saved successfully message
            session.setAttribute("savedMessage", "Action Form has been saved successfully.");

            return "redirect:/showRcaAction.htm?actionNumber=" + actionForm.getRcaAction().getActionNumber();
        }
        if (actionForm.getFormAction().equalsIgnoreCase(CLOSE)) {
            // process deleted files
            processDeletedFiles(actionForm);
            if (!processSupportingFile(file, actionForm, result, loggedInUser)) {
                setUpDataInForm(actionForm, model, request);
                return "showRcaAction";
            }

            actionFormValidator.validate(actionForm, result);
            if (!result.hasErrors()) {
                try {
                    String roles = getLoggedInUserRole(actionForm.getRcaAction().getRca(), loggedInUser);
                    rcaActionService.closeAction(actionForm.getRcaAction(), actionForm.getActionHelper(), loggedInUser, roles);
                    if (rcaEmailSetting != null && rcaEmailSetting.getRcaWorkflowNotificationEnabled().equalsIgnoreCase(ConstantDataManager.YES)
                            && rcaEmailSetting.getActionItemClosedNotification().equalsIgnoreCase(ConstantDataManager.YES)) {
                        rcaActionNotificationService.sendRcaActionClosedNotification(actionForm.getRcaAction(), actionForm.getActionHelper());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                setUpDataInForm(actionForm, model, request);
                return "showRcaAction";
            }
        }

        return "redirect:/rcas.htm";
    }

    private boolean processSupportingFile(MultipartFile file, ActionForm actionForm, BindingResult result, GpsUser loggedInUser) {
        if (!file.isEmpty() && file.getSize() > 0L) {
            log.info("uploaded file: " + file.getOriginalFilename() + ", Type = " + file.getContentType() + ", size = " + file.getSize());
            fileValidator.validate(file, result);
            if (!result.hasErrors()) {
                try {
                    rcaActionService.saveFileInDb(file, actionForm.getActionHelper().getFileDescription(), actionForm.getRcaAction(), loggedInUser);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private void processDeletedFiles(ActionForm actionForm) {
        if (CollectionUtils.isNotEmpty(actionForm.getActionHelper().getSupportingFiles())) {
            List<RcaActionSupportingFile> supportingFiles = actionForm.getActionHelper().getSupportingFiles();
            for (RcaActionSupportingFile supportingFile : supportingFiles) {
                RcaActionSupportingFile rcaActionSupportingFile = rcaActionService.getSupportingFileById(supportingFile.getFileId());
                if (supportingFile.getIsDeleted().equalsIgnoreCase("Y")) {
                    rcaActionService.deleteRcaActionSupportFile(rcaActionSupportingFile);
                }
            }
        }
    }


    private void loadRcaActionHelper(RcaAction rcaAction, ActionHelper actionHelper, HttpSession session) throws Exception {
        if (rcaAction.getCreatedBy() != null) {
            GpsUser actionCreator = gpsUserService.getUserById(rcaAction.getCreatedBy());
            if (actionCreator != null) {
                try {
                    actionCreator.setNotesId(BluePages.getNotesIdFromIntranetId(actionCreator.getEmail()));
                    actionHelper.setActionCreatedBy(actionCreator);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        if (rcaAction.getUpdatedBy() != null) {
            GpsUser actionUpdatedBy = gpsUserService.getUserById(rcaAction.getUpdatedBy());
            if (actionUpdatedBy != null) {
                try {
                    actionUpdatedBy.setNotesId(BluePages.getNotesIdFromIntranetId(actionUpdatedBy.getEmail()));
                    actionHelper.setActionUpdatedBy(actionUpdatedBy);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        if (StringUtils.isNotBlank(rcaAction.getRca().getRcaCoordinatorId())) {
            actionHelper.setRcaCoordinator(BluePages.getNotesIdFromIntranetId(rcaAction.getRca().getRcaCoordinatorId()));
        }
        if (rcaAction.getAssignedOn() != null) {
            actionHelper.setAssignedDate(CommonUtil.convertDateToString(rcaAction.getAssignedOn()));
        }
        if (rcaAction.getTargetDate() != null) {
            actionHelper.setTargetDate(CommonUtil.convertDateToString(rcaAction.getTargetDate()));
        }

        // loading history logs
        List<RcaActionHistoryLog> historyLogs = rcaActionService.getHistoryLogsByActionId(rcaAction.getRcaActionId());
        if (CollectionUtils.isNotEmpty(historyLogs)) {
            actionHelper.setRcaActionHistoryLogs(historyLogs);
        }

        // load user roles
        UserSession userSession = (UserSession) session.getAttribute("userSession");
        GpsUser loggedInUser = gpsUserService.getUserByIntranetId(userSession.getGpsUser().getEmail());
        List<String> rolesList = new ArrayList();


        // check for rca owner
        if (loggedInUser.getEmail().equalsIgnoreCase(rcaAction.getRca().getRcaOwner())) {
            rolesList.add("owner");
        }
        // check for rca delegate
        if (loggedInUser.getEmail().equals(rcaAction.getRca().getRcaDelegate())) {
            rolesList.add("delegate");
        }

        // check for rca dpe
        if (rcaAction.getRca().getRcaDpeId() != null) {
            GpsUser dpe = gpsUserService.getUserById(rcaAction.getRca().getRcaDpeId());
            if (dpe != null && dpe.getUserId() == loggedInUser.getUserId()) {
                rolesList.add("dpe");
            }
        }

        // check for rca coordinator
        if (rcaAction.getRca().getRcaCoordinatorId() != null) {
            if (rcaAction.getRca().getRcaCoordinatorId().equalsIgnoreCase(loggedInUser.getEmail())) {
                rolesList.add("coordinator");
            }
        }

        //check if the user is creator
        GpsUser creator = gpsUserService.getUserById(rcaAction.getRca().getCreatedBy());
        if (creator.getEmail().equalsIgnoreCase(loggedInUser.getEmail())) {
            rolesList.add("creator");
        }

        // check if user is reader
        UserRole readerRole = userRoleService.getUserRolesByContractIdAndUserIdAndRole(rcaAction.getRca().getContract().getContractId(), loggedInUser.getUserId(), "reader");
        if (readerRole != null) {
            rolesList.add("reader");
        }

        // check for Contract based roles
        List<UserRole> contractUserRoles = userRoleService.getUserRolesByContractIdAndUserId(rcaAction.getRca().getContract().getContractId(), loggedInUser.getUserId());
        if (CollectionUtils.isNotEmpty(contractUserRoles)) {
            for (UserRole role : contractUserRoles) {
                if (role.getRole().equalsIgnoreCase("owner") && !rolesList.contains("owner")) {
                    rolesList.add("owner");
                }
                if (role.getRole().equalsIgnoreCase("delegate") && !rolesList.contains("delegate")) {
                    rolesList.add("delegate");
                }
                if (role.getRole().equalsIgnoreCase("dpe") && !rolesList.contains("dpe")) {
                    rolesList.add("dpe");
                }
                if (role.getRole().equalsIgnoreCase("editor") && !rolesList.contains("editor")) {
                    rolesList.add("editor");
                }
                if (role.getRole().equalsIgnoreCase("creator") && !rolesList.contains("creator")) {
                    rolesList.add("creator");
                }
            }
        }


        configureGlobalRoles(loggedInUser, rolesList);
        actionHelper.setUserRoles(StringUtils.join(rolesList, ","));

    }

    private void configureGlobalRoles(GpsUser loggedInUser, List<String> rolesList) {
        //check for global coordinator
        if (rcaUtilService.isGlobalCoordinator(loggedInUser.getUserId())) {
            rolesList.add("globalCoordinator");
        }
        //check for global delegate
        if (rcaUtilService.isGlobalDelegate(loggedInUser.getUserId())) {
            rolesList.add("globalDelegate");
        }
        //check for global dpe
        if (rcaUtilService.isGlobalDpe(loggedInUser.getUserId())) {
            rolesList.add("globalDpe");
        }
        //check for global owner
        if (rcaUtilService.isGlobalOwner(loggedInUser.getUserId())) {
            rolesList.add("globalOwner");
        }
        //check for global coordinator or global
        if (rcaUtilService.isGlobalEditor(loggedInUser.getUserId())) {
            rolesList.add("globalEditor");
        }
        //check for global reader
        if (rcaUtilService.isGlobalReader(loggedInUser.getUserId())) {
            rolesList.add("globalReader");
        }
    }

    private void loadSupportingFiles(RcaAction rcaAction, ActionHelper actionHelper) {
        List<RcaActionSupportingFile> rcaFiles = new ArrayList<RcaActionSupportingFile>();
        try {
            List<RcaActionSupportingFile> supportingFiles = rcaActionService.getFilesByRcaActionId(rcaAction.getRcaActionId());
            if (CollectionUtils.isNotEmpty(supportingFiles)) {
                for (RcaActionSupportingFile rcaActionSupportingFile : supportingFiles) {
                    if (rcaActionSupportingFile.getUploadedOn() != null) {
                        rcaActionSupportingFile.setUploadedTime(CommonUtil.convertTimestampToString(rcaActionSupportingFile.getUploadedOn()));
                        if (rcaActionSupportingFile.getUploadedBy() != null) {
                            GpsUser uploadedBy = rcaActionSupportingFile.getUploadedBy();
                            uploadedBy.setNotesId(BluePages.getNotesIdFromIntranetId(rcaActionSupportingFile.getUploadedBy().getEmail()));
                            rcaActionSupportingFile.setUploadedBy(uploadedBy);
                        }
                        rcaFiles.add(rcaActionSupportingFile);
                    }
                }
            }
        } catch (Exception e) {
            //log.error(e.getMessage(),e);
        }
        actionHelper.setSupportingFiles(rcaFiles);
    }

    private String getLoggedInUserRole(Rca rca, GpsUser loggedInUser) {
        StringBuilder roles = new StringBuilder();
        if (loggedInUser.getEmail().equalsIgnoreCase(rca.getRcaOwner())) {
            roles.append("rcaOwner,");
        }
        if (loggedInUser.getEmail().equals(rca.getRcaDelegate())) {
            roles.append("rcaDelegate,");
        }
        if (rca.getRcaDpeId() != null) {
            GpsUser dpe = gpsUserService.getUserById(rca.getRcaDpeId());
            if (dpe != null && dpe.getUserId() == loggedInUser.getUserId()) {
                roles.append("rcaDpe");
            }
        }
        if (org.apache.commons.lang.StringUtils.isNotBlank(roles.toString())) {
            String lastCharStr = roles.toString().substring(roles.length() - 2, roles.length() - 1);
            if (lastCharStr.equals(",")) {
                roles = new StringBuilder(roles.toString().substring(0, roles.length() - 2));
            }
        }
        return roles.toString();

    }

    private void setUpDataInForm(ActionForm actionForm, Map<Object, Object> model, HttpServletRequest request) {
        model.put("basePath", CommonUtil.getBasePath(request));
        model.put("year", CommonUtil.getYearFromDate(actionForm.getRcaAction().getAssignedOn()));
        model.put("month", CommonUtil.getMonthNameFromNumber(CommonUtil.getMonthFromDate(actionForm.getRcaAction().getAssignedOn())));
        model.put("contractId", actionForm.getRcaAction().getRca().getContract().getContractId());
        model.put("contractName", actionForm.getRcaAction().getRca().getContract().getTitle());
    }

    private void configureLoggedInUserRole(ActionForm actionForm, RcaAction rcaAction, GpsUser loggedInUser) {
        List<String> rolesList = new ArrayList();
        List<String> roles = new ArrayList();

        // check for rca owner
        if (loggedInUser.getEmail().equalsIgnoreCase(rcaAction.getRca().getRcaOwner())) {
            rolesList.add("owner");
        }
        // check for rca delegate
        if (loggedInUser.getEmail().equals(rcaAction.getRca().getRcaDelegate())) {
            rolesList.add("delegate");
        }
        // check for rca dpe
        if (rcaAction.getRca().getRcaDpeId() != null) {
            GpsUser dpe = gpsUserService.getUserById(rcaAction.getRca().getRcaDpeId());
            if (dpe != null && dpe.getUserId() == loggedInUser.getUserId()) {
                rolesList.add("dpe");
            }
        }

        // check for rca coordinator
        if (rcaAction.getRca().getRcaCoordinatorId() != null) {
            if (rcaAction.getRca().getRcaCoordinatorId().equalsIgnoreCase(loggedInUser.getEmail())) {
                rolesList.add("coordinator");
            }
        }

        //check if the user is creator
        GpsUser creator = gpsUserService.getUserById(rcaAction.getRca().getCreatedBy());
        if (creator.getEmail().equalsIgnoreCase(loggedInUser.getEmail())) {
            rolesList.add("creator");
        }

        // check if user is reader
        UserRole readerRole = userRoleService.getUserRolesByContractIdAndUserIdAndRole(rcaAction.getRca().getContract().getContractId(), loggedInUser.getUserId(), "reader");
        if (readerRole != null) {
            rolesList.add("reader");
        }

        // check if user is admin
        if (rcaUtilService.isAdmin(loggedInUser.getUserId())) {
            rolesList.add("admin");
        }

        configureGlobalRoles(loggedInUser, rolesList);

        // check for Contract based roles
        List<UserRole> contractUserRoles = userRoleService.getUserRolesByContractIdAndUserId(rcaAction.getRca().getContract().getContractId(), loggedInUser.getUserId());
        if (CollectionUtils.isNotEmpty(contractUserRoles)) {
            for (UserRole role : contractUserRoles) {
                if (role.getRole().equalsIgnoreCase("owner") && !rolesList.contains("owner")) {
                    rolesList.add("owner");
                }
                if (role.getRole().equalsIgnoreCase("delegate") && !rolesList.contains("delegate")) {
                    rolesList.add("delegate");
                }
                if (role.getRole().equalsIgnoreCase("dpe") && !rolesList.contains("dpe")) {
                    rolesList.add("dpe");
                }
                if (role.getRole().equalsIgnoreCase("editor") && !rolesList.contains("editor")) {
                    rolesList.add("editor");
                }
            }
        }


        actionForm.setUserRoles(StringUtils.join(rolesList, ","));

    }


}
