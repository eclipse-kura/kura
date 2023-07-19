package org.eclipse.kura.rest.position.api;

public class IsLockedDTO {

    boolean islocked;

    public IsLockedDTO(boolean islocked) {
        this.islocked = islocked;
    }

    public boolean getIsLocked() {
        return this.islocked;
    }
    
}
