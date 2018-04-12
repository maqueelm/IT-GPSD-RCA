<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%
	String path = request.getContextPath();
	String basePath = request.getScheme() + "://"
			+ request.getServerName() + ":" + request.getServerPort()
			+ path + "/";
	System.out.println("showing editContract.jsp....");
%>
<html>
<head profile="w3.ibm.com">
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title></title>
    <link href="http://ciolab.ibm.com/misc/typeahead/builds/facestypeahead-0.4.4.css" rel="stylesheet"></link>
    <link rel="stylesheet" type="text/css" href="css/jquery.datetimepicker.css" />
	<link rel="stylesheet" type="text/css"
		  href="css/style.css"/>

	<style type="text/css">
        .custom-date-style {
            background-color: red !important;
        }
    </style>

	<style type="text/css" media="all">
		.bar-green-dark {
			background: #7a3;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-green-med-dark {
			background: #9c3;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-green-med-dark_cell {
			background: #9c3;
			color: #fff;
			margin: 2px 2px 2px 2px;
			padding: 2px 2px 2px 2px;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-green-med-light {
			background: #bd6;
			color: #000;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-blue-med-dark {
			background: #47b;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-blue-med {
			background: #69c;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-blue-med-light {
			background: #9be;
			color: #fff;
			padding: .2em .2em;
			font-size: 1.0em;
			font-weight: bold;
		}

		.bar-gray-dark {
			background: #666;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-gray-med-dark {
			background: #cccccc;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-gray-med-light {
			background: #ccc;
			color: #000;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-gray-light {
			background: #ddd;
			color: #000;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-blue-black {
			background: #003f69;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-green-black {
			background: #bbbbbb;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-gray-black {
			background: #999999;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-green-black-small {
			background: #aaaaaa;
			color: #fff;
			padding: .2em .2em;
			font-size: 1.0em;
			font-weight: bold;
		}

		.bar-gray-black-small {
			background: #cccccc;
			color: #fff;
			padding: .2em .2em;
			font-size: 1.0em;
		font-

		.lr-mar-10px {
			margin-left: 10px;
			margin-right: 10px;
			margin-top: 10px;
			margin-bottom: 10px;
		}

		.dot-bor-1px {
			border: 2px dotted #CCC
		}

		.sol-bor-1px {
			border: 1px solid #CCC
		}

		.contract-contact-info {
			width: 180px
		}

		.errors {
			color: #ff0000;
			font-style: italic;
		}

		#general_Information span {
			display: block;
			height: 25px;
		}

		#general_Information label {
			width: auto;
			width: 250px;
			font-weight: normal;
		}

		#general_Information tr {
			height: 25px;
		}

		#bcrs span {
			display: block;
			height: 30px;
		}

		#bcrs label {
			width: auto;
			width: 550px;
			font-weight: normal;
		}

		#csst span {
			display: block;
			height: 30px;
		}

		#csst label {
			width: auto;
			width: 550px;
			font-weight: normal;
		}

		.bar-green-black {
			background: #666666;
			color: #fff;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-gray-light {
			background: #eeeeee;
			color: #000;
			padding: .3em .5em;
			font-size: 1.1em;
			font-weight: bold;
		}

		.bar-gray-light-test {
			background: #eee;
			color: #000;
			padding: .1em .1em;
			font-size: 1.0em;
		}

		.errors {
			color: #ff0000;
			font-style: italic;
		}
	</style>

    <script src="js/jquery.js"></script>
    <script src="js/jquery.datetimepicker.js"></script>
    <script src="http://ciolab.ibm.com/misc/typeahead/builds/facestypeahead-0.4.4.js"></script>

    <script>
        dojo.addOnLoad(function(){
            ta1 = FacesTypeAhead.init(
                    dojo.query(".typeahead"),
                    {
                        key: "typeahead_demo;webahead@us.ibm.com",
                        faces: {
                            headerLabel: "People",

                            onclick: function(person) {
                                return person['notes-id'];
                            }
                        }
                    });

        });
    </script>
</head>

