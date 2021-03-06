
/**<pre> 
 *==========================================================================
 *
 * Copyright: (C) IBM Corporation 2013 -- IBM Internal Use Only
 *
 *==========================================================================
 *
 *    FILE: ACLController.java
 *    CREATOR:Waqar Malik
 *    DEPT: GBS PAK
 *    DATE: 17/07/2013
 *
 *-PURPOSE-----------------------------------------------------------------
 *
 *--------------------------------------------------------------------------
 *
 *
 *-CHANGE LOG--------------------------------------------------------------
 * 17/07/2013Waqar Malik Initial coding.
 *==========================================================================
 * </pre> */
package com.gps.web.controller;

import com.gps.service.ContractService;
import com.gps.service.GpsUserService;
import com.gps.service.UserRoleService;
import com.gps.util.CommonUtil;
import com.gps.util.StringUtils;
import com.gps.util.UserSession;
import com.gps.vo.UserRole;
import com.gps.vo.helper.Constant;
import com.gps.vo.helper.UserRoleForm;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;


/**
 * This class serves as controller for ACLs.
 *
 * @author Waqar Malik
 */

@Controller
@SessionAttributes({"userSession"})
public class UserRoleController {
    public static final String REPORT_ADMIN = "reportAdmin";
    private static Logger log = Logger.getLogger(UserRoleController.class);

    @Autowired
    UserRoleService userRoleService;

    @Autowired
    CommonUtil commonUtil;

    @Autowired
    GpsUserService gpsUserService;

    @Autowired
    ContractService contractService;

    @RequestMapping(value = "/addUserRole.htm", method = RequestMethod.GET)
    public String showACL(@RequestParam("ur_id") Integer urId, @ModelAttribute UserRoleForm userRoleForm, BindingResult result, Map<Object, Object> model) {
        log.debug("loading user role form ................");
        if (urId != null) {
            UserRole userRole = userRoleService.loadUserRole(urId);
            if (userRole != null) {
                userRoleForm.setUserRole(userRole);
            }
        }
        userRoleForm.setContracts(contractService.getAllContracts());
        model.put("referenceData", commonUtil.buildReferenceData());
        model.put("emails", gpsUserService.getEmails());
        model.put("userRoleForm", userRoleForm);
        model.put("userWithRoles", userRoleService.getAllUsersHavingRole());
        model.put("userWithRoles", userRoleService.getAllUsersHavingRole());

        return "addUserRole";
    }


    @RequestMapping(value = "/addSingleRole.htm", method = RequestMethod.GET)
    public
    @ResponseBody
    String addUserRole(@RequestParam("ur_id") Integer urId, @RequestParam("email") String email, @RequestParam("role") String role, @RequestParam("contract_id") Integer contractId) {
        //log.debug("UserRole Command received for user => "+email);
        if (StringUtils.isNotBlank(email)) {
            email = StringUtils.toLowerCase(email);
        }
        String response = "No User Role Entry added in the system. Please check if User Role with these details already exists.";
        boolean outcome = userRoleService.processUserRole(urId, email, contractId, role);
        if (outcome) {
            response = "Successfully added User Role Entry.";
        }

        return response;
    }

    @RequestMapping(value = "/modifyUserRole.htm", method = RequestMethod.GET)
    public String showModifyUserRole(@RequestParam("user_id") Integer userId, @ModelAttribute UserRoleForm userRoleForm, BindingResult result, Map<Object, Object> model) {
        log.debug("loading modify form ................" + userId);
        List<UserRole> userRoles = userRoleService.getUserRolesByUserId(userId);
        formatUserRoleNames(userRoles);
        model.put("referenceData", commonUtil.buildReferenceData());
        model.put("userRole", userRoles);
        model.put("userRoleForm", userRoleForm);
//		model.put("accessMap", CommonUtil.getAcessLevelMap());
        log.debug("showModifyUserRole() response: modifyRole");
        return "modifyUserRole";
    }

    private void formatUserRoleNames(List<UserRole> userRoles) {
        int i = 0;
        if (CollectionUtils.isNotEmpty(userRoles)) {
            for (UserRole userRole : userRoles) {
                if (userRole.getRole().equalsIgnoreCase(REPORT_ADMIN)) {
                    userRoles.get(i).setRole("Report Admin");
                }
                i++;
            }
        }
    }


    @RequestMapping(value = "/dropUserRole.htm", method = RequestMethod.GET)
    public
    @ResponseBody
    String dropUserRole(@RequestParam("ur_id") Integer urId, HttpSession session) {
    	UserSession userSession = (UserSession) session.getAttribute("userSession");
    	Integer loggedInUser = null;
        if (userSession != null) {
        	loggedInUser = userSession.getGpsUser().getUserId();
        }
        log.debug("drop ur request received urId = " + urId);
        String navigation = Constant.FAIL;
        if (urId != null) {
            Boolean droped = userRoleService.deleteUserRole(urId, loggedInUser);
            if (droped.booleanValue()) {
                navigation = Constant.SUCCESS;
            }
        }
        return navigation;
    }


}
