package software.plusminus.audit.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import software.plusminus.security.SecurityParameterProvider;

import java.util.AbstractMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

@Component
public class DeviceSecurityParameterProvider implements SecurityParameterProvider {

    @Autowired
    private HttpServletRequest request;

    @Override
    @Nullable
    public Map.Entry<String, String> provideParameter() {
        String device = request.getParameter("device");
        if (device == null) {
            return null;
        }
        return new AbstractMap.SimpleEntry<>("device", device);
    }
}