<body>
<div id="ibm-pcon">
	<!-- CONTENT_BEGIN -->
	<div id="ibm-content">
		<!-- TITLE_BEGIN -->
		<!-- TITLE_END -->
		<!-- CONTENT_BODY -->
		<div id="ibm-content-body">
			<div id="ibm-content-main">
				<div class="ibm-columns">
					<div class="ibm-col-6-6">
						<div id="contractDetailContainer" class="ibm-container">

							<form:form method="post" commandName="actionForm"
									   cssClass="ibm-column-form" id="actionForm" name="actionForm"
									   enctype="multipart/form-data">

								<form:hidden path="rcaAction.rcaActionId" id="rcaActionId" />
								<form:hidden path="formAction" id="formAction" />
								<form:hidden path="actionHelper.fileDescription"
											 id="fileDescription" />
								<form:hidden path="rcaAction.rca.rcaId" id="rcaId" />
								<form:hidden path="actionHelper.targetDateUpdated"
											 id="targetDateUpdated" />
								<form:hidden path="actionHelper.resolutionDescription"
											 id="resolutionDescription" />
								<form:hidden path="actionHelper.userRoles"
											 id="userRoles" />

								<div id="pii" style="display: none">
									<p style="display: none">The fields indicated with an
										asterisk (*) are required to complete this transaction; other
										fields are optional. If you do not want to provide us with the
										required information, please use the "Back" button on your
										browser to return to the previous page, or close the window or
										browser session that is displaying this page.</p>
								</div>


								<div id="ibm-leadspace-head" class="ibm-alternate">
									<div align="right">
										<a
												href="https://w3-connections.ibm.com/wikis/home?lang=en_US#/wiki/GPS%20Root%20Cause%20Analysis%20%28RCA%29/page/Introduction"
												target="_blank">Help</a>
									</div>
									<div id="ibm-leadspace-body">
										<ul id="ibm-navigation-trail">
											<li><a href="/GPSDRCA/contracts.htm">GPSDRCA</a></li>
											<li><a href="/GPSDRCA/rcas.htm">RCAs/Actions</a></li>
											<li><a
													href="/GPSDRCA/showRcaAction.htm?actionNumber=${rcaAction.actionNumber}">RCA
												Action</a></li>
										</ul>

										<h1 class="ibm-small">${contractId}</em> | ${contractName}</h1>
										<p>
											<em class="ibm-small">Action Form: ${month} - ${year} &nbsp;&nbsp;  Global Client Name: ${contractName}</em>
										</p>


										<br />
										<form:errors path="*" element="div" cssClass="errors"/>
										<span class="errors" align="center">${actionForm.formSavedMessage}</span>
										<br />


										<br /> <span id="requiredFieldExplaination"> Required
											fields are marked with asterisk(<span class="ibm-required">*</span>)and
											a double asterisk (<span class="ibm-required">**</span>)indicates
											conditionally required fields.
										</span>
									</div>
								</div>

								<div id="body" class="ibm-container-body; sol-bor-1px">
									<div id="clientInformationDiv">

										<!-- Start Header Information -->
										<div class="bar-gray-black">Action Item Header
											Information</div>
										<br />

										<div class="lr-mar-10px">
											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">Action
														Item No:</th>
													<th scope="col" width="25%"><form:input
															path="rcaAction.actionNumber" disabled="disabled"
															style="background: #fafafa" size="40" /></th>
													<th scope="col" width="15%"></th>
													<th scope="col" width="15%" style="font-weight: normal;">RCA
														Number:</th>
													<th scope="col" width="30%"><form:input
															path="rcaAction.rca.rcaNumber" disabled="disabled"
															id="rcaNumber" style="background: #fafafa" size="40" /></th>

												</tr>

												</thead>
											</table>

											<table id="created_by" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">Created
														By:</th>
													<th scope="col" width="25%"><form:input
															path="actionHelper.actionCreatedBy.notesId"
															disabled="disabled" style="background: #fafafa"
															size="40" /></th>
													<th scope="col" width="15%"></th>
													<th scope="col" width="15%" style="font-weight: normal;">Last
														Updated By:</th>
													<th scope="col" width="30%"><form:input
															path="actionHelper.actionUpdatedBy.notesId"
															disabled="disabled" id="rcaNumber"
															style="background: #fafafa" size="40" /></th>

												</tr>

												</thead>
											</table>

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">RCA
														Coordinator:</th>
													<th scope="col" width="25%"><form:input
															path="actionHelper.rcaCoordinator" disabled="disabled"
															style="background: #fafafa" size="40" /></th>
													<th scope="col" width="15%"></th>
													<th scope="col" width="15%" style="font-weight: normal;"></th>
													<th scope="col" width="30%"></th>

												</tr>

												</thead>
											</table>
											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">Action
														Status:</th>
													<th scope="col" width="25%"><form:input
															id="actionStatus" path="rcaAction.actionStatus" disabled="disabled"
															style="background: #fafafa" size="20" /></th>
													<th scope="col" width="15%"></th>
													<th scope="col" width="15%" style="font-weight: normal;"></th>
													<th scope="col" width="30%"></th>

												</tr>

												</thead>
											</table>

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">
														<span class="ibm-required">*</span>	% Completed Action Item:</th>
													<th scope="col" width="25%"><form:input
															path="rcaAction.completePercentTxt"
															size="40" maxlength="250"/></th>
													<th scope="col" width="15%"></th>
													<th scope="col" width="15%" style="font-weight: normal;"></th>
													<th scope="col" width="30%"></th>

												</tr>

												</thead>
											</table>

										</div>
										<!-- End Header Information -->


										<!-- Start Criteria & System Location information -->
										<div class="bar-gray-black">Criteria & System Location</div>
										<br />

										<div id="action_item_assignment_div" class="lr-mar-10px">
											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">
														Reason RCA Required:
													</th>
													<th scope="col"><form:input
															path="rcaAction.rca.rcaReason"
															readonly="true" style="background: #fafafa" size="40" /></th>
												</tr>

												</thead>
											</table>
											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">
														System/Server Name:
													</th>
													<th scope="col"><form:input
															path="rcaAction.rca.systemImpacted"
															readonly="true" style="background: #fafafa" size="40" /></th>
												</tr>

												</thead>
											</table>
											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">
														Link to RCA:
													</th>
													<th scope="col">
														<a class="ibm-sso-signin" href="${basePath}editRca.htm?rcaNumber=${rcaAction.rca.rcaNumber}" aria-label="Click to view RCA">Click to view RCA</a>
													</th>
												</tr>

												</thead>
											</table>
										</div>

										<!-- End Criteria & System Location information -->


										<!-- Start Item Description -->
										<div class="bar-gray-black">Action Item Assignment</div>
										<br />

										<div id="action_item_assignment_div" class="lr-mar-10px">

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;"><span class="ibm-required">*</span>Action
														Description:</th>
													<th scope="col"><form:textarea
															path="rcaAction.actionDesc" cols="137" rows="5" /></th>
												</tr>

												</thead>
											</table>

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;"><span class="ibm-required">*</span>Assigned
														To:</th>
													<th scope="col" width="25%">
														<form:input id="assignedTo" path="rcaAction.assignedTo" size="40" class="typeahead" maxlength="250"/></th>
													<th scope="col" width="15%"></th>
													<th scope="col" width="15%" style="font-weight: normal;"></th>
													<th scope="col" width="30%"></th>

												</tr>

												</thead>
											</table>

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;">Assigned
														on:</th>
													<th scope="col" width="25%"><form:input
															path="actionHelper.assignedDate" readonly="true"
															style="background: #fafafa" size="40" maxlength="250"/></th>
													<th scope="col" width="15%"></th>
													<th scope="col" width="15%" style="font-weight: normal;"></th>
													<th scope="col" width="30%"></th>

												</tr>

												</thead>
											</table>

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;"><span class="ibm-required">*</span>Target
														Date:</th>
													<th scope="col" width="25%"><form:input
															id="targetDate" path="actionHelper.targetDate"
															onchange="enableJustification();" size="40" /></th>
													<th scope="col" width="15%"></th>
													<th scope="col" width="15%" style="font-weight: normal;"></th>
													<th scope="col" width="30%"></th>
												</tr>

												</thead>
											</table>

											<table id="target_date_reason" summary="Contacts"
												   class="ibm-results-table" border="0" style="display: none;">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;"><span class="ibm-required">*</span>Justification/Reason
														for Changing Target Date:</th>
													<th scope="col"><form:textarea
															path="rcaAction.targetDateModificationReason" cols="137"
															rows="5" /></th>
												</tr>

												</thead>
											</table>

										</div>
										<!-- End Item Description -->





										<!-- Start supporting information -->
										<div id="add_sp_info" class="bar-gray-black">Additional
											Supporting Information</div>
										<div id ="add_info_div" class="lr-mar-10px">

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;"><span class="ibm-required">*</span>Additional
														Information:</th>
													<th scope="col"><form:textarea
															path="rcaAction.additionalInfo" cols="137" rows="5" /></th>
												</tr>

												</thead>
											</table>


											<table class="ibm-results-table"
												   summary="additional support info">
												<thead>
												<tr>
													<td width="100%"><label id="fileLabel" for="file"
																			style="font-weight: normal; white-space: nowrap;">Click
														browse to upload files. Maximum size allowed is 10MB.</label></td>
												</tr>
												</thead>
											</table>
											<table class="ibm-results-table" summary="file">
												<thead>
												<tr>
													<th scope="col" style="font-weight: normal;" width="5%"><input
															id="file" type="file" name="file"
															onchange="enableDescritpion();" /></th>
													<th scope="col" width="10%"></th>
													<td width="10%" id="descTd" style="display: none;">Description:
													</td>
													<td width="75%"><input type="text" id="description"
																		   style="display: none;" size="30" /></td>
												</tr>
												</thead>
											</table>
										</div>

										<c:if test="${not empty actionForm.actionHelper.supportingFiles}">

											<div class="lr-mar-10px">

												<table id="additionalCommentIbm" summary="Contacts"
													   class="ibm-results-table" border="0">
													<thead>
													<tr>
														<th>
															<div class="bar-blue-med-light"
																 style="font-weight: normal; height: 23px;">
																<table id="rca_closed_date" summary="Contacts"
																	   border="0">
																	<thead>
																	<tr>
																		<th width="20%">Uploaded Files</th>
																		<th width="40%"></th>
																		<th width="20%"></th>
																		<th width="10%"></th>
																		<th width="10%"></th>
																	</tr>
																	</thead>
																</table>
															</div>
														</th>
													</tr>

													</thead>
												</table>

												<table class="ibm-data-table" summary="file name">
													<thead>

													<tr></tr>

													<tr style="">
														<th class="bar-gray-light-test" scope="col"
															align="center" width="15%">File Name</th>

														<th class="bar-gray-light-test" scope="col"
															align="center" width="23%">Date</th>

														<th class="bar-gray-light-test" scope="col"
															align="center" width="24%">Description</th>

														<th class="bar-gray-light-test" scope="col"
															align="center" width="20%">Uploaded By</th>

														<th class="bar-gray-light-test" scope="col"
															align="center" width="21%"></th>

													</tr>

													</thead>
													<c:forEach
															items="${actionForm.actionHelper.supportingFiles}"
															var="file" varStatus="loop">
														<c:if
																test="${file.name != null || fn:trim(file.name) != ''}">
															<form:hidden
																	path="actionHelper.supportingFiles[${loop.index}].fileId" />
															<form:hidden
																	path="actionHelper.supportingFiles[${loop.index}].name" />
															<form:hidden
																	path="actionHelper.supportingFiles[${loop.index}].type" />
															<form:hidden
																	path="actionHelper.supportingFiles[${loop.index}].uploadedOn" />
															<form:hidden
																	path="actionHelper.supportingFiles[${loop.index}].isDeleted"
																	id="isDeleted_${loop.index}" />
															<tr id="rcaActionFile_${loop.index}">
																<td width="15%"><a class="ibm-download-link"
																				   href="getRcaActionFile.htm?rcaActionFileId=${file.fileId}"
																				   target="_blank">${file.name}</a></td>

																<td width="20%">${file.uploadedTime}</td>

																<td width="23%">${file.description}</td>

																<td width="23%">${file.uploadedBy.notesId}</td>

																<td width="21%"><a class="ibm-delete-link"
																				   href="javascript:void(0)"
																				   onclick="hide('rcaActionFile_${loop.index}'); setIsFileDelete('${loop.index}');"
																				   alt="Delete">Delete</a></td>
															</tr>
														</c:if>
													</c:forEach>

												</table>
											</div>

										</c:if>



										<!-- RCA Manager Notations -->
										<div class="bar-gray-black">RCA Manager Notations</div>
										<br />

										<div id="rca_manager_notations_div" class="lr-mar-10px">
											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;"><span class="ibm-required">*</span>Action
														Item Notes:</th>
													<th scope="col"><form:textarea
															path="rcaAction.actionItemNote" cols="137" rows="5" /></th>
												</tr>

												</thead>
											</table>

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;"><span class="ibm-required">*</span>Follow-up
														Activities:</th>
													<th scope="col"><form:textarea
															path="rcaAction.followupActivity" cols="137" rows="5" /></th>
												</tr>

												</thead>
											</table>

											<table id="rca_number" summary="Contacts"
												   class="ibm-results-table" border="0">
												<thead>
												<tr>
													<th scope="col" width="15%" style="font-weight: normal;"><span class="ibm-required">*</span>RCA
														Notes:</th>
													<th scope="col"><form:textarea
															path="rcaAction.rcaNote" cols="137" rows="5" /></th>
												</tr>

												</thead>
											</table>



										</div>


										<!-- Start Document Event History Log -->
										<div class="bar-gray-black">
											<input type="checkbox" id="actionHistoryLogCheckBox"
												   name="actionHistoryLogCheckBox"
												   onclick="showOrHideActionHistoryLog();">&nbsp;Document
											Event History Log
										</div>

										<div id="actionHistoryLogDiv" style="display: none">
											<c:if
													test="${not empty actionForm.actionHelper.rcaActionHistoryLogs}">
												<table id="incident_details" summary="Contacts"
													   class="ibm-results-table" border="0">
													<thead>
													<tr>
														<td scope="col" align="center"
															class="bar-gray-black-small" width="21%">Submit
															Date</td>
														<th width="0.5%"></th>
														<td scope="col" align="center"
															class="bar-gray-black-small" width="13%">Form
															Action</td>
														<th width="0.5%"></th>
														<td scope="col" align="center"
															class="bar-gray-black-small" width="35%">Submit
															Comments</td>
														<th width="0.5%"></th>
														<td scope="col" align="center"
															class="bar-gray-black-small" width="20%">Submitted
															By</td>
														<th width="0.5%"></th>
														<td scope="col" align="center"
															class="bar-gray-black-small" width="14%">Role</td>
													</tr>
													</thead>
												</table>
											</c:if>
											<c:forEach
													items="${actionForm.actionHelper.rcaActionHistoryLogs}"
													var="item" varStatus="itemRow">
												<table id="incident_details" summary="Contacts"
													   class="ibm-results-table" border="0">
													<thead>
													<tr>
														<td scope="col" align="center" width="21%">${item.submittedOn}</td>
														<th width="0.5%"></th>
														<td scope="col" width="13%" align="center">${item.formAction}</td>
														<th width="0.5%"></th>
														<td scope="col" align="center" width="35%">${item.comments}</td>
														<th width="0.5%"></th>
														<td scope="col" align="center" width="20%">${item.submittedBy.email}</td>
														<th width="0.5%"></th>
														<td scope="col" align="center" width="14%">${item.role}</td>
													</tr>
													</thead>
												</table>

											</c:forEach>

										</div>

										<br /> <br />


										<!-- Start Form Action -->
										<div class="bar-gray-black">Form Actions</div>
										<br />
										<div class="lr-mar-10px">
											<input type="submit" value="Back" style="height: 30px;"
												   name="ibm-back" class="ibm-btn-cart-sec"
												   onclick="setFormAction('back');" /> <span class="ibm-sep">&nbsp;</span>
											<input type="submit" value="Save" style="height: 30px;"
												   id="ibm-save" name="ibm-save"
												   onclick="setFormAction('save'); setFileDescription();"
												   class="ibm-btn-cart-sec"> <span id="submit-span"
																				   class="ibm-sep">&nbsp;</span> <span id="cancel-span"
																													   class="ibm-sep">&nbsp;</span>
											<input type="button"
												   value="Close Action" style="height: 30px;" id="ibm-close"
												   name="ibm-close"
												   onclick="setFormAction('close'); showCloseActionBox();"
												   class="ibm-btn-cart-sec">

										</div>


									</div>
									<!-- End Form Action -->




								</div>

							</form:form>


						</div>


					</div>
				</div>
			</div>
		</div>
	</div>

