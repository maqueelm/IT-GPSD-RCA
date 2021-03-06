package com.gps.service.impl;

import com.gps.dao.EmailTemplateDao;
import com.gps.dao.RcaCoordinatorDao;
import com.gps.dao.RcaEditorDao;
import com.gps.service.GpsUserService;
import com.gps.service.RcaActionNotificationService;
import com.gps.service.RcaActionService;
import com.gps.util.BluePages;
import com.gps.util.GPSMailer;
import com.gps.util.StringUtils;
import com.gps.vo.*;
import com.gps.vo.helper.ActionHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by Waqar Malik on 23-12-2014.
 */
@Service
public class RcaActionNotificationServiceImpl implements RcaActionNotificationService {


    @Autowired
    EmailTemplateDao emailTemplateDao;

    @Autowired
    GPSMailer gpsMailer;

    @Autowired
    GpsUserService gpsUserService;

    @Autowired
    RcaEditorDao rcaEditorDao;

    @Autowired
    RcaCoordinatorDao rcaCoordinatorDao;

    @Autowired
    RcaActionService rcaActionService;


    @Value(value = "#{'${initiate.rca.notification}'}")
    private String initiateRcaNotification;

    private static Logger log = Logger.getLogger(RcaActionNotificationServiceImpl.class);

