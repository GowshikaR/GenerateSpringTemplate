package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.List;

@Service
public class EventSubscriptionService {
   /*
    @Value("${tmf632.eventsubscription.get-all}")
    private String getAllUrl;
    */
/*
    @Value("${tmf632.eventsubscription.get-by-id}")
    private String getByIdUrl;
*/

    @Value("${tmf632.eventsubscription.create}")
    private String createUrl;

/*
    @Value("${tmf632.eventsubscription.patch}")
    private String updateUrl;
*/

    @Value("${tmf632.eventsubscription.delete}")
    private String deleteUrl;

    private final RestTemplate restTemplate;

    public EventSubscriptionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /*
    public List<?> getAll() {
        ResponseEntity<List> response = restTemplate.getForEntity(getAllUrl, List.class);
        return response.getBody();
    }
    */

    /*
    public Object getById(String id) {
        String url = getByIdUrl.replace("{id}", id);
        return restTemplate.getForObject(url, Object.class);
    }
    */

    
    public Object create(Object requestObject) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<Object> requestEntity = new HttpEntity<>(requestObject, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(createUrl, requestEntity, Object.class);
        return response.getBody();
    }
    

    /*
    public Object patch(String id, Object requestObject) {
        String url = updateUrl.replace("{id}", id);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<Object> requestEntity = new HttpEntity<>(requestObject, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(url, requestEntity, Object.class);
        return response.getBody();
    }
    */
    /*
        public Object put(String id, Object requestObject) {
            String url = updateUrl.replace("{id}", id);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<Object> requestEntity = new HttpEntity<>(requestObject, headers);
            ResponseEntity<Object> response = restTemplate.postForEntity(url, requestEntity, Object.class);
            return response.getBody();
        }
        */

    
    public void delete(String id) {
        String url = deleteUrl.replace("{id}", id);
        restTemplate.delete(url);
    }
    
}
