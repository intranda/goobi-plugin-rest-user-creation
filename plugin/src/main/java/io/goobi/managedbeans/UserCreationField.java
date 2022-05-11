package io.goobi.managedbeans;

import java.util.List;

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
    private String validationErrorDescription;

    private String value;

    private List<String> selectItemList;


    public void setBooleanValue(boolean val) {
        if (val) {
            value ="true";
        } else {
            value="false";
        }
    }

    public boolean getBooleanValue() {
        return StringUtils.isNotBlank(value) && "true".equals(value);
    }
}
