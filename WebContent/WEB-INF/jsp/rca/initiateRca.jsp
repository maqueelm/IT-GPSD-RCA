<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + path + "/";
    String imagesBasePath = "//" + request.getServerName() + ":" + request.getServerPort() + path + "/";
%>


<style type="text/css">
    .ibm-common-overlay input[type="password"] {
        border: 1px solid #eee;
        color: #666;
        padding: 3px;
    }

    .ibm-common-overlay input[type="password"]:hover {
        border: 1px solid #ccc;
        color: #666;
    }

    .ibm-common-overlay input[type="password"]:focus {
        border: 1px solid #ccc;
        color: #666;
        background: #666;
        filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#666', endColorstr='#eee');
        background: -webkit-gradient(linear, left top, left bottom, from(#666),
        to(#eee));
        background: -moz-linear-gradient(top, #666, #eee);
        background: -o-linear-gradient(top, #666, #eee);
    }
</style>
<form:form id="initiateRcaForm" action="initiateRCA.do" commandName="initiateRcaForm" class="ibm-column-form" method="POST"
           style="margin-top: 5px;">
    <div class="ibm-container ibm-alternate ibm-buttons-last">
        <div class="ibm-container-body">
            <tr>


                <td>
                    <table class="ibm-results-table" width="100%" cellspacing="1" cellpadding="1"
                           style="margin-bottom: 10px;" border="0"
                           summary="The following table shows the labels of profile listing">
                        <tbody>
                        <thead>


                        <th align="left" scope="col" width="28%">RCA Month-Year:</th>
                        <th align="left" scope="col" width="72%" id="rcac">
                            <label id="monthValue" style="font-weight: normal">January</label>
                        </th>


                        </thead>
                        </tbody>
                    </table>
                </td>
            </tr>


            <tr>
                <td>
                    <table class="ibm-results-table" width="100%" cellspacing="1" cellpadding="1"
                           style="margin-bottom: 10px;" border="0"
                           summary="The following table shows the labels of profile listing">
                        <tbody>
                        <thead>


                        <th align="left" scope="col" width="25%"><label for="rcaContract">*Select Application:</label></th>

                        <th align="left" scope="col" width="45%">
						<span>						
						<form:select path="rcaContract" cssStyle="background: white;" id="rcaContract">
                            <form:options items="${referenceData.initiateRcaContracts}" itemValue="contractId"
                                          itemLabel="title" style="width:150px;"/>
                        </form:select>
                        <form:hidden path="rcaMonth" id="rcaMonth" value=""/>
                        <form:hidden path="rcaYear"  id="rcaYear" value=""/>
                        </span>

                        </th>
                        <th width="3%"></th>
                        <th align="left" scope="col" width="30%">
                            <input value="Load RCA Coordinator" id="load-rca-cdr-btn" type="button" name="ibm-submit"
                                   class="ibm-btn-arrow-pri" onClick="return loadRCACoordinators();"
                                   onSubmit="return loadRCACoordinators();"/>
                        </th>
                        </thead>
                        </tbody>
                    </table>
                </td>

            </tr>



            <div id="rcaCoordinatorDiv">

            </div>


            <tr>
                <div class="ibm-overlay-rule">
                    <hr/>
                </div>
                <div class="ibm-buttons-row">
                    <p>
                        <input value="Create RCA" id="create-rca-submit-btn" type="button" name="ibm-submit"
                               class="ibm-btn-arrow-pri" onClick="return createRCA();" onSubmit="return createRCA();"/>
                        <span class="ibm-sep">&nbsp;</span>
                        <input value="Cancel" id="create-rca-cancel-btn" type="button" name="ibm-cancel"
                               class="ibm-btn-cancel-sec" onclick="ibmweb.overlay.hide('initiate-rca-box');"/>
                    </p>
                </div>
            </tr>
        </div>
    </div>
</form:form>


<script type="text/javascript">


    function validateUser() {
        // Local var representing if the form has been sent at all
        //var hasBeenSent = false;
        // Local var representing node to be updated
        var contractListDiv = dojo.byId("validationResponse");
        // Using dojo.xhrPost, as the amount of data sent could be large
        dojo.xhrPost({
            // The URL of the request
            url: "validateUser.do",
            form: dojo.byId("loginForm"),
            // The success handler
            load: function (response) {

                if (response != null && response == "SUCCESS") {
                    dojo.byId("loginForm").submit();
                    return true;
                } else {
                    contractListDiv.innerHTML = response;
                    return false;
                }
            },
            // The error handler
            error: function () {
                contractListDiv.innerHTML = "An error occured, please try again.";
                return false;
            },
            // The complete handler
            handle: function () {
                // hasBeenSent = true;
            }
        });

    }

    function loadRCACoordinators() {
        console.log("loadRCACoordinators()........");
        var modifyAclDiv = dojo.byId("rcaCoordinatorDiv");
        modifyAclDiv.innerHTML = "";
        var URL = "<%=basePath%>loadRCACoordinators.htm";
        var contract = dojo.byId("rcaContract").value;
        // The parameters to pass to xhrGet, the url, how to handle it, and the callbacks.
        var xhrArgs = {
            url: URL,
            handleAs: "text",
            preventCache: true,
            content: {
                contract_id: contract
            },
            load: function (data) {
                // Replace tabs with spacess.
                // data = data.replace(/\t/g, "first/last/only");
                dojo.place(data, modifyAclDiv, "only");
                console.log("success");
            },
            error: function (error) {
                console.log("failure");
                modifyAclDiv.innerHTML = "An unexpected error occurred: " + error;
            }
        }

        // Call the asynchronous xhrGet
        var deferred = dojo.xhrGet(xhrArgs);

    }


    function createRCA() {
        // Local var representing if the form has been sent at all
        //var hasBeenSent = false;
        // Local var representing node to be updated
        var contractListDiv = dojo.byId("validationResponse");
        // Using dojo.xhrPost, as the amount of data sent could be large
        dojo.xhrPost({
            // The URL of the request
            url: "validateRca.do",
            form: dojo.byId("initiateRcaForm"),
            // The success handler
            load: function (response) {
                if (response != null && response == "SUCCESS") {
                    dojo.byId("initiateRcaForm").submit();
                    return true;
                } else {
                    contractListDiv.innerHTML = response;
                    return false;
                }
            },
            // The error handler
            error: function () {
                contractListDiv.innerHTML = "An error occured, please try again.";
                return false;
            },
            // The complete handler
            handle: function () {
                // hasBeenSent = true;
            }
        });

    }


</script>


