package software.plusminus.audit.service;

import org.springframework.lang.Nullable;

public interface DeviceContext {

    @Nullable
    String currentDevice();

}
