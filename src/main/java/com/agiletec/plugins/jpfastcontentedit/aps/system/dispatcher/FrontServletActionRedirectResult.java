/*
*
* Copyright 2005 AgileTec s.r.l. (http://www.agiletec.it) All rights reserved.
*
* This file is part of jAPS software.
* jAPS is a free software; 
* you can redistribute it and/or modify it
* under the terms of the GNU General Public License (GPL) as published by the Free Software Foundation; version 2.
* 
* See the file License for the specific language governing permissions   
* and limitations under the License
* 
* 
* 
* Copyright 2005 AgileTec s.r.l. (http://www.agiletec.it) All rights reserved.
*
*/
package com.agiletec.plugins.jpfastcontentedit.aps.system.dispatcher;

import com.agiletec.aps.system.ApsSystemUtils;
import com.agiletec.aps.system.RequestContext;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.system.services.page.Page;
import com.agiletec.aps.system.services.url.IURLManager;
import com.agiletec.aps.tags.InternalServletTag;
import com.agiletec.aps.util.ApsWebApplicationUtils;
import com.agiletec.apsadmin.system.dispatcher.ServletActionRedirectResultWithAnchor;
import java.util.Arrays;
import java.util.List;

import org.apache.struts2.dispatcher.ServletRedirectResult;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;
import com.opensymphony.xwork2.util.reflection.ReflectionExceptionHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.struts2.ServletActionContext;

/**
 * Redirect Action Result with ancot for internal servlet actions.
 * @author E.Santoboni
 */
public class FrontServletActionRedirectResult extends ServletRedirectResult implements ReflectionExceptionHandler {
	

    private static final Logger LOG = LoggerFactory.getLogger(ServletActionRedirectResultWithAnchor.class);
    
    protected String actionName;
    protected String namespace;
    protected String method;
    protected String _anchorDest;

    public FrontServletActionRedirectResult() {
        super();
    }

    public FrontServletActionRedirectResult(String actionName) {
        this(null, actionName, null, null);
    }

    public FrontServletActionRedirectResult(String actionName, String method) {
        this(null, actionName, method, null);
    }


    public FrontServletActionRedirectResult(String namespace, String actionName, String method) {
        this(namespace, actionName, method, null);
    }

    public FrontServletActionRedirectResult(String namespace, String actionName, String method, String anchor) {
        super(null, anchor);
        this.namespace = namespace;
        this.actionName = actionName;
        this.method = method;
    }
    
    /**
     * @see com.opensymphony.xwork2.Result#execute(com.opensymphony.xwork2.ActionInvocation)
     */
    @Override
    public void execute(ActionInvocation invocation) throws Exception {
    	try {
            this.actionName = this.conditionalParse(this.actionName, invocation);
            if (this.namespace == null) {
                    this.namespace = invocation.getProxy().getNamespace();
            } else {
                    this.namespace = this.conditionalParse(this.namespace, invocation);
            }
            if (this.method == null) {
                    this.method = "";
            } else {
                    this.method = this.conditionalParse(this.method, invocation);
            }
            
            String anchorDest = null;
            Map<String, String> redirectParams = new HashMap<String, String>();
            ResultConfig resultConfig = invocation.getProxy().getConfig().getResults().get(invocation.getResultCode());
            if (resultConfig != null ) {
                this.extractResultParams(redirectParams, resultConfig, invocation);
                anchorDest = this.extractAnchorDest(resultConfig, invocation);
            }
            
            HttpServletRequest request = ServletActionContext.getRequest();
            RequestContext reqCtx = (RequestContext) request.getAttribute(RequestContext.REQCTX);
            this.extractInternalServletParams(redirectParams, reqCtx);
            
            IURLManager urlManager = (IURLManager) ApsWebApplicationUtils.getBean(SystemConstants.URL_MANAGER, request);
            Page currentPage = (Page) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_PAGE);
            Lang currentLang = (Lang) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_LANG);
            String url = urlManager.createUrl(currentPage, currentLang, redirectParams);
            url = url.replaceAll("&amp;", "&");
            if (null != anchorDest) {
                url += "#" + anchorDest;
            }
            this.setLocation(url);
        } catch (Throwable t) {
            ApsSystemUtils.logThrowable(t, this, "execute");
        }
        super.execute(invocation);
    }
    
    protected void extractResultParams(Map<String, String> redirectParams, ResultConfig resultConfig, ActionInvocation invocation) {
        Map resultConfigParams = resultConfig.getParams();
        for (Iterator i = resultConfigParams.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            if (!this.getProhibitedResultParams().contains(e.getKey())) {
                String potentialValue = e.getValue() == null ? "" : conditionalParse(e.getValue().toString(), invocation);
                if (!supressEmptyParameters || ((potentialValue != null) && (potentialValue.length() > 0))) {
                    redirectParams.put(e.getKey().toString(), potentialValue);
                }
            }
        }
    }
    
    protected String extractAnchorDest(ResultConfig resultConfig, ActionInvocation invocation) {
        Map<String, String> resultConfigParams = resultConfig.getParams();
        for (Iterator i = resultConfigParams.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            if (e.getKey().equals("anchorDest")) {
                String potentialValue = e.getValue() == null ? "" : conditionalParse(e.getValue().toString(), invocation);
                if (potentialValue != null && potentialValue.length() > 0) {
                    return potentialValue;
                }
            }
        }
        return null;
    }
    
    protected void extractInternalServletParams(Map<String, String> redirectParams, RequestContext reqCtx) {
        String actionPath = "/ExtStr2" + namespace + "/" + actionName;
        Integer currentFrame = (Integer) reqCtx.getExtraParam(SystemConstants.EXTRAPAR_CURRENT_FRAME);
        redirectParams.put(InternalServletTag.REQUEST_PARAM_ACTIONPATH, actionPath);
        redirectParams.put(InternalServletTag.REQUEST_PARAM_FRAMEDEST, currentFrame.toString());
    }
    
    /**
     * Sets the action name
     * @param actionName The name
     */
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
    
    /**
     * Sets the namespace
     * @param namespace The namespace
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
    /**
     * Sets the method
     * @param method The method
     */
    public void setMethod(String method) {
        this.method = method;
    }
    
    protected String getAnchorDest() {
		return _anchorDest;
	}
    
	/**
     * Sets the anchor destination
     * @param method The anchor destination
     */
    public void setAnchorDest(String anchorDest) {
		this._anchorDest = anchorDest;
	}
    
    @Override
    protected List<String> getProhibitedResultParams() {
        return Arrays.asList(new String[]{
                DEFAULT_PARAM, "namespace", "method", "encode", "parse", "location",
                "prependServletContext", "supressEmptyParameters", "anchor", "anchorDest"});
    }
    
}