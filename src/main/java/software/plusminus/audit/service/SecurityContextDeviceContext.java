package software.plusminus.audit.service;

import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import software.plusminus.context.Context;
import software.plusminus.security.Security;

@AllArgsConstructor
@Component
public class SecurityContextDeviceContext implements DeviceContext {

    private Context<Security> securityContext;

    @Nullable
    @Override
    public String currentDevice() {
        return securityContext.get().getOthers().get("device");
    }
}
