package com.gps.vo;

import javax.persistence.*;
import java.io.Serializable;


/**
 * The persistent class for the ACCESS_CONTROL_LIST database table.
 * 
 */
@Entity
@Table(name="ACCESS_CONTROL_LIST")
@NamedQuery(name="AccessControlList.findAll", query="SELECT a FROM AccessControlList a")
public class AccessControlList implements Serializable {
	private static final long serialVersionUID = 1L;
	private Integer aclId;
	private String activeAccessLevel;
	private String approvedAccessLevel;
	private String awaitingAccessLevel;
	private Integer customer;
	private String formType;
	private Contract contract;
	private GpsUser gpsUser;

	@Transient
	public String toCsv(){
		StringBuilder builder = new StringBuilder();
		builder.append("Id=").append(aclId);
		builder.append(" {").append(activeAccessLevel).append(", ").append(approvedAccessLevel).append("}");
		builder.append(", formType="+formType);
		return builder.toString();
	}


	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="ACL_ID")
	public Integer getAclId() {
		return this.aclId;
	}

	public void setAclId(Integer aclId) {
		this.aclId = aclId;
	}


	@Column(name="ACTIVE_ACCESS_LEVEL")
	public String getActiveAccessLevel() {
		return this.activeAccessLevel;
	}

	public void setActiveAccessLevel(String activeAccessLevel) {
		this.activeAccessLevel = activeAccessLevel;
	}


	@Column(name="APPROVED_ACCESS_LEVEL")
	public String getApprovedAccessLevel() {
		return this.approvedAccessLevel;
	}

	public void setApprovedAccessLevel(String approvedAccessLevel) {
		this.approvedAccessLevel = approvedAccessLevel;
	}


	@Column(name="AWAITING_ACCESS_LEVEL")
	public String getAwaitingAccessLevel() {
		return this.awaitingAccessLevel;
	}

	public void setAwaitingAccessLevel(String awaitingAccessLevel) {
		this.awaitingAccessLevel = awaitingAccessLevel;
	}


	public Integer getCustomer() {
		return this.customer;
	}

	public void setCustomer(Integer customer) {
		this.customer = customer;
	}


	@Column(name="FORM_TYPE")
	public String getFormType() {
		return this.formType;
	}

	public void setFormType(String formType) {
		this.formType = formType;
	}

    //bi-directional many-to-one association to Contract
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="CONTRACT_ID", nullable=true)
    public Contract getContract() {
        return this.contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }




	

	//bi-directional many-to-one association to BpdUser
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="USER_ID", nullable=false)
	public GpsUser getGpsUser() {
		return gpsUser;
	}


	/**
	 * @param gpsUser the gpsUser to set
	 */
	public void setGpsUser(GpsUser gpsUser) {
		this.gpsUser = gpsUser;
	}

}