</div>

<!-- Close Action -->
<div class="ibm-common-overlay ibm-overlay-alt" id="close-action-box">
	<div class="ibm-head">
		<p>
			<a class="ibm-common-overlay-close" href="#close">Close [x]</a>
		</p>
	</div>
	<div class="ibm-body">
		<div class="ibm-main">
			<div class="ibm-title">
				<h2>Resolution Description</h2>
			</div>
			<div class="ibm-overlay-rule">
				<hr />
			</div>
			<div class="ibm-container ibm-alternate">
				<div class="ibm-container-body">
					<p>
						<tr>
							<td>
								<table class="ibm-results-table" width="100%" cellspacing="1"
									   cellpadding="1" style="margin-bottom: 10px;"
									   summary="The following table shows the labels of profile listing">
									<tbody>
									<thead>
									<form:textarea id="closeActionComments"
												   path="actionForm.actionHelper.resolutionDescription"
												   cols="70" rows="3" style="font-size: 12px;" />
									</thead>
									</tbody>
								</table>
							</td>
						</tr>
						<tr>
							<div class="ibm-overlay-rule">
								<hr />
							</div>
							<div class="ibm-buttons-row">
								<p>
									<input value="Submit" id="ibm-submit-btn" type="submit"
										   name="ibm-submit" class="ibm-btn-arrow-pri"
										   onclick="closeRcaAction();" /> <span class="ibm-sep">&nbsp;</span>
									<input value="Cancel" id="ibm-cancel-btn" type="button"
										   name="ibm-cancel" class="ibm-btn-cancel-sec"
										   onclick="ibmweb.overlay.hide('close-action-box');" />
								</p>
							</div>
						</tr>
					</p>
				</div>
			</div>
		</div>
	</div>
	<div class="ibm-footer"></div>
