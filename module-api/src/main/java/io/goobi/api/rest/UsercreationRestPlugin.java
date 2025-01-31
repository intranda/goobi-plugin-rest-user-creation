package io.goobi.api.rest;

import java.io.IOException;
import java.util.Base64;
import java.util.Enumeration;

import org.goobi.beans.User;
import org.goobi.beans.User.UserStatus;
import org.jboss.weld.contexts.SerializableContextualInstanceImpl;
import org.json.JSONObject;

import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import de.sub.goobi.forms.SessionForm;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.JwtHelper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.UserManager;
import io.goobi.managedbeans.ExternalLoginBean;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import lombok.extern.log4j.Log4j2;

@Log4j2
@jakarta.ws.rs.Path("/users")
public class UsercreationRestPlugin {

    @Context
    private HttpServletRequest servletRequest;

    @Context
    private HttpServletResponse servletResponse;

    @Inject
    private SessionForm sessionForm;

    @jakarta.ws.rs.Path("/email/{token}")
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
            return;
        } catch (TokenExpiredException | SignatureVerificationException e) {
            log.error(e);
            Base64.Decoder decoder = Base64.getUrlDecoder();

            String[] parts = token.split("\\."); // Splitting header, payload and signature

            String payload = new String(decoder.decode(parts[1])); //Payload: {"purpose":"confirmMail","iss":"Goobi","id":"39","exp":1659142199,"user":"testaccount"}

            JSONObject obj = new JSONObject(payload);
            String username = obj.getString("user");
            int userId = obj.getInt("id");
            try {
                User user = UserManager.getUserById(userId);
                if (user != null && user.getLogin().equals(username) && user.getStatus() == UserStatus.REGISTERED) {
                    UserManager.deleteUser(user);
                }
            } catch (DAOException e1) {
                log.error(e);
            }

        } catch (Exception e) {
            log.error(e);
        }
        try {
            servletResponse.sendRedirect("/goobi/uii/external_index.xhtml");
        } catch (IOException e1) {
            log.error(e1);
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
