package com.mybill.MyBill_Backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class AppStartupConfigValidator implements InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(AppStartupConfigValidator.class);

    private final Environment environment;

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;

    public AppStartupConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isDev = activeProfiles.contains("dev");
        if ("update".equalsIgnoreCase(ddlAuto) && !isDev) {
            log.error("CRITICAL SECURITY ERROR: Detected spring.jpa.hibernate.ddl-auto=update outside the 'dev' profile! " +
                    "This setting allows hibernate to alter database tables on startup, causing schema instability and potential data loss/corruption. " +
                    "Failsafe activated: stopping the application bootstrap process.");
            throw new IllegalStateException("Startup aborted: spring.jpa.hibernate.ddl-auto can only be set to 'update' under the 'dev' profile.");
        }

        // The 'dev' profile enables permissive settings (wildcard CORS with credentials,
        // ddl-auto=update, verbose logging, public API docs). It must never run in a
        // production/cloud deployment. Render sets the RENDER env variable for all services.
        boolean deployedToRender = "true".equalsIgnoreCase(System.getenv("RENDER"));
        if (isDev && deployedToRender) {
            log.error("CRITICAL SECURITY ERROR: The 'dev' profile is active on a Render deployment. " +
                    "This configuration is forbidden in production because it enables permissive, unsafe settings. " +
                    "Failsafe activated: stopping the application bootstrap process.");
            throw new IllegalStateException("Startup aborted: the 'dev' profile cannot be used on Render.");
        }
    }
}
