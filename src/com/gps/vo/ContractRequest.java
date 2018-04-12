package com.gps.vo;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


/**
 * The persistent class for the CONTRACT_REQUEST database table.
 * 
 */
@Entity
@Table(name="CONTRACT_REQUEST")
@NamedQuery(name="ContractRequest.findAll", query="SELECT c FROM ContractRequest c")
public class ContractRequest implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 602955893647036608L;
	// Start @Transient Variables
	private String formAction;
	private Integer  contractId;
	private String action;
    // end @Transient Variables
	private Long requestId;
	private Date actionDate;
	private String businessPurpose;
	private String comments;
	private String contractName;
    private String newContractName;
	private String customer;
	private String dataElementRequired;
	private String dataUseInTools;
	private Date dateRequested;
	private String dpescActiveLevel;
	private String dpescApprovedLevel;
	private String dpescEmail;
	private String infoFrequency;
	private String isArchive;
	private String isListed;
	private String justification;
	private String legalName;
	private int month;
	private String pastMethod;
	private String profileActiveLevel;
    //private String profileAwaitingLevel;
	private String profileApprovedLevel;
    private String profileEmail;
	private String rejectionComments;
	private String requestScope;
	private String requestType;
	private String requestedBy;
	private String status;
	private String towerLeadId;
	private long year;
    private String requestedRole;
	private String accessRequestType;

	public ContractRequest() {
	}


	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="REQUEST_ID")
	public Long getRequestId() {
		return this.requestId;
	}

	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}


	@Temporal(TemporalType.DATE)
	@Column(name="ACTION_DATE")
	public Date getActionDate() {
		return this.actionDate;
	}

	public void setActionDate(Date actionDate) {
		this.actionDate = actionDate;
	}


	@Column(name="BUSINESS_PURPOSE")
	public String getBusinessPurpose() {
		return this.businessPurpose;
	}

	public void setBusinessPurpose(String businessPurpose) {
		this.businessPurpose = businessPurpose;
	}


	public String getComments() {
		return this.comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}


	@Column(name="CONTRACT_NAME")
	public String getContractName() {
		return this.contractName;
	}

	public void setContractName(String contractName) {
		this.contractName = contractName;
	}

    @Column(name="NEW_CONTRACT_NAME")
    public String getNewContractName() {
        return newContractName;
    }

    public void setNewContractName(String newContractName) {
        this.newContractName = newContractName;
    }

	public String getCustomer() {
		return this.customer;
	}

	public void setCustomer(String customer) {
		this.customer = customer;
	}


	@Column(name="DATA_ELEMENT_REQUIRED")
	public String getDataElementRequired() {
		return this.dataElementRequired;
	}

	public void setDataElementRequired(String dataElementRequired) {
		this.dataElementRequired = dataElementRequired;
	}


	@Column(name="DATA_USE_IN_TOOLS")
	public String getDataUseInTools() {
		return this.dataUseInTools;
	}

	public void setDataUseInTools(String dataUseInTools) {
		this.dataUseInTools = dataUseInTools;
	}


	@Temporal(TemporalType.DATE)
	@Column(name="DATE_REQUESTED")
	public Date getDateRequested() {
		return this.dateRequested;
	}

	public void setDateRequested(Date dateRequested) {
		this.dateRequested = dateRequested;
	}


	@Column(name="DPESC_ACTIVE_LEVEL")
	public String getDpescActiveLevel() {
		return this.dpescActiveLevel;
	}

	public void setDpescActiveLevel(String dpescActiveLevel) {
		this.dpescActiveLevel = dpescActiveLevel;
	}


	@Column(name="DPESC_APPROVED_LEVEL")
	public String getDpescApprovedLevel() {
		return this.dpescApprovedLevel;
	}

	public void setDpescApprovedLevel(String dpescApprovedLevel) {
		this.dpescApprovedLevel = dpescApprovedLevel;
	}


	@Column(name="DPESC_EMAIL")
	public String getDpescEmail() {
		return this.dpescEmail;
	}

	public void setDpescEmail(String dpescEmail) {
		this.dpescEmail = dpescEmail;
	}


	@Column(name="INFO_FREQUENCY")
	public String getInfoFrequency() {
		return this.infoFrequency;
	}

	public void setInfoFrequency(String infoFrequency) {
		this.infoFrequency = infoFrequency;
	}

	@Column(name="IS_ARCHIVE")
	public String getIsArchive() {
		return this.isArchive;
	}

	public void setIsArchive(String isArchive) {
		this.isArchive = isArchive;
	}


	@Column(name="IS_LISTED")
	public String getIsListed() {
		return this.isListed;
	}

	public void setIsListed(String isListed) {
		this.isListed = isListed;
	}


	public String getJustification() {
		return this.justification;
	}

	public void setJustification(String justification) {
		this.justification = justification;
	}


	@Column(name="LEGAL_NAME")
	public String getLegalName() {
		return this.legalName;
	}

	public void setLegalName(String legalName) {
		this.legalName = legalName;
	}


	@Column(name="\"MONTH\"")
	public int getMonth() {
		return this.month;
	}

	public void setMonth(int month) {
		this.month = month;
	}


	@Column(name="PAST_METHOD")
	public String getPastMethod() {
		return this.pastMethod;
	}

	public void setPastMethod(String pastMethod) {
		this.pastMethod = pastMethod;
	}


	@Column(name="PROFILE_ACTIVE_LEVEL")
	public String getProfileActiveLevel() {
		return this.profileActiveLevel;
	}

	public void setProfileActiveLevel(String profileActiveLevel) {
		this.profileActiveLevel = profileActiveLevel;
	}

 /*   @Column(name="PROFILE_AWAITING_LEVEL")
    public String getProfileAwaitingLevel() {
        return profileAwaitingLevel;
    }

    public void setProfileAwaitingLevel(String profileAwaitingLevel) {
        this.profileAwaitingLevel = profileAwaitingLevel;
    } */

	@Column(name="PROFILE_APPROVED_LEVEL")
	public String getProfileApprovedLevel() {
		return this.profileApprovedLevel;
	}

	public void setProfileApprovedLevel(String profileApprovedLevel) {
		this.profileApprovedLevel = profileApprovedLevel;
	}


	@Column(name="PROFILE_EMAIL")
	public String getProfileEmail() {
		return this.profileEmail;
	}

	public void setProfileEmail(String profileEmail) {
		this.profileEmail = profileEmail;
	}


	@Column(name="REJECTION_COMMENTS")
	public String getRejectionComments() {
		return this.rejectionComments;
	}

	public void setRejectionComments(String rejectionComments) {
		this.rejectionComments = rejectionComments;
	}


	@Column(name="REQUEST_SCOPE")
	public String getRequestScope() {
		return this.requestScope;
	}

	public void setRequestScope(String requestScope) {
		this.requestScope = requestScope;
	}


	@Column(name="REQUEST_TYPE")
	public String getRequestType() {
		return this.requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}


	@Column(name="REQUESTED_BY")
	public String getRequestedBy() {
		return this.requestedBy;
	}

	public void setRequestedBy(String requestedBy) {
		this.requestedBy = requestedBy;
	}


	@Column(name="\"STATUS\"")
	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}


	@Column(name="TOWER_LEAD_ID")
	public String getTowerLeadId() {
		return this.towerLeadId;
	}

	public void setTowerLeadId(String towerLeadId) {
		this.towerLeadId = towerLeadId;
	}


	@Column(name="\"YEAR\"")
	public long getYear() {
		return this.year;
	}

	public void setYear(long year) {
		this.year = year;
	}


    @Column(name="REQUESTED_ROLE")
    public String getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

	@Column(name="ACCESS_REQUEST_TYPE")
	public String getAccessRequestType() {
		return accessRequestType;
	}

	public void setAccessRequestType(String accessRequestType) {
		this.accessRequestType = accessRequestType;
	}


	/**
	 * @return the formAction
	 */
	@Transient 
	public String getFormAction() {
		return formAction;
	}


	/**
	 * @param formAction the formAction to set
	 */
	public void setFormAction(String formAction) {
		this.formAction = formAction;
	}


	/**
	 * @return the contractId
	 */
	@Transient
	public Integer getContractId() {
		return contractId;
	}


	/**
	 * @param contractId the contractId to set
	 */
	public void setContractId(Integer contractId) {
		this.contractId = contractId;
	}

	@Transient
	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

}