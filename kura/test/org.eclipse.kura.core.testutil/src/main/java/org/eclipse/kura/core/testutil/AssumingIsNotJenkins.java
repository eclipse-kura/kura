package org.eclipse.kura.core.testutil;

import java.io.File;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class AssumingIsNotJenkins implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                File tempFile = new File("/tmp/isJenkins.txt");
                if (tempFile.exists()) {
                    throw new AssumptionViolatedException("Jenkins detected. Skipping tests!");
                } else {
                    base.evaluate();
                }
            }
        };
    }
}
