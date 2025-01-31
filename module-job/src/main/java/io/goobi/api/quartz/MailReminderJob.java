package io.goobi.api.quartz;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang.StringUtils;
import org.goobi.api.mail.SendMail;
import org.goobi.beans.User;
import org.goobi.production.flow.jobs.AbstractGoobiJob;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.persistence.managers.MySQLHelper;
import de.sub.goobi.persistence.managers.UserManager;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.log4j.Log4j2;

@WebListener
@Log4j2
public class MailReminderJob extends AbstractGoobiJob {

    @Override
    public String getJobName() {
        return "MailReminderJob";
    }

    @Override
    public void execute() {

        // check current date
        LocalDateTime current = LocalDateTime.now();

        XMLConfiguration conf = ConfigPlugins.getPluginConfig("intranda_administration_deliveryManagement");
        conf.setExpressionEngine(new XPathExpressionEngine());
        String sqlMonthly = conf.getString("/reminder/query[@type='month']");
        String sqlQuarterly = conf.getString("/reminder/query[@type='quarter']");
        String sqlYearly = conf.getString("/reminder/query[@type='year']");
        String mailSubject = conf.getString("/reminder/subject");
        String mailBody = conf.getString("/reminder/body");

        // get email addresses of all users with activated for the period
        List<User> emails = new ArrayList<>();
        // if first day of month
        if (current.isEqual(current.with(TemporalAdjusters.firstDayOfMonth()))) {
            //        if (current.isEqual(current)) {
            List<User> emailAddresses = getUserFromDatabase(sqlMonthly);
            emails.addAll(emailAddresses);
        }

        // ... quarter
        if (current.get(IsoFields.DAY_OF_QUARTER) == 1) {
            List<User> emailAddresses = getUserFromDatabase(sqlQuarterly);
            emails.addAll(emailAddresses);
        }

        // ... year
        if (current.isEqual(current.with(TemporalAdjusters.firstDayOfYear()))) {
            List<User> emailAddresses = getUserFromDatabase(sqlYearly);
            emails.addAll(emailAddresses);
        }
        String url = SendMail.getInstance().getConfig().getApiUrl().replace("api/mails/disable", "uii/external_index.xhtml");
        // send reminder mail
        if (!emails.isEmpty()) {
            for (User user : emails) {
                if (StringUtils.isNotBlank(user.getEmail())) {
                    String messageSubject = mailSubject;
                    String messageBody = mailBody.replace("{login}", user.getLogin())
                            .replace("{name}", user.getVorname() + " " + user.getNachname())
                            .replace("{url}", url);
                    SendMail.getInstance().sendMailToUser(messageSubject, messageBody, user.getEmail());
                }
            }
        }

    }

    private List<User> getUserFromDatabase(String sql) {
        List<User> emailAddressess = null;

        Connection connection = null;
        try {
            connection = MySQLHelper.getInstance().getConnection();
            QueryRunner run = new QueryRunner();
            emailAddressess = run.query(connection, sql, UserManager.resultSetToUserListHandler);
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (connection != null) {
                try {
                    MySQLHelper.closeConnection(connection);
                } catch (SQLException e) {
                }
            }
        }
        return emailAddressess;
    }
}
