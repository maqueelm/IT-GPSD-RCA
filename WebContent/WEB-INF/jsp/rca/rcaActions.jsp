<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<div id="actionItemsDiv">
    <input type="hidden" id="hasActions" value="${hasActions}" />
    <c:if test="${not empty rcaForm.rcaActions}">
        <table id="incident_details" summary="Contacts" class="ibm-results-table" border="0">
            <thead>
            <tr>

                <td scope="col" align="center" width="4%" class="bar-green-black-small">ID</td>
                <th width="0.5%"></th>
                <td scope="col" align="center" width="40%" class="bar-green-black-small">**Action Description</td>
                <th width="0.5%"></th>
                <td scope="col" align="center" width="20%" class="bar-green-black-small">**Owner</td>
                <th width="0.5%"></th>
                <td scope="col" align="center" width="15%" class="bar-green-black-small">Action Dates</td>
                <th width="0.5%"></th>
                <td scope="col" align="center" width="10%" class="bar-green-black-small">Status</td>
                <th width="0.5%"></th>
                <td width="7.5%" class="bar-green-black-small"></td>
            </tr>
            </thead>
        </table>
    </c:if>
    <c:forEach items="${rcaForm.rcaActions}" var="item" varStatus="itemRow">

        <form:hidden id="rcaForm.rcaActions[${itemRow.index}].rcaActionId" path="rcaForm.rcaActions[${itemRow.index}].rcaActionId" />
        <form:hidden id="rcaForm.rcaActions[${itemRow.index}].actionStatus" path="rcaForm.rcaActions[${itemRow.index}].actionStatus" />
        <form:hidden id="rcaForm.rcaActions[${itemRow.index}].actionNumber" path="rcaForm.rcaActions[${itemRow.index}].actionNumber" />


        <c:set var="count" value="${itemRow.index + 1}" scope="page"/>
        <table id="incident_details" summary="Contacts" class="ibm-results-table" border="0">
            <thead>
            <tr>

                <td scope="col" align="center" width="4%"><input type="text" value="${count}" size="1"
                                                                 disabled="disabled"></td>
                <th width="0.5%"></th>

                <c:choose>
                    <c:when test="${rcaStage != null && rcaStage == 'ClosedWithOpenActions'}">
                        <td scope="col" width="40%" align="center">
                            <form:textarea
                                    id="rcaForm.rcaActions[${itemRow.index}].actionDesc"
                                    path="rcaForm.rcaActions[${itemRow.index}].actionDesc" cols="61"
                                    rows="3" disabled="true" />
                        </td>
                    </c:when>
                    <c:otherwise>
                        <td scope="col" width="40%" align="center">
                            <form:textarea
                                    id="rcaForm.rcaActions[${itemRow.index}].actionDesc"
                                    path="rcaForm.rcaActions[${itemRow.index}].actionDesc" cols="61"
                                    rows="3" />
                        </td>

                    </c:otherwise>
                </c:choose>

                <th width="0.5%"></th>
                <c:choose>
                    <c:when test="${rcaStage != null && rcaStage == 'ClosedWithOpenActions'}">
                        <td scope="col" align="center" width="20%"><form:input
                                path="rcaForm.rcaActions[${itemRow.index}].assignedTo"
                                id="rcaForm.rcaActions[${itemRow.index}].assignedTo"
                                size="29" disabled="true" maxlength="250"/></td>
                    </c:when>
                    <c:otherwise>
                        <td scope="col" align="center" width="20%"><form:input
                                path="rcaForm.rcaActions[${itemRow.index}].assignedTo"
                                id="rcaForm.rcaActions[${itemRow.index}].assignedTo"
                                size="29" onclick="ibmFaces();" class="typeahead" maxlength="250" /></td>
                    </c:otherwise>
                </c:choose>


                <th width="0.5%"></th>

                <c:choose>
                    <c:when test="${rcaStage != null && rcaStage == 'Closed'}">
                        <td scope="col" align="left" width="15%">Target: <form:input
                                path="rcaForm.rcaActions[${itemRow.index}].actionTargetDate"
                                id="action${itemRow.index}.actionTargetDate"
                                size="10" readonly="true" maxlength="250"/>
                            <br/><br/>

                            <c:if test="${rcaForm.rcaActions[itemRow.index].actionClosedDate != null}">
                                Closed: <form:input path="rcaForm.rcaActions[${itemRow.index}].actionClosedDate"
                                                    id="rcaForm.rcaActions[${itemRow.index}].actionClosedDate"
                                                    size="10" readonly="true" maxlength="250"/>

                            </c:if>

                        </td>
                    </c:when>
                    <c:otherwise>
                        <td scope="col" align="left" width="15%">Target: <form:input
                                path="rcaForm.rcaActions[${itemRow.index}].actionTargetDate"
                                id="actionTargetDate${itemRow.index}"
                                size="10"
                                onclick="$('#actionTargetDate${itemRow.index}').datetimepicker({dayOfWeekStart: 1,lang: 'en',format: 'Y-m-d',formatDate: 'Y.m.d',timepicker:false})"
                                onmouseover="$('#actionTargetDate${itemRow.index}').datetimepicker({dayOfWeekStart: 1,lang: 'en',format: 'Y-m-d',formatDate: 'Y.m.d',timepicker:false})" />

                        </td>
                    </c:otherwise>
                </c:choose>



                <th width="0.5%"></th>
                <td scope="col" align="center" width="10%"><form:input
                        path="rcaForm.rcaActions[${itemRow.index}].actionStatus"
                        id="rcaForm.rcaActions[${itemRow.index}].actionStatus"
                        size="10" disabled="true"/></td>
                <th width="0.5%"></th>
                <td width="7.5%">
                    <c:if test="${rcaForm.rcaActions[itemRow.index].targetDate != null}">
                        <a target="_blank" class="ibm-sso-signin" href="showRcaAction.htm?actionNumber=${rcaForm.rcaActions[itemRow.index].actionNumber}" onclick="" tabindex="0" dojoattrid="2" role="button"
                           aria-label="View Action">View</a>
                    </c:if>
                    <c:if test="${rcaForm.rcaActions[itemRow.index].targetDate == null}">
                        <input  align="right" type="button" value="Delete" style="width:60px; height:25px;" name="ibm-submit"
                                onclick="deleteRcaAction('${basePath}','${rcaForm.rcaActions[itemRow.index].rcaActionId}')">
                    </c:if>
                </td>

            </tr>
            </thead>
        </table>

    </c:forEach>
</div>
