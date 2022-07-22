package io.goobi.managedbeans;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private static final String CONFIGURATION_NAME = "intranda_administration_deliveryManagement";

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
    private String firstname;
    @Getter
    @Setter
    private String lastname;
    @Getter
    @Setter
    private boolean privacyTextAccepted = false;
    @Getter
    @Setter
    private String wizzardMode = "page1";

    @Getter
    @Setter
    transient User currentUser;
    @Getter
    @Setter
    private String uiStatus = "";

    @Getter
    @Setter
    private String institutionName;

    @Getter
    private boolean institutionNameInvalid = false;

    @Getter
    private String privacyStatement;

    // third page
    @Getter
    private boolean userIsContactPerson;

    @Getter
    private boolean displaySecondContact = false;

    @Getter
    @Setter
    private boolean activation;

    @Getter
    private String privacyLink;

    @Getter
    private String legalNoticeLink;

    private static final String COMBO_FIELDNAME = "combo";

    private String registrationMailRecipient;
    private String registrationMailSubject;
    private String registrationMailBody;

    @Getter
    private boolean accountNameValid = true;
    @Getter
    private String accountNameErrorMessage;

    @Getter
    private boolean firstNameValid = true;
    @Getter
    private String firstNameErrorMessage;

    @Getter
    private boolean lastNameValid = true;
    @Getter
    private String lastNameErrorMessage;

    @Getter
    private boolean emailValid = true;
    @Getter
    private String emailErrorMessage;

    @Getter
    private boolean passwordValid = true;
    @Getter
    private String passwordErrorMessage;

    @Getter
    private boolean privacyValid = true;
    @Getter
    private String privacyErrorMessage;

    // additional fields, stored in a map with page number as key and list of fields as value
    @Getter
    transient Map<String, List<UserCreationField>> additionalFields = new HashMap<>();

    public ExternalLoginBean() {
        readConfiguration();
    }

    private void readConfiguration() {
        XMLConfiguration conf = ConfigPlugins.getPluginConfig(CONFIGURATION_NAME);
        conf.setExpressionEngine(new XPathExpressionEngine());
        privacyStatement = conf.getString("/privacyStatement");

        privacyLink = conf.getString("/privacyLink");
        legalNoticeLink = conf.getString("/legalNoticeLink");

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
            if (ucf.getFieldType().equals("dropdown") || ucf.getFieldType().equals(COMBO_FIELDNAME)) {
                List<String> valueList = Arrays.asList(hc.getStringArray("/value"));
                ucf.setSelectItemList(valueList);
            }

            configuredFields.add(ucf);
            additionalFields.put(ucf.getPosition(), configuredFields);

        }

        registrationMailRecipient = conf.getString("/registration/recipient");
        registrationMailSubject = conf.getString("/registration/subject");
        registrationMailBody = conf.getString("/registration/body");

    }

    public void createAccount() {
        accountNameValid = true;
        firstNameValid = true;
        lastNameValid = true;
        emailValid = true;
        passwordValid = true;
        // validate entries
        if (StringUtils.isBlank(accountName)) {
            accountNameErrorMessage = Helper.getTranslation("keinLoginAngegeben");
            accountNameValid = false;
            return;
        } else {
            // check that account name only uses valid characters
            if (!isLoginValide(accountName)) {
                accountNameErrorMessage = Helper.getTranslation("loginWrongCharacter");
                accountNameValid = false;
                return;
            }
            // check that the account name was not used yet
            String query = "login='" + StringEscapeUtils.escapeSql(accountName) + "'";

            try {
                int num = new UserManager().getHitSize(null, query, null);
                if (num > 0) {
                    accountNameErrorMessage = Helper.getTranslation("loginBereitsVergeben");
                    accountNameValid = false;
                    return;
                }
            } catch (DAOException e) {
                log.error(e);
            }
        }

        if (StringUtils.isBlank(firstname)) {
            firstNameValid = false;
            firstNameErrorMessage = Helper.getTranslation("plugin_rest_usercreation_requiredField");
            return;
        }

        if (StringUtils.isBlank(lastname)) {
            lastNameValid = false;
            lastNameErrorMessage = Helper.getTranslation("plugin_rest_usercreation_requiredField");
            return;
        }

        // check that email address is valid?
        if (!EmailValidator.getInstance().isValid(emailAddress)) {
            emailValid = false;
            emailErrorMessage = Helper.getTranslation("emailNotValid");
            return;
        }
        if (StringUtils.isBlank(password)) {
            passwordValid = false;
            passwordErrorMessage = Helper.getTranslation("plugin_rest_usercreation_requiredField");
            return;
        }
        if (!password.equals(confirmPassword)) {
            passwordValid = false;
            passwordErrorMessage = Helper.getTranslation("plugin_rest_usercreation_new_account_confirmPasswordWrong");
            return;
        }
        // check password length

        if (!privacyTextAccepted) {
            privacyValid = false;
            privacyErrorMessage = Helper.getTranslation("plugin_rest_usercreation_new_account_privacyTextNotAcccepted");
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
        wizzardMode = "confirm";
    }

    public void createInstitution() {

        if (!activation) {
            Helper.setFehlerMeldung("plugin_rest_usercreation_new_account_activationNotSet");
            return;
        }

        Institution institution = new Institution();
        institution.setLongName(institutionName);
        institution.setAllowAllPlugins(true);
        institution.setAllowAllAuthentications(true);
        currentUser.setInstitution(institution);

        for (Entry<String, List<UserCreationField>> fields : additionalFields.entrySet()) {
            for (UserCreationField f : fields.getValue()) {
                if ("institution".equals(f.getType())) {
                    if (f.getFieldType().equals(COMBO_FIELDNAME) && f.getBooleanValue()) {
                        institution.getAdditionalData().put(f.getName(), f.getSubValue());
                    } else {
                        institution.getAdditionalData().put(f.getName(), f.getValue());
                    }
                } else {
                    if (f.getFieldType().equals(COMBO_FIELDNAME) && f.getBooleanValue()) {
                        currentUser.getAdditionalData().put(f.getName(), f.getSubValue());
                    } else {
                        currentUser.getAdditionalData().put(f.getName(), f.getValue());
                    }
                }
            }
        }

        try {
            InstitutionManager.saveInstitution(institution);
            institution.setShortName("inst_" + institution.getId());
            InstitutionManager.saveInstitution(institution);

            UserManager.saveUser(currentUser);
        } catch (DAOException e) {
            log.error(e);
        }

        // send mail to staff to activate account
        if (StringUtils.isNotBlank(registrationMailRecipient) && StringUtils.isNotBlank(registrationMailBody)) {
            SendMail.getInstance().sendMailToUser(registrationMailSubject, registrationMailBody.replace("{login}", accountName), emailAddress);

        }
        wizzardMode = "wait";
    }

    public static String createRandomPassword(int length) {
        SecureRandom r = new SecureRandom();
        StringBuilder password = new StringBuilder();
        while (password.length() < length) {
            // ASCII interval: [97 + 0, 97 + 25] => [97, 122] => [a, z]
            password.append((char) (r.nextInt(26) + 'a'));
        }
        return password.toString();
    }

    private boolean isLoginValide(String inLogin) {
        boolean valide = true;
        String patternStr = "[a-z0-9\\._-]+";
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
            // shortname: max 6 characters, A-Za-z0-9
            //            valid = validateInstitutionName(valid);

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

    //    private boolean validateInstitutionName(boolean valid) {
    //        if (StringUtils.isBlank(institutionShortName) || institutionShortName.length() > 6 || !institutionShortName.matches("[A-Za-z0-9]+")) {
    //            institutionShortNameInvalid = true;
    //            valid = false;
    //        } else {
    //            institutionShortNameInvalid = false;
    //
    //            // check that shortname is not in use
    //            String query = "select count(1) from institution where shortName = ?";
    //            Connection connection = null;
    //            try {
    //                connection = MySQLHelper.getInstance().getConnection();
    //                QueryRunner run = new QueryRunner();
    //                int number = run.query(connection, query, MySQLHelper.resultSetToIntegerHandler, institutionShortName);
    //                if (number > 0) {
    //                    valid = false;
    //                    institutionShortNameInUse = true;
    //                } else {
    //                    institutionShortNameInUse = false;
    //                }
    //            } catch (SQLException e) {
    //                log.error(e);
    //            } finally {
    //                if (connection != null) {
    //                    try {
    //                        MySQLHelper.closeConnection(connection);
    //                    } catch (SQLException e) {
    //                        log.error(e);
    //                    }
    //                }
    //            }
    //
    //        }
    //        return valid;
    //    }

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

    public void setUserIsContactPerson(boolean userIsContactPerson) {
        if (this.userIsContactPerson != userIsContactPerson) {
            this.userIsContactPerson = userIsContactPerson;
            if (userIsContactPerson) {

                List<UserCreationField> fields = additionalFields.get("page3");
                for (UserCreationField field : fields) {
                    if (field.getName().toLowerCase().contains("firstname")) {
                        field.setValue(currentUser.getVorname());
                    } else if (field.getName().toLowerCase().contains("lastname")) {
                        field.setValue(currentUser.getNachname());
                    } else if (field.getName().toLowerCase().contains("email")) {
                        field.setValue(currentUser.getEmail());
                    }
                }
            }
        }
    }

    public void createNewContact() {
        displaySecondContact = true;
    }
}