    @Override
    public void sendRcaActionClosedNotification(RcaAction rcaAction, ActionHelper actionHelper) {
        RcaAction dbRcaAction = rcaActionService.getRcaActionByActionNumber(rcaAction.getActionNumber());
        try {
            log.info("notification is enabled.");
            EmailTemplate emailTemplate = emailTemplateDao.getEmailTemplateByName("RCA_ACTION_CLOSED");
            Hashtable<String, String> keys = getValueMap(dbRcaAction, actionHelper);
            if (emailTemplate != null) {
                String to = getActionOwnerEmail(dbRcaAction);
                String body = StringUtils.replaceAll(emailTemplate.getBody(), keys);
                String subject = StringUtils.replaceAll(emailTemplate.getSubject(), keys);
                String cc = getToEmail(emailTemplate.getCcEmail(), dbRcaAction.getRca());
                String bcc = getToEmail(emailTemplate.getBccEmail(), dbRcaAction.getRca());
                List<String> recipients = Arrays.asList(to);
                List<String> ccList = Arrays.asList(cc.split(","));
                gpsMailer.sendEmail(emailTemplate.getFromAlias(), subject, body, recipients, ccList);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }

    @Override
    public void sendRcaActionReAssignedNotification(String newAssigneeEmail, RcaAction dbRcaAction, ActionHelper actionHelper) {
         try {
            log.info("notification is enabled.");
            EmailTemplate emailTemplate = emailTemplateDao.getEmailTemplateByName("RCA_ACTION_RE_ASSIGNED");
            Hashtable<String, String> keys = getValueMap(dbRcaAction, actionHelper);
            if (emailTemplate != null) {
                String to = newAssigneeEmail;
                String body = StringUtils.replaceAll(emailTemplate.getBody(), keys);
                String subject = StringUtils.replaceAll(emailTemplate.getSubject(), keys);
                String cc = getToEmail(emailTemplate.getCcEmail(), dbRcaAction.getRca());
                String bcc = getToEmail(emailTemplate.getBccEmail(), dbRcaAction.getRca());
                List<String> recipients = Arrays.asList(to);
                List<String> ccList = Arrays.asList(cc.split(","));
                gpsMailer.sendEmail(emailTemplate.getFromAlias(), subject, body, recipients, ccList);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void sendRcaActionCreatedNotification(String assigneeEmail, RcaAction dbRcaAction) {
        try {
            log.info("notification is enabled.");
            EmailTemplate emailTemplate = emailTemplateDao.getEmailTemplateByName("RCA_ACTION_CREATED");
            dbRcaAction.setAssignedTo(BluePages.getNotesIdFromIntranetId(assigneeEmail));
            Hashtable<String, String> keys = getValueMap(dbRcaAction, null);
            if (emailTemplate != null) {
                String to = assigneeEmail;
                String body = StringUtils.replaceAll(emailTemplate.getBody(), keys);
                String subject = StringUtils.replaceAll(emailTemplate.getSubject(), keys);
                String cc = getToEmail(emailTemplate.getCcEmail(), dbRcaAction.getRca());
                String bcc = getToEmail(emailTemplate.getBccEmail(), dbRcaAction.getRca());
                List<String> recipients = Arrays.asList(to);

                List<String> ccList = null;
                if(StringUtils.isNotBlank(cc)){
                   ccList =  Arrays.asList(cc.split(","));
                }
                gpsMailer.sendEmail(emailTemplate.getFromAlias(), subject, body, recipients, ccList);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }


    private String getToEmail(String whom, Rca rca) {
        StringBuilder toList = new StringBuilder();
        if (StringUtils.isNullOrEmpty(whom)) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(whom, ",");
        if (st.countTokens() > 1) {
            while (st.hasMoreTokens()) {
                String recipientType = st.nextToken();
                if (recipientType.equalsIgnoreCase("RCA Owner")) {
                    String owner = rca.getRcaOwner();
                    if (!StringUtils.isNullOrEmpty(owner)) {
                        toList.append(owner.trim());
                        toList.append(",");
                    }
                }
                if (recipientType.equalsIgnoreCase("RCA Delegate")) {
                    String delegate = rca.getRcaDelegate();
                    if (!StringUtils.isNullOrEmpty(delegate)) {
                        toList.append(delegate.trim());
                    }
                }
                if (recipientType.equalsIgnoreCase("RCA Editors")) {
                    List<RcaEditor> rcaEditors = rcaEditorDao.getRcaEditorsByRcaId(rca.getRcaId());
                    if (CollectionUtils.isNotEmpty(rcaEditors)) {
                        for (RcaEditor rcaEditor : rcaEditors) {
                            if (toList.toString().indexOf(rcaEditor.getGpsUser().getEmail()) < 0) {
                                toList.append(",");
                                toList.append(rcaEditor.getGpsUser().getEmail().trim());
                            }
                        }
                    }
                }
                if (recipientType.equalsIgnoreCase("RCA Dpe")) {
                    try {
                        GpsUser user = gpsUserService.getUserById(rca.getRcaDpeId());
                        if (user != null) {
                            toList.append(",");
                            toList.append(user.getEmail());
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
                if (recipientType.equalsIgnoreCase("RCA Coordinator")) {
                    try {
                        String rcaCoordinator = rca.getRcaCoordinatorId();
                        if (StringUtils.isNotBlank(rcaCoordinator)) {
                            toList.append(",");
                            toList.append(rcaCoordinator);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }

            }
        } else {
            if (whom.equalsIgnoreCase("RCA Dpe")) {
                try {
                    GpsUser user = gpsUserService.getUserById(rca.getRcaDpeId());
                    if (user != null) {
                        toList.append(user.getEmail());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            if (whom.equalsIgnoreCase("RCA Editors")) {
                List<RcaEditor> rcaEditors = rcaEditorDao.getRcaEditorsByRcaId(rca.getRcaId());
                if (CollectionUtils.isNotEmpty(rcaEditors)) {
                    for (RcaEditor rcaEditor : rcaEditors) {
                        if (toList.toString().indexOf(rcaEditor.getGpsUser().getEmail()) < 0) {
                            toList.append(",");
                            toList.append(rcaEditor.getGpsUser().getEmail().trim());
                        }
                    }
                }
            }


        }

        return toList.toString();
    }

    private String getActionOwnerEmail(RcaAction rcaAction) throws Exception {
        String actionOwnerEmail = "";
        if (org.apache.commons.lang.StringUtils.isNotBlank(rcaAction.getAssignedTo())) {
            if (BluePages.isNotesID(rcaAction.getAssignedTo())) {
                actionOwnerEmail = BluePages.getIntranetIdFromNotesId(rcaAction.getAssignedTo());
            } else {
                actionOwnerEmail = rcaAction.getAssignedTo();
            }
        }

        return actionOwnerEmail;
    }

    private Hashtable<String, String> getValueMap(RcaAction rcaAction, ActionHelper actionHelper) {
        Hashtable<String, String> keys = new Hashtable<String, String>();
        keys.put("rcaNumber", rcaAction.getRca().getRcaNumber());
        keys.put("contractName", rcaAction.getRca().getContract().getTitle());
        keys.put("contractId", String.valueOf(rcaAction.getRca().getContract().getContractId()));
        keys.put("actionCloseDate", String.valueOf(rcaAction.getCloseDate()));
        keys.put("actionItemNo", String.valueOf(rcaAction.getActionNumber()));
        keys.put("assignedTo", rcaAction.getAssignedTo());
        if(actionHelper != null) {
            keys.put("resolutionDesc", actionHelper.getResolutionDescription());
        }

        return keys;
    }
}
