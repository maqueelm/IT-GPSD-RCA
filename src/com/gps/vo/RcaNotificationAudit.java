package com.gps.vo;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/*
 * The persistent class for the RCA_EVENT_DEATILS database table.
*/


@Entity
@Table(name="RCA_NOTIFICATION_AUDIT")
@NamedQuery(name="RcaNotificationAudit.findAll", query="SELECT r FROM RcaNotificationAudit r")
public class RcaNotificationAudit implements Serializable {

	private static final long serialVersionUID = 4973474968432821117L;
	private int rcaId;
    private String isOwnerNotificationSent;
    private String isDelegateNotificationSent;
    private String isDpeApprovalRequestSent;
    private String isCoordinatorApprovalRequestSent;
    private Timestamp ownerNotificationSentDate;
    private Timestamp delegateNotificationSentDate;
    private Timestamp dpeApprovalRequestSentDate;
    private Timestamp coordinatorApprovalRequestSentDate;

    private Rca rca;

	public RcaNotificationAudit() {
	}

    @Id
	@Column(name="RCA_ID")
	public int getRcaId() {
		return this.rcaId;
	}

	public void setRcaId(int rcaId) {
		this.rcaId = rcaId;
	}

    @Column(name="IS_OWNER_NOTIFICATION_SENT")
    public String getIsOwnerNotificationSent() {
        return isOwnerNotificationSent;
    }

    public void setIsOwnerNotificationSent(String isOwnerNotificationSent) {
        this.isOwnerNotificationSent = isOwnerNotificationSent;
    }

    @Column(name="IS_DELEGATE_NOTIFICATION_SENT")
    public String getIsDelegateNotificationSent() {
        return isDelegateNotificationSent;
    }

    public void setIsDelegateNotificationSent(String isDelegateNotificationSent) {
        this.isDelegateNotificationSent = isDelegateNotificationSent;
    }

    @Column(name="IS_DPE_APPROVAL_REQUEST_SENT")
    public String getIsDpeApprovalRequestSent() {
        return isDpeApprovalRequestSent;
    }

    public void setIsDpeApprovalRequestSent(String isDpeApprovalRequestSent) {
        this.isDpeApprovalRequestSent = isDpeApprovalRequestSent;
    }

	//bi-directional one-to-one association to Rca
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="RCA_ID")
	public Rca getRca() {
		return this.rca;
	}

	public void setRca(Rca rca) {
		this.rca = rca;
	}

    @Column(name="OWNER_NOTIFICATION_SENT_DATE")
    public Timestamp getOwnerNotificationSentDate() {
        return ownerNotificationSentDate;
    }

    public void setOwnerNotificationSentDate(Timestamp ownerNotificationSentDate) {
        this.ownerNotificationSentDate = ownerNotificationSentDate;
    }

    @Column(name="DELEGATE_NOTIFICATION_SENT_DATE")
    public Timestamp getDelegateNotificationSentDate() {
        return delegateNotificationSentDate;
    }

    public void setDelegateNotificationSentDate(Timestamp delegateNotificationSentDate) {
        this.delegateNotificationSentDate = delegateNotificationSentDate;
    }


    @Column(name="DPE_APPROVAL_REQUEST_SENT_DATE")
    public Timestamp getDpeApprovalRequestSentDate() {
        return dpeApprovalRequestSentDate;
    }

    public void setDpeApprovalRequestSentDate(Timestamp dpeApprovalRequestSentDate) {
        this.dpeApprovalRequestSentDate = dpeApprovalRequestSentDate;
    }

    @Column(name="COORDINATOR_APPROVAL_REQUEST_SENT_DATE")
    public Timestamp getCoordinatorApprovalRequestSentDate() {
        return coordinatorApprovalRequestSentDate;
    }

    public void setCoordinatorApprovalRequestSentDate(Timestamp coordinatorApprovalRequestSentDate) {
        this.coordinatorApprovalRequestSentDate = coordinatorApprovalRequestSentDate;
    }

    @Column(name="IS_COORDINATOR_APPROVAL_REQUEST_SENT")
    public String getIsCoordinatorApprovalRequestSent() {
        return isCoordinatorApprovalRequestSent;
    }

    public void setIsCoordinatorApprovalRequestSent(String isCoordinatorApprovalRequestSent) {
        this.isCoordinatorApprovalRequestSent = isCoordinatorApprovalRequestSent;
    }
}
