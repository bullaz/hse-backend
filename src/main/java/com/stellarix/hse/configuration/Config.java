package com.stellarix.hse.configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.service.AccountService;

@Configuration
@Slf4j
public class Config {

    @Bean
    CommandLineRunner commandLineRunner(AccountService accountService){
        return args -> {        	
        	try {
        		
        		/*insert HSE accounts here for now*/
        		/**/
//        		Hse user = new Hse(0,"andersonmahosi@gmail.com","Mahosy","Anderson","bulla","bulla");
//            	accountService.addUser(user);
//            	
//            	log.info("after adding hse account");
        	}catch(Exception e) {
        		log.error("Failed to add HSE account during startup", e);
        	}
        };
    }
}
