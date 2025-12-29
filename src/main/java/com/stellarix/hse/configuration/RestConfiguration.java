package com.stellarix.hse.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import com.stellarix.hse.entity.Societe;

@Configuration
public class RestConfiguration implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        // Expose IDs for specific entity classes
        config.exposeIdsFor(Societe.class); 
        
        // Alternatively, to expose IDs for all entities in a specific package/domain
        // config.exposeIdsFor(allEntityClasses); 
    }
}
