<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<c:forEach items="${rcaForm.rcaContributingCauses}" var="item" varStatus="itemRow">

	<form:hidden id="rcaForm.rcaContributingCauses[${itemRow.index}].rcaCauseId" path="rcaForm.rcaContributingCauses[${itemRow.index}].rcaCauseId" />


	<!----------*** Begin Root Cause Description ***---------->
	<table id="service_description" summary="Contacts"
		   class="ibm-results-table" border="0">
		<thead>
		<tr>
			<th scope="col" align width="15%"
				style="font-weight: normal;">*Outage Category:
			</th>
			<th scope="col" width="15%"><form:select
					path="rcaForm.rcaContributingCauses[${itemRow.index}].outageCategory" id="rcaForm.rcaContributingCauses[${itemRow.index}].outageCategory"
					onchange="getOutageSubCategories('${basePath}','rcaForm.rcaContributingCauses[${itemRow.index}].outageCategory','rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory');"
					onselect="getOutageSubCategories('${basePath}','rcaForm.rcaContributingCauses[${itemRow.index}].outageCategory','rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory');">
				<form:option value="" label="Please select the option"/>
				<form:options items="${outageCategories}"
							  itemValue="serviceDescriptionId"
							  itemLabel="serviceDescriptionName"/>
			</form:select></th>
			<th scope="col" width="5%"></th>
			<th scope="col" width="15%" style="color: #ff0000;">*Location of
				Business Impact:
			</th>
			<th scope="col" width="15%">
				<form:select path="rcaForm.rcaContributingCauses[${itemRow.index}].locationOfBusinessImpact"
							 id="rcaForm.rcaContributingCauses[${itemRow.index}].locationOfBusinessImpact">
					<form:option value="" label="Please select the option"/>
					<form:options items="${locationsOfBusiness}"/>
				</form:select></th>
			<th scope="col" width="5%"></th>
			<th scope="col" align="centre" width="15%"
				style="font-weight: normal;">Other Location of Business Impact:
			</th>
			<th scope="col" width="15%"><form:input
					path="rcaForm.rcaContributingCauses[${itemRow.index}].otherLocOfBusinessImpact"
					id="rcaForm.rcaContributingCauses[${itemRow.index}].otherLocOfBusinessImpact" size="25" maxlength="250"/></th>
		</tr>
		</thead>
	</table>

	<table id="cause_description" summary="Contacts"
		   class="ibm-results-table" border="0">
		<thead>
		<tr>
			<th scope="col" align width="14.5%"
				style="color: #ff0000;"><span class="ibm-required">*</span>Sub Category:
			</th>
			<th scope="col" width="15%"><form:select
					path="rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory"
					id="rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory"
					onchange="getOutageSubCategories2('${basePath}','rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory','rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory2');"
					onselect="getOutageSubCategories2('${basePath}','rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory','rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory2');">
				<form:option value="" label="Please select the option"/>
				<form:options items="${rcaForm.rcaContributingCauses[itemRow.index].outageSubCategories}"
							  itemValue="serviceDescriptionId"
							  itemLabel="serviceDescriptionName"/>
			</form:select></th>
			<th scope="col" width="5%"></th>
			<th scope="col" width="14.5%" style="color: #ff0000;"><span class="ibm-required">*</span>Sub Category 2:</th>
			<th scope="col" width="15.5%"><form:select
					path="rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory2"
					id="rcaForm.rcaContributingCauses[${itemRow.index}].outageSubCategory2">
				<form:option value="" label="Please select the option"/>
				<form:options items="${rcaForm.rcaContributingCauses[itemRow.index].outageSubCategories2}"
							  itemValue="serviceDescriptionName"
							  itemLabel="serviceDescriptionName"/>
			</form:select></th>
			<th scope="col" width="5%"></th>
			<th scope="col" align="centre" width="15.5%"
				style="font-weight: normal;"> Location of System:
			</th>
			<th scope="col" width="15%"><form:input
					path="rcaForm.rcaContributingCauses[${itemRow.index}].locOfSystem"
					id="rcaForm.rcaContributingCauses[${itemRow.index}].locOfSystem" size="25" maxlength="250"/></th>
		</tr>
		</thead>
	</table>

	<table id="cause_description" summary="Contacts"
		   class="ibm-results-table" border="0">
		<thead>
		<tr>
			<th scope="col" align width="15%"
				style="font-weight:normal;">**Contributing Cause:</th>
			<th scope="col" width="15%"><form:select
					path="rcaForm.rcaContributingCauses[${itemRow.index}].rootOrContributingCause"
					id="rcaForm.rcaContributingCauses[${itemRow.index}].rootOrContributingCause"
					onchange="getCauses('${basePath}','rcaForm.rcaContributingCauses[${itemRow.index}].rootOrContributingCause','rcaForm.rcaContributingCauses[${itemRow.index}].causeCategory'); resetCauseGuidence('#causeSelectionGuidance${itemRow.index}');"
					onselect="getCauses('${basePath}','rcaForm.rcaContributingCauses[${itemRow.index}].rootOrContributingCause','rcaForm.rcaContributingCauses[${itemRow.index}].causeCategory');">
				<form:option value="" label="Please select the option"/>
				<form:options items="${rootCauses}"
							  itemValue="causeDescriptionName"
							  itemLabel="causeDescriptionName"/>
			</form:select></th>
			<th scope="col" width="5%"></th>
			<th scope="col" width="14%" style="font-weight:normal;">*Cause
				Category:
			</th>
			<th scope="col" width="18%"><form:select
					path="rcaForm.rcaContributingCauses[${itemRow.index}].causeCategory"
					id="rcaForm.rcaContributingCauses[${itemRow.index}].causeCategory"
					onchange="getCauses('${basePath}','rcaForm.rcaContributingCauses[${itemRow.index}].causeCategory','rcaForm.rcaContributingCauses[${itemRow.index}].causeSubCategory'); resetCauseGuidence('#causeSelectionGuidance${itemRow.index}');"
					onselect="getCauses('${basePath}','rcaForm.rcaContributingCauses[${itemRow.index}].causeCategory','rcaForm.rcaContributingCauses[${itemRow.index}].causeSubCategory'); resetCauseGuidence('#causeSelectionGuidance${itemRow.index}');">
				<form:option value="" label="Please select the option"/>
				<form:options items="${rcaForm.rcaContributingCauses[itemRow.index].causeCategories}"
							  itemValue="causeDescriptionName"
							  itemLabel="causeDescriptionName"/>
			</form:select></th>
			<th scope="col" width="3%"></th>
			<th scope="col" align="centre" width="15%"
				style="font-weight: normal;">Cause Subcategory:
			</th>
			<th scope="col" width="15%"><form:select
					path="rcaForm.rcaContributingCauses[${itemRow.index}].causeSubCategory"
					id="rcaForm.rcaContributingCauses[${itemRow.index}].causeSubCategory"
					onchange = "setCauseGuidence('rcaForm.rcaContributingCauses[${itemRow.index}].causeCategory','rcaForm.rcaContributingCauses[${itemRow.index}].causeSubCategory', '#causeSelectionGuidance${itemRow.index}'); "
					onselect = "setCauseGuidence('rcaForm.rcaContributingCauses[${itemRow.index}].causeCategory','rcaForm.rcaContributingCauses[${itemRow.index}].causeSubCategory','#causeSelectionGuidance${itemRow.index}'); ">
				<form:option value="" label="Please select the option"/>
				<form:options items="${rcaForm.rcaContributingCauses[itemRow.index].causeSubCategories}"
							  itemValue="causeDescriptionId"
							  itemLabel="causeDescriptionName"/>
			</form:select></th>
		</tr>
		</thead>
	</table>

	<table id="service_description" summary="Contacts"
		   class="ibm-results-table" border="0">
		<thead>
		<tr>

			<th scope="col" align="centre" width="13%"
				style="font-weight: normal;">Guidance for Cause selection:
			</th>
			<th scope="col" width="87%"><form:textarea
					id="causeSelectionGuidance${itemRow.index}"
					path="rcaForm.rcaContributingCauses[${itemRow.index}].causeSelectionGuidance" cols="143"
					rows="3" readonly="true" style="background: #fafafa"/></th>
		</tr>
		</thead>
	</table>



	<table id="cause_summary" summary="Contacts" class="ibm-results-table"
		   border="0">
		<thead>
		<tr>

			<th scope="col" align="centre" width="15%"
				style="font-weight:normal;">**Contributing Cause Summary:
			</th>
			<th scope="col" width="70%"><form:textarea path="rcaForm.rcaContributingCauses[${itemRow.index}].causeSummary"
													   id="rcaForm.rcaContributingCauses[${itemRow.index}].causeSummary"
													   cols="110"
													   rows="2" /></th>
			<th scope="col" align="centre" width="5%"></th>
			<th scope="col" align="centre" width="10%"
				style="font-weight:normal;"> <input  align="right" type="button" value="Delete" style="height:28px; width:1px;" name="ibm-submit"
													 class="ibm-btn-cart-sec" onclick="deleteRCAContributingCause('${basePath}','${rcaForm.rcaContributingCauses[itemRow.index].rcaCauseId}')">
			</th>
		</thead>
	</table>

	<!----------*** Begin Root Cause Description ***---------->
	<hr/>

</c:forEach>