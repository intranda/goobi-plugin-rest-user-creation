package io.goobi.managedbeans;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data

@AllArgsConstructor
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

}
