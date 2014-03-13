package org.opendaylight.controller.firewall.northbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.enunciate.jaxrs.ResponseCode;
import org.codehaus.enunciate.jaxrs.StatusCodes;
import org.codehaus.enunciate.jaxrs.TypeHint;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.firewall.FirewallRule;
import org.opendaylight.controller.firewall.IFirewall;
import org.opendaylight.controller.northbound.commons.RestMessages;
import org.opendaylight.controller.northbound.commons.exception.InternalServerErrorException;
import org.opendaylight.controller.northbound.commons.exception.MethodNotAllowedException;
import org.opendaylight.controller.northbound.commons.exception.NotAcceptableException;
import org.opendaylight.controller.northbound.commons.exception.ResourceNotFoundException;
import org.opendaylight.controller.northbound.commons.exception.ServiceUnavailableException;
import org.opendaylight.controller.northbound.commons.exception.UnauthorizedException;
import org.opendaylight.controller.northbound.commons.utils.NorthboundUtils;
import org.opendaylight.controller.sal.authorization.Privilege;
//import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
//import org.opendaylight.controller.switchmanager.ISwitchManager;

/**
 * Northbound APIs that provide capabilities to program firewall.
 *
 * <br>
 * <br>
 * Authentication scheme : <b>HTTP Basic</b><br>
 * Authentication realm : <b>opendaylight</b><br>
 * Transport : <b>HTTP and HTTPS</b><br>
 * <br>
 * HTTPS Authentication is disabled by default.
 *
 */

@Path("/")
public class FirewallNorthbound{
    private String username;
    @Context
    public void setSecurityContext(SecurityContext context) {
        if (context != null && context.getUserPrincipal() != null) {
            username = context.getUserPrincipal().getName();
        }
    }

    protected String getUserName(){
        return username;
    }

