package com.anywide.dawdler.clientplug.web.session.conf;

import javax.annotation.Resource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.anywide.dawdler.clientplug.web.session.SpringBootDawdlerSessionFilter;

@Configuration
public class FilterConfig {
	@Resource
	SpringBootDawdlerSessionFilter springBootDawdlerSessionFilter;
    @Bean
    public FilterRegistrationBean filterRegistration1() {
        FilterRegistrationBean registration = new FilterRegistrationBean(springBootDawdlerSessionFilter);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
