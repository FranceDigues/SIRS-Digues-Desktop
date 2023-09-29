package fr.sirs.util;

import static fr.sirs.util.JRColumnParameter.DisplayPolicy.LABEL;

/**
 *
 * @author Samuel Andrés (Geomatys) <samuel.andres at geomatys.com>
 */
public class JRColumnParameter {

    public enum DisplayPolicy{LABEL, REFERENCE_CODE, REFERENCE_LABEL_AND_CODE};

    private final String fieldName;
    private final float columnWidthCoeff;
    private final DisplayPolicy displayPolicy;
    private final boolean bold;
    private final String headerName;

    public JRColumnParameter(final String fieldName, final float columnWidthCoeff, final DisplayPolicy displayPolicy, final boolean bold, final String headerName) {
        this.fieldName = fieldName;
        this.columnWidthCoeff = columnWidthCoeff;
        this.displayPolicy = displayPolicy;
        this.bold = bold;
        this.headerName = headerName;
    }

    public JRColumnParameter(final String fieldName, final float columnWidthCoeff, final DisplayPolicy displayPolicy, final boolean bold) {
        this(fieldName, columnWidthCoeff, displayPolicy, bold, null);
    }

    public JRColumnParameter(final String fieldName, final float columnWidthCoeff, final boolean bold) {
        this(fieldName, columnWidthCoeff, LABEL, bold);
    }

    public JRColumnParameter(final String fieldName, final float columnWidthCoeff, final DisplayPolicy displayPolicy) {
        this(fieldName, columnWidthCoeff, displayPolicy, false);
    }

    public JRColumnParameter(final String fieldName, final float columnWidthCoeff, final String headerName) {
        this(fieldName, columnWidthCoeff, LABEL, false, headerName);
    }

    public JRColumnParameter(final String fieldName, final float columnWidthCoeff) {
        this(fieldName, columnWidthCoeff, LABEL);
    }

    public JRColumnParameter(final String fieldName, final boolean bold) {
        this(fieldName, 1.f, LABEL, bold);
    }

    public JRColumnParameter(final String fieldName) {
        this(fieldName, 1.f, LABEL);
    }

    public String getFieldName() {return fieldName;}
    public float getColumnWidthCoeff() {return columnWidthCoeff;}
    public DisplayPolicy getDisplayPolicy() {return displayPolicy;}
    public boolean isBold() {return bold;}
    public String getHeaderName() {return headerName;}
}
