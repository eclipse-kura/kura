package org.eclipse.kura.example.wire.math.singleport.minimum;

import org.eclipse.kura.example.wire.math.singleport.AbstractSingleportMathComponent;
import org.eclipse.kura.example.wire.math.singleport.RunningExtremes;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.type.TypedValues;

public class MinimumComponent extends AbstractSingleportMathComponent {
    
    private RunningExtremes runningExtremes;
    
    @Override
    protected void init() {
        this.runningExtremes = null;
    }

    @Override
    public TypedValue<?> apply(TypedValue<?> t) {
        if (runningExtremes == null) {
            this.runningExtremes = new RunningExtremes(this.options.getWindowSize());
        }
        final double value = ((Number) t.getValue()).doubleValue();
        return TypedValues.newDoubleValue(this.runningExtremes.updateAndGetMin(value));
    }

}
