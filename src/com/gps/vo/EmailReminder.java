package com.gps.vo;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


/**
 * The persistent class for the EMAIL_REMINDERS database table.
 * 
 */
@Entity
@Table(name="EMAIL_REMINDERS")
@NamedQuery(name="EmailReminder.findAll", query="SELECT e FROM EmailReminder e")
public class EmailReminder implements Serializable {
	private static final long serialVersionUID = 1L;
	private int emailReminderId;
    private String questionnaireId;
    private String questionnaireType;
    private String toEmail;
    private String ccEmail;
    private Date startDate;
    private Date endDate;
    private String reminderType;
    private String isEnabled;
	private Integer numberOfTimesSent;

	public EmailReminder() {
	}


	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="EMAIL_REMINDERS_ID")
	public int getEmailReminderId() {
		return this.emailReminderId;
	}

	public void setEmailReminderId(int emailReminderId) {
		this.emailReminderId = emailReminderId;
	}

	@Column(name="CC")
	public String getCcEmail() {
		return this.ccEmail;
	}

	public void setCcEmail(String ccEmail) {
		this.ccEmail = ccEmail;
	}

	@Column(name="REMINDER_TYPE")
	public String getReminderType() {
		return this.reminderType;
	}

	public void setReminderType(String reminderType) {
		this.reminderType = reminderType;
	}

	@Column(name="IS_ENABLED")
	public String getIsEnabled() {
		return this.isEnabled;
	}

	public void setIsEnabled(String isEnabled) {
		this.isEnabled = isEnabled;
	}


    @Column(name="QUESTIONARE_ID")
	public String getQuestionnaireId() {
		return this.questionnaireId;
	}

	public void setQuestionnaireId(String questionnaireId) {
		this.questionnaireId = questionnaireId;
	}

	@Column(name="TO")
	public String getToEmail() {
		return this.toEmail;
	}

	public void setToEmail(String toEmail) {
		this.toEmail = toEmail;
	}


    @Column(name="QUESTIONARE_TYPE")
    public String getQuestionnaireType() {
        return questionnaireType;
    }

    public void setQuestionnaireType(String questionnaireType) {
        this.questionnaireType = questionnaireType;
    }

    @Column(name="START_DATE")
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Column(name="END_DATE")
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

	@Column(name="NUMBER_OF_TIMES_SENT")
	public Integer getNumberOfTimesSent() {
		return numberOfTimesSent;
	}

	public void setNumberOfTimesSent(Integer numberOfTimesSent) {
		this.numberOfTimesSent = numberOfTimesSent;
	}
}