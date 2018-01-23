package org.eclipse.kura.internal.wire.math.singleport;

import java.util.Map;

public class AbstractSingleportMathComponentOptions {

    private static final String OPERAND_NAME_PROP_NAME = "operand.name";
    private static final String RESULT_NAME_PROP_NAME = "result.name";
    private static final String WINDOW_SIZE_PROPERTY_NAME = "window.size";

    private static final String OPERAND_NAME_DEFAULT = "operand";
    private static final String RESULT_NAME_DEFAULT = "result";
    private static final int WINDOW_SIZE_DEFAULT = 10;

    private String operandName = OPERAND_NAME_DEFAULT;
    private String resultName = RESULT_NAME_DEFAULT;
    private int windowSize = WINDOW_SIZE_DEFAULT;

    public AbstractSingleportMathComponentOptions(final Map<String, Object> properties) {

    }

    public String getOperandName() {
        return operandName;
    }

    public String getResultName() {
        return resultName;
    }

    public int getWindowSize() {
        return windowSize;
    }
}
