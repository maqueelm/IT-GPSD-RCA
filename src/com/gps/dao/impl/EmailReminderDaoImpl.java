/**<pre> 
 *==========================================================================
 *
 * Copyright: (C) IBM Corporation 2013 -- IBM Internal Use Only
 *
 *==========================================================================
 *
 *    FILE: EmailTemplateDaoImpl.java
 *    CREATOR:Waqar Malik
 *    DEPT: GBS PAK
 *    DATE: 04/10/2013
 *
 *-PURPOSE-----------------------------------------------------------------
 *
 *--------------------------------------------------------------------------
 *
 *
 *-CHANGE LOG--------------------------------------------------------------
 * 25/09/2013Waqar Malik Initial coding.
 *==========================================================================
 * </pre> */
package com.gps.dao.impl;

import com.gps.dao.EmailReminderDao;
import com.gps.exceptions.GPSException;
import com.gps.vo.EmailReminder;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * This class implements EmailTemplateDao.
 *
 * @authorWaqar Malik
 */
@Repository
@Transactional(propagation= Propagation.REQUIRED)
public class EmailReminderDaoImpl implements EmailReminderDao {
    private static Logger log = Logger.getLogger(EmailReminderDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public void addEmailReminder(EmailReminder emailReminder) {
        try {
            entityManager.persist(emailReminder);
        } catch (Exception e) {
            throw new GPSException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteEmailReminder(EmailReminder emailReminder) {
        try {
            entityManager.remove(entityManager.merge(emailReminder));
        } catch (Exception e) {
            throw new GPSException(e.getMessage(), e);
        }
    }

    @Override
    public EmailReminder findByQuestionnaireIdAndReminderType(Integer questionnaireId, String reminderType) throws GPSException {
        StringBuilder builder = new StringBuilder();
        EmailReminder emailReminder = null;
        try {
            builder.append("SELECT e FROM EmailReminder e ");
            builder.append("WHERE e.questionnaireId=:questionnaireId ");
            builder.append("AND e.reminderType=:reminderType ");
            Query query = entityManager.createQuery(builder.toString());
            query.setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);
            query.setParameter("questionnaireId", questionnaireId.toString());
            query.setParameter("reminderType", reminderType);
            emailReminder = (EmailReminder) query.getSingleResult();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return emailReminder;
    }

    @Override
    public List<EmailReminder> findByReminderType(String reminderType) {
        StringBuilder builder = new StringBuilder();
        List<EmailReminder> emailReminders = null;
        try {
            builder.append("SELECT e FROM EmailReminder e ");
            builder.append("WHERE  e.reminderType=:reminderType ");
            Query query = entityManager.createQuery(builder.toString());
            query.setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);
            query.setParameter("reminderType", reminderType);
            emailReminders = (List<EmailReminder>) query.getResultList();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return emailReminders;


    }

    @Override
    public void updateEmailReminder(EmailReminder emailReminder) {
        try {
            entityManager.merge(emailReminder);
        } catch (Exception e) {
            throw new GPSException(e.getMessage(), e);
        }
    }

    @Override
    public EmailReminder findById(Integer emailReminderId) {
        StringBuilder builder = new StringBuilder();
        EmailReminder emailReminder = null;
        try {
            builder.append("SELECT e FROM EmailReminder e ");
            builder.append("WHERE  e.emailReminderId=:emailReminderId ");
            Query query = entityManager.createQuery(builder.toString());
            query.setHint("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);
            query.setParameter("emailReminderId", emailReminderId);
            emailReminder = (EmailReminder) query.getSingleResult();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return emailReminder;

    }
}
