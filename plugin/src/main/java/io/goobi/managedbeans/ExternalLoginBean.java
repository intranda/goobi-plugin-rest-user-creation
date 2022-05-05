package io.goobi.managedbeans;

import java.io.Serializable;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.goobi.api.mail.SendMail;
import org.goobi.beans.Institution;
import org.goobi.beans.Ldap;
import org.goobi.beans.User;
import org.goobi.beans.User.UserStatus;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.NavigationForm;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.InstitutionManager;
import de.sub.goobi.persistence.managers.LdapManager;
import de.sub.goobi.persistence.managers.UserManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Named
@SessionScoped
@Log4j2
public class ExternalLoginBean implements Serializable {

    private static final long serialVersionUID = 2129311315931276111L;

    @Getter
    @Setter
    private String accountName;
    @Getter
    @Setter
    private String emailAddress;
    @Getter
    @Setter
    private String firstname;
    @Getter
    @Setter
    private String lastname;
    @Getter
    @Setter
    private String address;

    public void createAccount() {
        // validate entries
        if (StringUtils.isBlank(accountName)) {
            Helper.setFehlerMeldung("keinLoginAngegeben");
            return;
        } else {
            // check that account name only uses valid characters
            if (!isLoginValide(accountName)) {
                Helper.setFehlerMeldung("loginNotValid");
                return;
            }
            // check that the account name was not used yet
            String query = "login='" + StringEscapeUtils.escapeSql(accountName) + "'";

            try {
                int num = new UserManager().getHitSize(null, query, null);
                if (num > 0) {
                    Helper.setFehlerMeldung("loginBereitsVergeben");
                    return;
                }
            } catch (DAOException e) {
                log.error(e);
            }
        }

        // check that email address is valid?
        if (!EmailValidator.getInstance().isValid(emailAddress)) {
            Helper.setFehlerMeldung("emailNotValid");
            return;
        }

        // create new user
        User user = new User();
        user.setVorname(firstname);
        user.setNachname(lastname);
        user.setLogin(accountName);
        user.setLdaplogin("");
        user.setStatus(UserStatus.REGISTERED);
        user.setEmail(emailAddress);

        user.getAdditionalData().put("address", address);

        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        Object salt = rng.nextBytes();
        user.setPasswordSalt(salt.toString());

        Institution i = InstitutionManager.getInstitutionByName(ConfigurationHelper.getInstance().getExternalUserDefaultInstitutionName());
        user.setInstitution(i);

        try {
            Ldap ldap = LdapManager.getLdapById(0);
            user.setLdapGruppe(ldap);
        } catch (DAOException e1) {
            log.error(e1);
        }

        // TODO default dashboard plugin

        // generate random password
        int length = ConfigurationHelper.getInstance().getMinimumPasswordLength() + 10;
        String password = createRandomPassword(length);

        user.setEncryptedPassword(user.getPasswordHash(password));
        // save user

        try {
            UserManager.saveUser(user);
        } catch (DAOException e) {
            log.error(e);
        }

        // send mail with password

        String messageSubject = SendMail.getInstance().getConfig().getUserCreationMailSubject();
        String messageBody =
                SendMail.getInstance().getConfig().getUserCreationMailBody().replace("{password}", password).replace("{login}", accountName);
        SendMail.getInstance().sendMailToUser(messageSubject, messageBody, emailAddress);

        // change ui status

        NavigationForm form = (NavigationForm) Helper.getBeanByName("NavigationForm", NavigationForm.class);
        form.getUiStatus().put("loginStatus", "");

    }

    public static String createRandomPassword(int length) {
        Random r = new Random();
        StringBuilder password = new StringBuilder();
        while (password.length() < length) {
            // ASCII interval: [97 + 0, 97 + 25] => [97, 122] => [a, z]
            password.append((char) (r.nextInt(26) + 'a'));
        }
        return password.toString();
    }

    private boolean isLoginValide(String inLogin) {
        boolean valide = true;
        String patternStr = "[A-Za-z0-9@_\\-.]*";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(inLogin);
        valide = matcher.matches();

        return valide;
    }
}
