/**<pre> 
 *==========================================================================
 *
 * Copyright: (C) IBM Corporation 2013 -- IBM Internal Use Only
 *
 *==========================================================================
 *
 *    FILE: ACLService.java
 *    CREATOR:Waqar Malik
 *    DEPT: GBS PAK
 *    DATE: 19/07/2013
 *
 *-PURPOSE-----------------------------------------------------------------
 * 
 *--------------------------------------------------------------------------
 *
 *
 *-CHANGE LOG--------------------------------------------------------------
 * 19/07/2013Waqar Malik Initial coding.
 *==========================================================================
 * </pre> */

package com.gps.service;

import com.gps.exceptions.GPSException;
import com.gps.vo.GpsUser;

import java.util.List;

/**
 * This class provides interface for ACL Service.
 *  
 * @authorWaqar Malik
 */
public interface GpsUserService {


	List<GpsUser> getEmails() throws GPSException;
	
	

	GpsUser getUserById(Integer userId) throws GPSException;

	GpsUser getUserByIntranetId(String intranetIn) throws GPSException;

	public void addUser(GpsUser dbUser);

}
