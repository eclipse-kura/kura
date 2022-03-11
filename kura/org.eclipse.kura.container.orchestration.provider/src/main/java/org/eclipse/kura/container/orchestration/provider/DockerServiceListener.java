package org.eclipse.kura.container.orchestration.provider;


public interface DockerServiceListener {
    
    public void onConnect();
    
    public void onDisconnect();
    
    public void onDisabled();

}
