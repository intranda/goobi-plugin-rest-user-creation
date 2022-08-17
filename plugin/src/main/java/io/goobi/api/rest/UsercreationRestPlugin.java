package io.goobi.api.rest;

import java.io.IOException;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.goobi.beans.User;
import org.jboss.weld.contexts.SerializableContextualInstanceImpl;

import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import de.sub.goobi.forms.SessionForm;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.JwtHelper;
import de.sub.goobi.persistence.managers.UserManager;
import io.goobi.managedbeans.ExternalLoginBean;
import lombok.extern.log4j.Log4j2;

@Log4j2
@javax.ws.rs.Path("/users")
public class UsercreationRestPlugin {

    @Context
    private HttpServletRequest servletRequest;

    @Context
    private HttpServletResponse servletResponse;

    @Inject
    private SessionForm sessionForm;

    @javax.ws.rs.Path("/email/{token}")
    @GET
    @Produces("text/xml")
    public void verifyEmail(@PathParam("token") String token) {

        HttpSession session = servletRequest.getSession();

        ExternalLoginBean elb = (ExternalLoginBean) getBeanFromSession(session, ExternalLoginBean.class);
        try {
            // validate request
            DecodedJWT jwt = JwtHelper.verifyTokenAndReturnClaims(token);

            // extract user data from request
            String accountName = jwt.getClaim("user").asString();

            // log user in
            User user = UserManager.getUserByLogin(accountName);
            user.lazyLoad();

            elb.setCurrentUser(user);
            elb.setWizzardMode("page2");
            elb.setUiStatus("accountCreation");
            // forward to institution creation screen
            sessionForm.updateSessionUserName(servletRequest.getSession(), user);
            servletResponse.sendRedirect("/goobi/uii/external_index.xhtml");

        } catch (TokenExpiredException e) {
            log.error(e);
            // TODO delete user account, to start from scratch, forward to start page

        } catch (Exception e) {
            log.error(e);
            try {
                servletResponse.sendRedirect("/goobi/uii/logout.xhtml");
            } catch (IOException e1) {
                log.error(e1);
            }
        }
    }

    private static Object getBeanFromSession(HttpSession session, Class<?> clazz) {
        Enumeration<String> attribs = session.getAttributeNames();
        String attrib;
        while (attribs.hasMoreElements()) {
            attrib = attribs.nextElement();
            Object obj = session.getAttribute(attrib);
            if (obj instanceof SerializableContextualInstanceImpl) {
                @SuppressWarnings("rawtypes")
                SerializableContextualInstanceImpl impl = (SerializableContextualInstanceImpl) obj;
                if (impl.getClass().equals(clazz)) {
                    return impl.getInstance();
                }
            }
        }
        return Helper.getBeanByClass(clazz);
    }
}
