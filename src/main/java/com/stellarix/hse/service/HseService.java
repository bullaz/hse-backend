package com.stellarix.hse.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stellarix.hse.entity.TaskDetail;

@Service
public class HseService {
	
	
	public Map<String,String> generateTaskCodes(TaskDetail task) {
//		Principal company abbreviation should be in the database so the app could be used not only for stellarix
		Map<String,String> map = new HashMap<String, String>();
//		map.put("supervisor", );
		return null;
	}
}