</div>
</body>
</html>

<script type="text/javascript" src="js/rca-1.0.js">
	//
</script>


<script>
	function enforceMaxLength(obj, length) {
		var maxLength = length;
		if (obj.value.length > maxLength) {
			obj.value = obj.value.substring(0, maxLength);
		}
	}
	function setFormAction(_formAction) {
		console.log('calling setFormAction...');
		dojo.byId("formAction").value = _formAction;
		return true;
	}

	function hide(id) {
		try {
			dojo.byId(id).style.display = 'none';
		} catch (err) {
			console.error("Error hide(" + id + ") => " + err.message);
		}
	}

	function setIsFileDelete(isFileDelete) {
		dojo.byId("isDeleted_" + isFileDelete).value = 'Y';
	}

	function enableDescritpion() {
		console.log('file is selected');
		$("#descTd").show();
		$("#description").show();
	}

	function setFileDescription() {
		var description = dojo.byId('description').value;
		dojo.query('#fileDescription').val(description);
	}

	function showOrHideActionHistoryLog() {
		if ($("#actionHistoryLogCheckBox").is(':checked')) {
			$('#actionHistoryLogDiv').show();
		} else {
			$('#actionHistoryLogDiv').hide();
		}
	}

	function showCloseActionBox() {
		console.log("showCloseActionBox.....");
		ibmweb.overlay.show('close-action-box');
	}

	function enableJustification() {
		console.log("enableJustification.....");
		$('#target_date_reason').show();
		dojo.query('#targetDateUpdated').val(true);
	}

	function closeRcaAction() {
		console.log('calling closeRcaAction....');
		var comment = dojo.byId('closeActionComments').value;
		console.log('resolution description :' + comment);
		dojo.query('#resolutionDescription').val(comment);
		setFormAction('close');
		$("#actionForm").submit();
	}



</script>
<script type="text/javascript">
	$('#targetDate').datetimepicker({
		dayOfWeekStart : 1,
		lang : 'en',
		format : 'Y-m-d',
		formatDate : 'Y.m.d',
		timepicker : false
	});
    enableOrDisableActionFormButtons();
	disableFormForClosedAction();

</script>