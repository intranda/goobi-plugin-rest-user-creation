package io.goobi.managedbeans;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;

import lombok.Data;

@Data
public class UserCreationField {

    private String type;
    private boolean displayInTable;
    private String fieldType;
    private String label;
    private String name;
    private String position;
    private boolean required;
    private String validation;
    private String validationErrorMessage;

    private String value;
    private String subValue;
    private List<SelectItem> selectItemList = new ArrayList<>();

    private boolean validationError = false;

    private String helpMessage;

    private String shortLabel;

    private String placeholderText;

    public void setBooleanValue(boolean val) {
        if (val) {
            value = "true";
        } else {
            value = "false";
        }
    }

    public boolean getBooleanValue() {
        return StringUtils.isNotBlank(value) && "true".equals(value);
    }

    public boolean validateValue() {

        String val = value;

        if (fieldType.equals("combo") && getBooleanValue() && StringUtils.isBlank(subValue)) {
            validationError = true;
            return false;
        }

        if (StringUtils.isNotBlank(subValue)) {
            val = subValue;
        }

        validationError = false;
        if (required && StringUtils.isBlank(val)) {
            validationError = true;
            return false;
        }

        if (StringUtils.isNotBlank(validation) && StringUtils.isNotBlank(val) && !val.matches(validation)) {
            validationError = true;
            return false;
        }
        return true;
    }

    public boolean isDisplayHelpButton() {
        return StringUtils.isNotBlank(helpMessage);
    }
}
