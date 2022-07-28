package software.plusminus.audit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import software.plusminus.security.context.SecurityContext;

@Component
public class SecurityContextDeviceContext implements DeviceContext {

    @Autowired
    private SecurityContext securityContext;

    @Nullable
    @Override
    public String currentDevice() {
        return securityContext.get("device");
    }
}