    private IFirewall getFirewallService(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper.getGlobalInstance(
                IContainerManager.class, this);
        if (containerManager == null) {
            throw new ServiceUnavailableException("Container " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

        boolean found = false;
        List<String> containerNames = containerManager.getContainerNames();
        for (String cName : containerNames) {
            if (cName.trim().equalsIgnoreCase(containerName.trim())) {
                found = true;
            }
        }

        if (found == false){
            throw new ResourceNotFoundException(containerName + " " + RestMessages.NOCONTAINER.toString());
        }

        IFirewall frw = (IFirewall) ServiceHelper.getInstance(
                IFirewall.class, containerName, this);

        if (frw == null){
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        return frw;
    }
    /**
     * get firewall status
     *
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return
     *         <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/firewall/default/status
     */
    @Path("/{containerName}/status")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public String isenabled(@PathParam("containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        IFirewall frw = getFirewallService(containerName);
        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (frw.isEnabled())
            return "{\"result\" : \"firewall enabled\"}\n";
        else
            return "{\"result\" : \"firewall disabled\"}\n";
    }
    /**
     * set firewall status
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return
     *         <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/firewall/default/status/enabled
     * http://localhost:8080/controller/nb/v2/firewall/default/status/disabled
     */
    @Path("/{containerName}/status/{status}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public String setStatus(@PathParam("containerName") String containerName,@PathParam("status") String status) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.READ, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        IFirewall frw = getFirewallService(containerName);
        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        if (status.equals("enabled")){
            Status statusLog=frw.setstatus(true);
            if (statusLog.isSuccess()){
                return "{\"status\" : \"success\", \"details\" : \"firewall running\"}\n";
            }else{
                return "{\"status\" : \"failed\", \"details\" : \"firewall running failed\"}\n";
            }
        }
        else if (status.equals("disabled")){
            Status statusLog=frw.setstatus(false);
            if (statusLog.isSuccess()){
                return "{\"status\" : \"success\", \"details\" : \"firewall stop\"}\n";
            }else{
                return "{\"status\" : \"failed\", \"details\" : \"firewall stop failed\"}\n";
            }
        }
        return status;
    }
    /**
     * Add a rule
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param name
     *            Name of a rule
     * @param rule
     *            Rule Configuration in JSON or XML format
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/firewall/default/add/rule1
     *
     * Request body in XML:
     * &lt;rule&gt;
     *         &#x20;&#x20;&#x20;&lt;name&gt;rule1&lt;/name&gt;
     *         &#x20;&#x20;&#x20;&lt;priority&gt;2&lt;/priority&gt;
     *         &#x20;&#x20;&#x20;&lt;etherType&gt;0x800&lt;/etherType&gt;
     *         &#x20;&#x20;&#x20;&lt;nwSrc&gt;10.0.0.1&lt;/nwSrc&gt;
     *         &#x20;&#x20;&#x20;&lt;nwDst&gt;10.0.0.6&lt;/nwDst&gt;
     *         &#x20;&#x20;&#x20;&lt;action&gt;ALLOW&lt;/action&gt;
     * &lt;/rule&gt;
     *
     * Request body in JSON:
      * {
     *    "name":"rule1",
     *    "priority":"2",
     *    "etherType":"0x800",
     *    "nwSrc":"10.0.0.1",
     *    "nwDst":"10.0.0.6",
     *    "action":"ALLOW"
     * }
     * </pre>
     */
    @Path("/{containerName}/add/{name}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response addRule(@PathParam(value="containerName") String containerName,@PathParam("name") String name, @TypeHint(FirewallRule.class) FirewallRule rule ) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        if (rule.getName() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Configuration. Name is null")
                    .build();
        }
        handleResourceCongruence(name, rule.getName());
        handleDefaultDisabled(containerName);

        IFirewall frw = getFirewallService(containerName);
        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        String installSoon=rule.getInstallSoon();
        Status status=new Status(StatusCode.BADREQUEST,"not define whether install flowentry soon based on this rule");
        if (installSoon==null){
            return NorthboundUtils.getResponse(status);
        }
        if (installSoon.equals("1")){
            status=frw.installRule(rule);
        }else{
            status=frw.addRule(rule);
        }
        if(status.isSuccess()){
              NorthboundUtils.auditlog("rule", username, "added", containerName);
              return Response.status(Response.Status.CREATED).entity("Success! FirewallRule created\n").build();
        }
        return NorthboundUtils.getResponse(status);
    }
    /**
     * Update or modifiy a rule with given name
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param name
     *            Name of a rule
     * @param rule
     *            Rule Configuration in JSON or XML format
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/firewall/default/update/rule1
     *
     * Request body in XML:
     * &lt;rule&gt;
     *         &#x20;&#x20;&#x20;&lt;name&gt;rule1&lt;/name&gt;
     *         &#x20;&#x20;&#x20;&lt;priority&gt;2&lt;/priority&gt;
     *         &#x20;&#x20;&#x20;&lt;etherType&gt;0x800&lt;/etherType&gt;
     *         &#x20;&#x20;&#x20;&lt;nwSrc&gt;10.0.0.1&lt;/nwSrc&gt;
     *         &#x20;&#x20;&#x20;&lt;nwDst&gt;10.0.0.6&lt;/nwDst&gt;
     *         &#x20;&#x20;&#x20;&lt;action&gt;ALLOW&lt;/action&gt;
     * &lt;/rule&gt;
     *
     * Request body in JSON:
      * {
     *    "name":"rule1",
     *    "priority":"2",
     *    "etherType":"0x800",
     *    "nwSrc":"10.0.0.1",
     *    "nwDst":"10.0.0.6",
     *    "action":"ALLOW"
     * }
     * </pre>
     */
    /*
    @Path("/{containerName}/update/{name}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Firewall rule modified successfully"),
        @ResponseCode(code = 201, condition = "rule Config processed successfully"),
        @ResponseCode(code = 400, condition = "Failed to create firewall rule due to invalid firewall rule configuration"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The Container Name or nodeId is not found"),
        @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
        @ResponseCode(code = 409, condition = "Failed to create firewall rule due to Conflicting Name or configuration"),
        @ResponseCode(code = 500, condition = "Failed to create firewall rule. Failure Reason included in HTTP Error response"),
        @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response updateRule(@PathParam(value = "containerName") String containerName,
            @PathParam(value = "name") String name, @TypeHint(FirewallRule.class) FirewallRule rule) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleDefaultDisabled(containerName);
        IFirewall frw = getFirewallService(containerName);
        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Status status=frw.updateRule(rule);
        if(status.isSuccess()){
          NorthboundUtils.auditlog("rule", username, "updated", containerName);
          return NorthboundUtils.getResponse(status);
        }
    return NorthboundUtils.getResponse(status);
    }*/
    @Path("/{containerName}/update/{id}")
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Firewall rule modified successfully"),
        @ResponseCode(code = 201, condition = "rule Config processed successfully"),
        @ResponseCode(code = 400, condition = "Failed to create firewall rule due to invalid firewall rule configuration"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The Container Name or nodeId is not found"),
        @ResponseCode(code = 406, condition = "Cannot operate on Default Container when other Containers are active"),
        @ResponseCode(code = 409, condition = "Failed to create firewall rule due to Conflicting Name or configuration"),
        @ResponseCode(code = 500, condition = "Failed to create firewall rule. Failure Reason included in HTTP Error response"),
        @ResponseCode(code = 503, condition = "One or more of Controller services are unavailable") })
    public Response updateRule(@PathParam(value = "containerName") String containerName,
            @PathParam(value = "id") String id, @TypeHint(FirewallRule.class) FirewallRule rule) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleDefaultDisabled(containerName);
        IFirewall frw = getFirewallService(containerName);
        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Status status=frw.updateRule(id,rule);
        if(status.isSuccess()){
          NorthboundUtils.auditlog("rule", username, "updated", containerName);
          return NorthboundUtils.getResponse(status);
        }
    return NorthboundUtils.getResponse(status);
    }
    /**
     * Get a rule
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param name
     *            Name of a rule
     * @return Firewall Rule configuration matching the name on a Container
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/firewall/default/get/rule1
     *
     * Response body in XML:
     * &lt;rule&gt;
     *         &#x20;&#x20;&#x20;&lt;name&gt;rule1&lt;/name&gt;
     *         &#x20;&#x20;&#x20;&lt;priority&gt;2&lt;/priority&gt;
     *         &#x20;&#x20;&#x20;&lt;etherType&gt;0x800&lt;/etherType&gt;
     *         &#x20;&#x20;&#x20;&lt;nwSrc&gt;10.0.0.1&lt;/nwSrc&gt;
     *         &#x20;&#x20;&#x20;&lt;nwDst&gt;10.0.0.6&lt;/nwDst&gt;
     *         &#x20;&#x20;&#x20;&lt;action&gt;ALLOW&lt;/action&gt;
     * &lt;/rule&gt;
     *
     * Response body in JSON:
     * {
     *    "name":"rule1",
     *    "priority":"2",
     *    "etherType":"0x800",
     *    "nwSrc":"10.0.0.1",
     *    "nwDst":"10.0.0.6",
     *    "action":"ALLOW"
     * }
     * </pre>
     */
    @Path("/{containerName}/get/{id}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public FirewallRule getRule(@PathParam(value="containerName") String containerName,@PathParam("id") String id) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        IFirewall frw = getFirewallService(containerName);
        FirewallRule r=frw.getFirewallRule(id);
        if (r == null) {
            throw new ResourceNotFoundException(RestMessages.NOPOLICY.toString());
        }
        return new FirewallRule(r);
    }
    /**
     * Returns a list of Firewall rules configured on the given container
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return
     *        List of rules configured on a given container
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/firewall/default/rules
     *
     * Response body in XML:
     * &lt;list&gt;
     *     &#x20;&#x20;&#x20;&lt;firewallRule&gt;
     *         &#x20;&#x20;&#x20;&lt;name&gt;rule1&lt;/name&gt;
     *         &#x20;&#x20;&#x20;&lt;priority&gt;2&lt;/priority&gt;
     *         &#x20;&#x20;&#x20;&lt;etherType&gt;0x800&lt;/etherType&gt;
     *         &#x20;&#x20;&#x20;&lt;nwSrc&gt;10.0.0.1&lt;/nwSrc&gt;
     *         &#x20;&#x20;&#x20;&lt;nwDst&gt;10.0.0.6&lt;/nwDst&gt;
     *         &#x20;&#x20;&#x20;&lt;action&gt;ALLOW&lt;/action&gt;
     *     &#x20;&#x20;&#x20;&lt;/firewallRule&gt;
     * &lt;/list&gt;
     *
     * Response body in JSON:
     * {
     *   "firewallRule": [
     *      {
     *         "name":"rule1",
     *         "priority":"2",
     *         "etherType":"0x800",
     *         "nwSrc":"10.0.0.1",
     *         "nwDst":"10.0.0.6",
     *         "action":"ALLOW"
     *      }
     *   ]
     * }
     * </pre>
     */
    @Path("/{containerName}/get")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public FirewallRules getAllRule(@PathParam(value="containerName") String containerName){
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleDefaultDisabled(containerName);
        List<FirewallRule> firewallRules = getFirewallRulesInternal(containerName);
        return new FirewallRules(firewallRules);
    }
    /**
     * Returns a list of Firewall rules id given rule name
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return
     *        List of rule id given rule name configured on a given container
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/firewall/default/getid/rule1
     *
     * </pre>
     */
    @Path("/{containerName}/getid/{ruleName}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public String getId(@PathParam(value="containerName") String containerName,@PathParam(value="ruleName") String ruleName){
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleDefaultDisabled(containerName);
        IFirewall frw = getFirewallService(containerName);

        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        String idSet="";
        idSet=frw.getRuleName_Id(ruleName);
        return idSet+"\n";
    }
    @Path("/{containerName}/getid")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({ @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Map<String,String> getIds(@PathParam(value="containerName") String containerName){
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleDefaultDisabled(containerName);
        IFirewall frw = getFirewallService(containerName);

        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Map<String,String> idSet=frw.getRuleName_Ids();
        return idSet;
    }
    /**
     * Delete a rule configuation
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @param name
     *            Name of the Firewall Rule configuration (Eg. 'rule1')
     * @return Response as dictated by the HTTP Response code
     *
     *         <pre>
     *
     * Example:
     *
     * RequestURL:
     * http://localhost:8080/controller/nb/v2/firewall/default/delete/rule1
     *
     * </pre>
     */
    @Path("/{containerName}/delete/{id}")
    @DELETE
    @StatusCodes({
        @ResponseCode(code = 204, condition = "Firewall Rule deleted successfully"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The Container Name is not found"),
        @ResponseCode(code = 406, condition = "Failed to delete Firewall Rule due to invalid operation. Failure details included in HTTP Error response"),
        @ResponseCode(code = 500, condition = "Failed to delete Firewall Rule. Failure Reason included in HTTP Error response"),
        @ResponseCode(code = 503, condition = "One or more of Controller service is unavailable") })
    public Response deleteRule(@PathParam(value = "containerName") String containerName,
            @PathParam(value = "id") String id) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleDefaultDisabled(containerName);

        IFirewall frw = getFirewallService(containerName);

        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

//        Node node = handleNodeAvailability(containerName, nodeType, nodeId);
        Status status = frw.removeFirewallRule(id);
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Firewall Rule", username, "removed",
                     containerName);
            return Response.status(Response.Status.NO_CONTENT).entity("Success! FirewallRule deleted."+"\n").build();
        }
        return NorthboundUtils.getResponse(status);
    }
    /**
     * Delete all rules
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return Response as dictated by the HTTP Response code
     *
     *         <pre>
     *
     * Example:
     *
     * RequestURL:
     * http://localhost:8080/controller/nb/v2/firewall/default/delete
     *
     * </pre>
     */
    @Path("/{containerName}/delete")
    @DELETE
    @StatusCodes({
        @ResponseCode(code = 204, condition = "Firewall Rules deleted successfully"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The Container Name is not found"),
        @ResponseCode(code = 406, condition = "Failed to delete all Firewall Rules due to invalid operation. Failure details included in HTTP Error response"),
        @ResponseCode(code = 500, condition = "Failed to delete all Firewall Rules. Failure Reason included in HTTP Error response"),
        @ResponseCode(code = 503, condition = "One or more of Controller service is unavailable") })
    public Response deleteRules(@PathParam(value = "containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        handleDefaultDisabled(containerName);

        IFirewall frw = getFirewallService(containerName);

        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }

//        Node node = handleNodeAvailability(containerName, nodeType, nodeId);
        Status status=new Status(StatusCode.BADREQUEST);
        for (FirewallRule conf : frw.getRuleConfigList().values()){
            status=frw.removeFirewallRule(conf.getId());
        }
        if (status.isSuccess()) {
            NorthboundUtils.auditlog("Firewall Rule", username, "removed",
                     containerName);
            return Response.status(Response.Status.NO_CONTENT).entity("Success! All firewallRules deleted"+"\n").build();
        }
        return NorthboundUtils.getResponse(status);
    }
    /**
     * Save the current firewall rules
     * @param containerName
     *            Name of the Container (Eg. 'default')
     * @return Response as dictated by the HTTP Response Status code
     *
     *         <pre>
     *
     * Example:
     *
     * Request URL:
     * http://localhost:8080/controller/nb/v2/firewall/default/save
     *
     * </pre>
     */
    @Path("/{containerName}/save")
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @StatusCodes({
        @ResponseCode(code = 200, condition = "Operation successful"),
        @ResponseCode(code = 401, condition = "User not authorized to perform this operation"),
        @ResponseCode(code = 404, condition = "The containerName is not found"),
        @ResponseCode(code = 500, condition = "Failed to save switch configuration. Failure Reason included in HTTP Error response"),
        @ResponseCode(code = 503, condition = "One or more of Controller Services are unavailable") })
    public Response saveFirewallRule(@PathParam("containerName") String containerName) {
        if (!NorthboundUtils.isAuthorized(getUserName(), containerName, Privilege.WRITE, this)) {
            throw new UnauthorizedException("User is not authorized to perform this operation on container "
                    + containerName);
        }
        IFirewall frw = getFirewallService(containerName);
        if (frw == null) {
            throw new ServiceUnavailableException("Firewall " + RestMessages.SERVICEUNAVAILABLE.toString());
        }
        Status ret = frw.saveFirewallRule();
        if (ret.isSuccess()) {
            NorthboundUtils.auditlog("Firewall Rule", username, "saved on",
                    containerName);
            return Response.status(Response.Status.NO_CONTENT).entity("Success! FirewallRule saved"+"\n").build();
        }
        return NorthboundUtils.getResponse(ret);
    }
    private List<FirewallRule> getFirewallRulesInternal(String containerName) {

        IFirewall frw = (IFirewall) ServiceHelper
                .getInstance(IFirewall.class, containerName,
                        this);

        if (frw == null) {
            throw new ResourceNotFoundException(RestMessages.NOCONTAINER
                    .toString());
        }

        List<FirewallRule> firewallrules = new ArrayList<FirewallRule>();

        for (FirewallRule conf : frw.getRuleConfigList()
                .values()) {
            firewallrules.add(conf);
        }
        return firewallrules;
    }

    private void handleResourceCongruence(String resource, String configured) {
        if (!resource.equals(configured)) {
            throw new MethodNotAllowedException("Path's resource name conflicts with payload's resource name");
        }
    }
    private void handleDefaultDisabled(String containerName) {
        IContainerManager containerManager = (IContainerManager) ServiceHelper
                .getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            throw new InternalServerErrorException(RestMessages.INTERNALERROR
                    .toString());
        }
        if (containerName.equals(GlobalConstants.DEFAULT.toString())
                && containerManager.hasNonDefaultContainer()) {
            throw new NotAcceptableException(RestMessages.DEFAULTDISABLED
                    .toString());
        }
    }
}
