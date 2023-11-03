package io.goobi.managedbeans;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
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

    private String userCreationMailSubject;
    private String userCreationMailBody;

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

    private static final String NEWLINE = "<br />";
    private static final String COLON = ": ";

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
            ucf.setShortLabel(hc.getString("@alternativeLabel", hc.getString("@label")));
            ucf.setPlaceholderText(hc.getString("@placeholderText", ""));
            List<UserCreationField> configuredFields = additionalFields.get(ucf.getPosition());
            if (configuredFields == null) {
                configuredFields = new ArrayList<>();
            }
            if ("dropdown".equals(ucf.getFieldType()) || COMBO_FIELDNAME.equals(ucf.getFieldType())) {
                List<HierarchicalConfiguration> valueList = hc.configurationsAt("/selectfield");
                for (HierarchicalConfiguration v : valueList) {
                    SelectItem si = new SelectItem(v.getString("@value"), v.getString("@label"));
                    ucf.getSelectItemList().add(si);
                }
            }

            configuredFields.add(ucf);
            additionalFields.put(ucf.getPosition(), configuredFields);

        }

        registrationMailRecipient = conf.getString("/registration/recipient");
        registrationMailSubject = conf.getString("/registration/subject");
        registrationMailBody = conf.getString("/registration/body");

        userCreationMailSubject = conf.getString("/userCreation/subject");
        userCreationMailBody = conf.getString("/userCreation/body");
    }

    public void createAccount() {
        resetValues();
        // validate entries
        if (StringUtils.isBlank(accountName)) {
            accountNameErrorMessage = Helper.getTranslation("keinLoginAngegeben");
            accountNameValid = false;
        } else {
            // check that account name only uses valid characters
            if (!isLoginValide(accountName)) {
                accountNameErrorMessage = Helper.getTranslation("loginWrongCharacter");
                accountNameValid = false;
            }
            // check that the account name was not used yet
            String query = "login='" + StringEscapeUtils.escapeSql(accountName) + "'";

            try {
                int num = new UserManager().getHitSize(null, query, null);
                if (num > 0) {
                    accountNameErrorMessage = Helper.getTranslation("loginBereitsVergeben");
                    accountNameValid = false;
                }
            } catch (DAOException e) {
                log.error(e);
            }
        }

        if (StringUtils.isBlank(firstname)) {
            firstNameValid = false;
            firstNameErrorMessage = Helper.getTranslation("plugin_rest_usercreation_requiredField"); //NOSONAR
        }

        if (StringUtils.isBlank(lastname)) {
            lastNameValid = false;
            lastNameErrorMessage = Helper.getTranslation("plugin_rest_usercreation_requiredField");
        }

        // check that email address is valid?
        if (!EmailValidator.getInstance().isValid(emailAddress)) {
            emailValid = false;
            emailErrorMessage = Helper.getTranslation("emailNotValid");
        }
        if (StringUtils.isBlank(password)) {
            passwordValid = false;
            passwordErrorMessage = Helper.getTranslation("plugin_rest_usercreation_requiredField");
        } else if (!password.equals(confirmPassword)) {
            passwordValid = false;
            passwordErrorMessage = Helper.getTranslation("plugin_rest_usercreation_new_account_confirmPasswordWrong");
        } else {
            // check password length
            // The new password must fulfill the minimum password length (read from default configuration file)
            int minimumLength = ConfigurationHelper.getInstance().getMinimumPasswordLength();
            if (password.length() < minimumLength) {
                Helper.setFehlerMeldung("neuesPasswortNichtLangGenug", "" + minimumLength);
                return;
            }
        }
        if (!privacyTextAccepted) {
            privacyValid = false;
            privacyErrorMessage = Helper.getTranslation("plugin_rest_usercreation_new_account_privacyTextNotAcccepted");
        }

        if (!accountNameValid || !firstNameValid || !lastNameValid || !emailValid || !passwordValid || !privacyValid) {
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
            String messageSubject = userCreationMailSubject;
            String messageBody = userCreationMailBody.replace("{firstname}", user.getVorname())
                    .replace("{lastname}", user.getNachname())
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
            if ("page3a".equals(fields.getKey()) && !displaySecondContact) { //NOSONAR
                continue;
            }

            for (UserCreationField f : fields.getValue()) {
                if ("institution".equals(f.getType())) {
                    if (COMBO_FIELDNAME.equals(f.getFieldType()) && f.getBooleanValue()) {
                        institution.getAdditionalData().put(f.getName(), f.getSubValue());
                    } else {
                        institution.getAdditionalData().put(f.getName(), f.getValue());
                    }
                } else if (COMBO_FIELDNAME.equals(f.getFieldType()) && f.getBooleanValue()) {
                    currentUser.getAdditionalData().put(f.getName(), f.getSubValue());
                } else {
                    currentUser.getAdditionalData().put(f.getName(), f.getValue());
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
            String subject = Helper.getTranslation(registrationMailSubject);
            String body = Helper.getTranslation(registrationMailBody);
            body = body.replace("{login}", currentUser.getLogin())
                    .replace("{firstname}", currentUser.getVorname())
                    .replace("{lastname}", currentUser.getNachname());

            StringBuilder sb = new StringBuilder();
            sb.append(NEWLINE);
            sb.append(NEWLINE);

            // first page
            sb.append("Account");
            sb.append(NEWLINE);
            sb.append(Helper.getTranslation("plugin_rest_usercreation_userName"));
            sb.append(COLON);
            sb.append(currentUser.getLogin());
            sb.append(NEWLINE);

            sb.append(Helper.getTranslation("firstname"));
            sb.append(COLON);
            sb.append(currentUser.getVorname());
            sb.append(NEWLINE);
            sb.append(Helper.getTranslation("lastname"));
            sb.append(COLON);
            sb.append(currentUser.getNachname());
            sb.append(NEWLINE);
            sb.append(Helper.getTranslation("plugin_rest_usercreation_new_account_emailAddress"));
            sb.append(COLON);
            sb.append(currentUser.getEmail());
            sb.append(NEWLINE);
            sb.append(NEWLINE);
            sb.append("Institution");
            sb.append(NEWLINE);

            // second page
            sb.append(Helper.getTranslation("plugin_rest_usercreation_new_account_institutionName"));
            sb.append(COLON);
            sb.append(institutionName);
            sb.append(NEWLINE);

            for (UserCreationField field : additionalFields.get("page2")) { //NOSONAR
                sb.append(Helper.getTranslation(field.getLabel()));
                sb.append(COLON);
                sb.append(field.getValue());
                if (StringUtils.isNotBlank(field.getSubValue())) {
                    sb.append(", " + field.getSubValue());
                }
                sb.append(NEWLINE);
            }
            sb.append(NEWLINE);

            // third page
            sb.append("Contact");
            sb.append(NEWLINE);
            for (UserCreationField field : additionalFields.get("page3")) { //NOSONAR
                sb.append(Helper.getTranslation(field.getLabel()));
                sb.append(COLON);
                sb.append(field.getValue());
                if (StringUtils.isNotBlank(field.getSubValue())) {
                    sb.append(", " + field.getSubValue());
                }
                sb.append(NEWLINE);
            }

            if (displaySecondContact) {
                sb.append("Second Contact");
                sb.append(NEWLINE);
                for (UserCreationField field : additionalFields.get("page3a")) { //NOSONAR
                    sb.append(Helper.getTranslation(field.getLabel()));
                    sb.append(COLON);
                    sb.append(field.getValue());
                    if (StringUtils.isNotBlank(field.getSubValue())) {
                        sb.append(", " + field.getSubValue());
                    }
                    sb.append(NEWLINE);
                }
            }
            sb.append(NEWLINE);

            // page 4
            sb.append(Helper.getTranslation("plugin_rest_usercreation_new_additionalData"));
            sb.append(NEWLINE);
            for (UserCreationField field : additionalFields.get("page4")) { //NOSONAR
                sb.append(Helper.getTranslation(field.getLabel()));
                sb.append(COLON);
                sb.append(field.getValue());
                if (StringUtils.isNotBlank(field.getSubValue())) {
                    sb.append(", " + field.getSubValue());
                }
                sb.append(NEWLINE);
            }
            sb.append(NEWLINE);

            List<String> recipients = new ArrayList<>();
            recipients.add(registrationMailRecipient);
            recipients.add(currentUser.getEmail());

            SendMail.getInstance().sendMailToUser(subject, body + NEWLINE + sb.toString(), recipients, false);

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
            case "page2": //NOSONAR
                if (validateFields("page2")) {
                    wizzardMode = "page3"; //NOSONAR
                }
                break;
            case "page3":
                if (validateFields("page3")) {
                    wizzardMode = "page4"; //NOSONAR
                }
                break;
            case "page4":
                if (validateFields("page4")) {
                    wizzardMode = "page5"; //NOSONAR
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
        if ("page3".equals(pageName) && displaySecondContact) {
            List<UserCreationField> fields = additionalFields.get("page3a");
            for (UserCreationField field : fields) {
                if (!field.validateValue()) {
                    valid = false;
                }
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

    public void disableContact() {
        displaySecondContact = false;
        List<UserCreationField> ucfList = additionalFields.get("page3a");
        for (UserCreationField ucf : ucfList) {
            ucf.setValue("");
        }
    }

    public void resetValues() {
        accountNameValid = true;
        firstNameValid = true;
        lastNameValid = true;
        emailValid = true;
        passwordValid = true;
        privacyValid = true;
    }
}
