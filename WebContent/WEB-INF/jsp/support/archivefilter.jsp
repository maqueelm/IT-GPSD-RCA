<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<!--
<style type="text/css" media="all">
.lr-mar-10px {margin-left: 10px; margin-right:10px; margin-bottom: 2px; margin-top: 2px }
</style>

-->
<div  id="pii" style="display:none">
<p style="display:none">The fields indicated with an asterisk (*) are required to complete this transaction; other fields are optional. If you do not want to provide us with the required information, please use the "Back" button on your browser to return to the previous page, or close the window or browser session that is displaying this page.</p>
</div>

<form:form commandName="searchFilter" method="POST" id="archiveFilterForm">
<fieldset style="border: 1px solid #ccc;" class="ibm-container-body">
<legend id="fieldset-legend"><span><h3>Filter Criteria</h3></span></legend>											
<div id="searchFilters-main">
<div style="padding: 0 5px 0 5px;">
<table width="100%" cellspacing="4" cellpadding="4" id="filterTab" class="ibm-results-table"  summary="support form">
	<tbody>
	 <thead>
		<tr>
			<th scope="row">
			<table width="100%" cellspacing="1" cellpadding="1" class="ibm-results-table" summary="support form fields">
				<tbody>
				<thead>
				<!--	<tr>
						<td width="2%"></td>
						<td width="25%"></td>
						<th width="15%"><h5>Month</h5> </th>
						<th width="31%" align="center"><h5>Year</h5></th>
					</tr> -->
					<tr>
						<th scope="col" width="2%"></th>
						<th scope="col" width="25%"></th>
						<th scope="col" width="15%" align="left"><span>
						<label for="month" ><h5>Month</h5></label>
						<form:select path="month"  cssStyle="background: white;">
									<form:option value="" label="All Months" />
									<form:option value="1" label="January" />
									<form:option value="2" label="February" />
									<form:option value="3" label="March" />
									<form:option value="4" label="April" />
									<form:option value="5" label="May" />
									<form:option value="6" label="June" />
									<form:option value="7" label="July" />
									<form:option value="8" label="August" />
									<form:option value="9" label="September" />
									<form:option value="10" label="October" />
									<form:option value="11" label="November" />
									<form:option value="12" label="December" /> 
						</form:select>
						</span></th>
						<th scope="col" width="31%" align="left">
							<span>
							<label for="year" ><h5>Year</h5></label>
								<form:select path="year"  cssStyle="background: white;">
									<form:option value="" label="All Years" />
									<form:option value="2010" label="2010" />
									<form:option value="2011" label="2011" />
									<form:option value="2012" label="2012" />
									<form:option value="2013" label="2013" />
									<form:option value="2014" label="2014" />
									<form:option value="2015" label="2015" />
									<form:option value="2016" label="2016" />
									<form:option value="2017" label="2017" />
									<form:option value="2018" label="2018" />
									<form:option value="2019" label="2019" />
									<form:option value="2020" label="2020" />									 
						</form:select>								
							</span>
						</th>
					</tr>
				  </thead>
				</tbody>
			</table>
			</th>
		</tr>

		<tr>
			<th scope="row">
			<table width="100%" cellspacing="1" cellpadding="1" class="ibm-results-table" summary="support form fields">
				<tbody>
				<thead>
				<!--	<tr>
						<th width="25%"><h5>Form</h5></th>
						
						<th width="25%" align="center"><h5>State</h5></th>
						<th width="25%" align="center"><h5>Submitter</h5></th>
						<th width="25%" align="center"><h5>Approver</h5></th>
						
					</tr>-->
					<tr>
						<th width="25%" scope="col">
							<span> 
							<label for="form" ><h5>Form</h5></label>
								<form:select path="form"  cssStyle="background: white;">
									<form:option value="" label="All Forms" />
									<form:option value="Access Request" label="GPSRCA Access Request" />
								</form:select>
							</span>
						</th>
						
						
						<th width="25%" scope="col" align="center"><span>
						<label for="state" ><h5>State</h5></label>
						<form:select path="state" cssStyle="background: white;">
								<form:option value="" label="Any State" />
								<form:option value="1" label="Accepted" />
								<form:option value="8" label="Rejected" />
							</form:select>
						</span></th>
						<th width="25%" scope="col" align="center"><span>
						<label for="submitter" ><h5>Submitter</h5></label>
							<form:select path="submitter" cssStyle="background: white;">
								<form:option value="" label="All Submitters" />
								<form:options items="${submitters}"/> 
							</form:select>
						</span></th>
						<th width="25%" scope="col" align="center"><span>
						<label for="approver" ><h5>Approver</h5></label>
							<form:select path="approver" cssStyle="background: white;">
								<form:option value="" label="All Approvers" />
								<form:options items="${approvers}"/> 
							</form:select>
						</span></th>
					</tr>
					</thead>
				</tbody>
			</table>
			</th>
		</tr>
<!--
		<tr>
			<th scope="row">
			<table width="100%" cellspacing="1" cellpadding="1" class="ibm-results-table" summary="support form fields">
				<tbody>
					<tr>
						
						
						<th width="40%" align="center"><h5></h5></th>
					</tr>
					<tr>
						
						
						<th ><span>
							
						</span></th>
					</tr>
				</tbody>
			</table>
			<table width="100%" cellspacing="1" cellpadding="1" class="ibm-results-table" summary="support form fields">
				<tbody>
					<tr>
						
						
						<th width="40%" align="center"><h5></h5></th>
					</tr>
					<tr>
						
						
						<th ><span>
							
						</span></th>
					</tr>
				</tbody>
			</table>
			</th>
		</tr>
	-->	
		
		
		<tr>
			<th scope="row">
			<table width="100%" cellspacing="2" cellpadding="2" class="ibm-results-table" summary="support form fields">
				<tbody>
				 <thead>
					<tr height="5">
					</tr>
					<tr>
						<th scope="col" width="37%" align="left">
					
						</th>
						<th scope="col" width="20%" align="left"> <!--<input type="hidden" id="" value="Y" name="">--> 
							<input type="button" value="Apply" tabindex="5" name="ibm-submit" class="ibm-btn-cart-sec" onclick="getArchiveList();" />&nbsp; 
						</th>
						<th scope="col" width="20%" align="left">
							<input type="submit" value="Clear" name="ibm-cancel" tabindex="6" id="ibm-cancel" class="ibm-btn-cart-sec" />
						</th>
						<th scope="col" width="30%" align="left">
					
						</th>
					</tr>
					</thead>
				</tbody>
			</table>
			</th>
		</tr>
		</thead>
	</tbody>
</table>
</div>
</div>
</fieldset>		
</form:form>