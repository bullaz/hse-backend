package com.stellarix.hse.configuration;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stellarix.hse.entity.Commentaire;
import com.stellarix.hse.entity.Hse;
import com.stellarix.hse.entity.MesureControle;
import com.stellarix.hse.entity.Question;
import com.stellarix.hse.entity.Toko5;
import com.stellarix.hse.repository.QuestionRepository;
import com.stellarix.hse.repository.Toko5Repository;
import com.stellarix.hse.service.AccountService;

@Configuration
@Slf4j
public class Config {

    @Bean
    CommandLineRunner commandLineRunner(AccountService accountService, QuestionRepository questionRepository, Toko5Repository toko5Repository){
        return args -> {        	
        	try {
        		
        		
        		/*insert HSE accounts here for now*/
        		/**/
//        		Hse user = new Hse(0,"andersonmahosi@gmail.com","Mahosy","Anderson","bulla","motdepasse2002");
//            	accountService.addUser(user);
//            	
//            	log.info("after adding hse account");
//        		
//        		List<Question> listQuestion = questionRepository.findByCategorieAndRequired("risk",false);
//
//        		log.info(listQuestion.toString());
//        		
//        		List<Commentaire> listComs = new ArrayList<Commentaire>();
//        		listComs.add(new Commentaire(null,null,"Pierre","Jean","Commentaire 1 de Jean Pierre"));
//        		listComs.add(new Commentaire(null,null,"Pierre","Jean","Commentaire 2 de Jean Pierre"));
//        			
//        		List<MesureControle> listControle  = new ArrayList<MesureControle>();
//        		listControle.add(new MesureControle(null,null,listQuestion.get(1),"mesure prise 1",true));
//        		listControle.add(new MesureControle(null,null,listQuestion.get(3),"mesure prise 2",true));
//        		
//        		Toko5 test = new Toko5(UUID.randomUUID(),"Rakoto","Jean",LocalDateTime.now(),"valide", listComs, listControle);
//        		test = toko5Repository.save(test);
//        		log.info(test.toString());

        		
//        		Toko5 toko51 = new Toko5(UUID.randomUUID(),"Rakoto","Jean",LocalDateTime.now(),"valide", null, null);
//        		
//        		List<Commentaire> listComs = new ArrayList<Commentaire>();
//        		listComs.add(new Commentaire(null,toko51,"Pierre","Jean","Commentaire 1 de Jean Pierre"));
//        		listComs.add(new Commentaire(null,toko51,"Pierre","Jean","Commentaire 2 de Jean Pierre"));
//        		
//        		List<MesureControle> listControle  = new ArrayList<MesureControle>();
//        		listControle.add(new MesureControle(null,toko51,listQuestion.get(1),"mesure prise 1",true));
//        		listControle.add(new MesureControle(null,toko51,listQuestion.get(3),"mesure prise 2",true));
//        		
//        		toko51.setListCommentaire(listComs);
//        		toko51.setListMesureControle(listControle);
//        		
//        		toko51 = toko5Repository.save(toko51);
        		
        		//List<Toko5> list = toko5Repository.findAll();
        		
        		
//        		List<Toko5> test = toko5Repository.findAll();
//        		
//        		log.info(test.getFirst().toString());
//        		
        		
        	}catch(Exception e) {
        		log.error("some errors in the config class", e);
        	}
        };
    }
}
