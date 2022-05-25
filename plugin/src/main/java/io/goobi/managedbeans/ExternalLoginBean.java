package io.goobi.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.naming.ConfigurationException;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
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

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.NavigationForm;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.JwtHelper;
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

    private static final String configurationName = "intranda_administration_deliveryManagement";

    // fields for first page
    @Getter
    @Setter
    private String accountName;
    @Getter
    @Setter
    private String password;
    @Getter
    @Setter
    private String confirmPassword;
    @Getter
    @Setter
    private String emailAddress;
    @Getter
    @Setter
    private String confirmEmailAddress;
    @Getter
    @Setter
    private String firstname;
    @Getter
    @Setter
    private String lastname;
    @Getter
    @Setter
    private boolean privacyTextAccepted;
    @Getter
    @Setter
    private String wizzardMode = "page1";

    @Getter
    @Setter
    private User currentUser;
    @Getter
    @Setter
    private String uiStatus = "";

    //    http://localhost:8080/goobi/api/users/email/eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJwdXJwb3NlIjoiY29uZmlybU1haWwiLCJpc3MiOiJHb29iaSIsImlkIjoiMjciLCJleHAiOjE2NTIyMTE0OTUsInVzZXIiOiJyb2JlcnQifQ.SKkARFPmmgRz-eKKecOIHPOkk6VQuGt7jYsU5iqIvSg

    // second page
    @Getter
    @Setter
    private String institutionName;

    @Getter
    private boolean institutionNameInvalid = false;

    // additional fields, stored in a map with page number as key and list of fields as value
    @Getter
    private Map<String, List<UserCreationField>> additionalFields = new HashMap<>();

    public ExternalLoginBean() {
        readConfiguration();
    }

    private void readConfiguration() {
        XMLConfiguration conf = ConfigPlugins.getPluginConfig(configurationName);
        conf.setExpressionEngine(new XPathExpressionEngine());

        List<HierarchicalConfiguration> fields = conf.configurationsAt("/fields/field");

        for (HierarchicalConfiguration hc : fields) {
            UserCreationField ucf = new UserCreationField();
            ucf.setType(hc.getString("@type"));

            ucf.setDisplayInTable(hc.getBoolean("@displayInTable", false));
            ucf.setFieldType(hc.getString("@fieldType", "input"));
            ucf.setLabel(hc.getString("@label"));
            ucf.setName(hc.getString("@name"));
            ucf.setPosition(hc.getString("@position", ""));
            ucf.setRequired(hc.getBoolean("@required", false));
            ucf.setValidation(hc.getString("@validation", null));
            ucf.setValidationErrorMessage(hc.getString("@validationErrorDescription", null));
            ucf.setHelpMessage(hc.getString("@helpMessage"));
            List<UserCreationField> configuredFields = additionalFields.get(ucf.getPosition());
            if (configuredFields == null) {
                configuredFields = new ArrayList<>();
            }
            if (ucf.getFieldType().equals("dropdown") || ucf.getFieldType().equals("combo")) {
                List<String> valueList = Arrays.asList(hc.getStringArray("/value"));
                ucf.setSelectItemList(valueList);
            }

            configuredFields.add(ucf);
            additionalFields.put(ucf.getPosition(), configuredFields);
        }

    }

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

        if (!emailAddress.equals(confirmEmailAddress)) {
            Helper.setFehlerMeldung("emailNotValid");
            return;
        }

        if (!password.equals(confirmPassword)) {
            Helper.setFehlerMeldung("TODO");
            return;
        }
        // check password length

        if (!privacyTextAccepted) {
            Helper.setFehlerMeldung("TODO");
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

        user.getAdditionalData().put("privacyAccpeted", String.valueOf(privacyTextAccepted));

        RandomNumberGenerator rng = new SecureRandomNumberGenerator();
        Object salt = rng.nextBytes();
        user.setPasswordSalt(salt.toString());

        Institution i = InstitutionManager.getInstitutionByName(ConfigurationHelper.getInstance().getExternalUserDefaultInstitutionName());
        user.setInstitution(i);

        try {
            Ldap ldap = LdapManager.getLdapByName(ConfigurationHelper.getInstance().getExternalUserDefaultAuthenticationType());
            user.setLdapGruppe(ldap);
        } catch (DAOException e1) {
            log.error(e1);
        }

        user.setStandort("-");

        user.setEncryptedPassword(user.getPasswordHash(password));

        user.setDashboardPlugin("intranda_dashboard_delivery");

        try {
            UserManager.saveUser(user);
        } catch (DAOException e) {
            log.error(e);
        }

        // send mail with password
        // generate confirmation link
        Map<String, String> tokenMap = new HashMap<>();
        tokenMap.put("purpose", "confirmMail");
        tokenMap.put("id", "" + user.getId());
        tokenMap.put("user", user.getLogin());

        try {
            String token = JwtHelper.createToken(tokenMap);

            String url = SendMail.getInstance().getConfig().getApiUrl().replace("mails/disable", "users/email/" + token);
            String messageSubject = SendMail.getInstance().getConfig().getUserCreationMailSubject();
            String messageBody = SendMail.getInstance()
                    .getConfig()
                    .getUserCreationMailBody()
                    .replace("{password}", password)
                    .replace("{login}", accountName)
                    .replace("{url}", url);
            SendMail.getInstance().sendMailToUser(messageSubject, messageBody, emailAddress);
        } catch (ConfigurationException e) {
            log.error(e);
        }

        // change ui status

        NavigationForm form = (NavigationForm) Helper.getBeanByName("NavigationForm", NavigationForm.class);
        form.getUiStatus().put("loginStatus", "");

    }

    public void createInstitution() {

        Institution institution = new Institution();
        institution.setLongName(institutionName);
        institution.setShortName(institutionName);
        institution.setAllowAllPlugins(true);
        institution.setAllowAllAuthentications(true);
        currentUser.setInstitution(institution);

        for (String pageNumber : additionalFields.keySet()) {
            List<UserCreationField> fields = additionalFields.get(pageNumber);
            for (UserCreationField f : fields) {
                if ("institution".equals(f.getType())) {
                    if (f.getFieldType().equals("combo") && f.getBooleanValue()) {
                        institution.getAdditionalData().put(f.getName(), f.getSubValue());
                    } else {
                        institution.getAdditionalData().put(f.getName(), f.getValue());
                    }
                } else {
                    if (f.getFieldType().equals("combo") && f.getBooleanValue()) {
                        currentUser.getAdditionalData().put(f.getName(), f.getSubValue());
                    } else {
                        currentUser.getAdditionalData().put(f.getName(), f.getValue());
                    }
                }
            }
        }

        try {
            InstitutionManager.saveInstitution(institution);
            UserManager.saveUser(currentUser);
        } catch (DAOException e) {
            log.error(e);
        }

        // TODO send mail to staff to activate account

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

    public void next() {
        switch (wizzardMode) {
            case "page2":
                if (validateFields("page2")) {
                    wizzardMode = "page3";
                }
                break;
            case "page3":
                if (validateFields("page3")) {
                    wizzardMode = "page4";
                }
                break;
            case "page4":
                if (validateFields("page4")) {
                    wizzardMode = "page5";
                }
                break;
            default:
                break;
        }
    }

    private boolean validateFields(String pageName) {

        boolean valid = true;

        if ("page2".equals(pageName)) {
            // validate institution name, not empty and max 255 character
            if (StringUtils.isBlank(institutionName) || institutionName.length() > 255) {
                institutionNameInvalid = true;
                valid = false;
            } else {
                institutionNameInvalid = false;
            }
        }

        List<UserCreationField> fields = additionalFields.get(pageName);
        for (UserCreationField field : fields) {
            if (!field.validateValue()) {
                valid = false;
            }
        }
        return valid;

    }

    public void back() {
        switch (wizzardMode) {
            case "page3":
                wizzardMode = "page2";
                break;
            case "page4":
                wizzardMode = "page3";
                break;
            case "page5":
                wizzardMode = "page4";
                break;
            default:
                break;
        }
    }

}
