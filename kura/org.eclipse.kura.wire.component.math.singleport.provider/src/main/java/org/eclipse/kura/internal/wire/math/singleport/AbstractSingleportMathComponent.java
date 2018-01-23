package org.eclipse.kura.internal.wire.math.singleport;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.type.TypedValue;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireEnvelope;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireRecord;
import org.eclipse.kura.wire.WireSupport;
import org.osgi.service.wireadmin.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSingleportMathComponent
        implements WireEmitter, WireReceiver, ConfigurableComponent, Function<TypedValue<?>, TypedValue<?>> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSingleportMathComponentOptions.class);

    private WireHelperService wireHelperService;
    private WireSupport wireSupport;
    protected AbstractSingleportMathComponentOptions options;

    public void setWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = wireHelperService;
    }

    public void unsetWireHelperService(final WireHelperService wireHelperService) {
        this.wireHelperService = null;
    }

    public void activated(final Map<String, Object> properties) {
        this.wireSupport = this.wireHelperService.newWireSupport(this);
        updated(properties);
    }

    public void updated(final Map<String, Object> properties) {
        this.options = getOptions(properties);
        init();
    }

    public void deactivated() {

    }

    protected abstract void init();

    protected AbstractSingleportMathComponentOptions getOptions(final Map<String, Object> properties) {
        return new AbstractSingleportMathComponentOptions(properties);
    }

    @Override
    public Object polled(Wire wire) {
        return wireSupport.polled(wire);
    }

    @Override
    public void consumersConnected(Wire[] wires) {
        wireSupport.consumersConnected(wires);
    }

    @Override
    public void updated(Wire wire, Object value) {
        wireSupport.updated(wire, value);
    }

    @Override
    public void producersConnected(Wire[] wires) {
        wireSupport.consumersConnected(wires);
    }

    @Override
    public void onWireReceive(WireEnvelope wireEnvelope) {
        final List<WireRecord> records = wireEnvelope.getRecords();
        if (records.isEmpty()) {
            logger.warn("Received empty envelope");
            return;
        }
        final TypedValue<?> operand = records.get(0).getProperties().get(this.options.getOperandName());
        if (operand == null) {
            logger.warn("Missing operand");
            return;
        }
        final TypedValue<?> result = this.apply(operand);
        this.wireSupport.emit(Collections
                .singletonList(new WireRecord(Collections.singletonMap(this.options.getResultName(), result))));
    }

}
