package rs.ac.ftn.isa.isabackend.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {
    // Spring će automatski pronaći ehcache.xml u resources folderu
}