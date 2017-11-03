package org.eclipse.kura.web.shared.model;

public class GwtModelTest {

    public String labelTest1;
    public String labelTest2;
    public String labelTest3;

    public String getLabelTest1() {
        return labelTest1;
    }

    public void setLabelTest1(String labelTest1) {
        this.labelTest1 = labelTest1;
    }

    public String getLabelTest2() {
        return labelTest2;
    }

    public void setLabelTest2(String labelTest2) {
        this.labelTest2 = labelTest2;
    }

    public String getLabelTest3() {
        return labelTest3;
    }

    public void setLabelTest3(String labelTest3) {
        this.labelTest3 = labelTest3;
    }

    public GwtModelTest() {
        super();
    }

    public GwtModelTest(String labelTest1, String labelTest2, String labelTest3) {
        super();
        this.labelTest1 = labelTest1;
        this.labelTest2 = labelTest2;
        this.labelTest3 = labelTest3;
    }
}
