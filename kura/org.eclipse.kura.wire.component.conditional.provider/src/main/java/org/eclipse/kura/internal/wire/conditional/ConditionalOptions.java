package org.eclipse.kura.internal.wire.conditional;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConditionalOptions {
    private static final Logger logger = LoggerFactory.getLogger(ConditionalOptions.class);
    
    private static final String CONDITION_PROPERTY_KEY = "condition";
    
    private static final String DEFAULT_CONDITION_PROPERTY_KEY = "condition";
    
    private final Map<String, Object> properties;

    /**
     * Instantiates a new cloud publisher options.
     *
     * @param properties
     *            the properties
     */
    ConditionalOptions(final Map<String, Object> properties) {
        requireNonNull(properties, "Properties must be not null");
        this.properties = properties;
    }
    
    
    CompiledScript getCompiledBooleanExpression(ScriptEngine scriptEngine){
        String booleanExpression = (String) properties.getOrDefault(CONDITION_PROPERTY_KEY, DEFAULT_CONDITION_PROPERTY_KEY);
        
        CompiledScript compiledBooleanExpression = null;
        try {
            compiledBooleanExpression = ((Compilable) scriptEngine).compile(booleanExpression);
        } catch (ScriptException e) {
            logger.warn("Error compiling script");
        }
        return compiledBooleanExpression;
    }

}
