package com.example.webhooksql;

import com.example.webhooksql.client.GenerateWebhookRequest;
import com.example.webhooksql.client.GenerateWebhookResponse;
import com.example.webhooksql.client.SubmitSolutionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class WebhookSqlApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(WebhookSqlApplication.class);

    private final RestTemplate restTemplate;

    public WebhookSqlApplication(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public static void main(String[] args) {
        SpringApplication.run(WebhookSqlApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            log.info("Starting webhook generation flow...");

            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            GenerateWebhookRequest request = new GenerateWebhookRequest(
                    "Rhythm Gondaliya",        
                    "22BCE1548",           
                    "rvgondaliya01@gmail.com"  
            );

            ResponseEntity<GenerateWebhookResponse> response =
                    restTemplate.postForEntity(generateUrl, request, GenerateWebhookResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Failed to generate webhook. Status: {}", response.getStatusCode());
                return;
            }

            GenerateWebhookResponse body = response.getBody();
            log.info("Received webhook URL: {}", body.getWebhook());
            log.info("Received access token (JWT): {}", body.getAccessToken());

            String finalQuery =
                    "WITH high_earners AS (\n" +
                    "    SELECT DISTINCT\n" +
                    "        e.emp_id,\n" +
                    "        e.first_name || ' ' || e.last_name AS emp_name,\n" +
                    "        e.dob,\n" +
                    "        e.department,\n" +
                    "        p.payment_time::date AS payment_date\n" +
                    "    FROM employee e\n" +
                    "    JOIN payments p ON p.emp_id = e.emp_id\n" +
                    "    WHERE p.amount > 70000\n" +
                    "),\n" +
                    "dept_ages AS (\n" +
                    "    SELECT\n" +
                    "        h.department,\n" +
                    "        AVG(EXTRACT(YEAR FROM age(current_date, h.dob))) AS average_age\n" +
                    "    FROM high_earners h\n" +
                    "    GROUP BY h.department\n" +
                    "),\n" +
                    "dept_employees AS (\n" +
                    "    SELECT\n" +
                    "        h.department,\n" +
                    "        h.emp_name,\n" +
                    "        ROW_NUMBER() OVER (PARTITION BY h.department ORDER BY h.emp_name) AS rn\n" +
                    "    FROM high_earners h\n" +
                    ")\n" +
                    "SELECT\n" +
                    "    d.department_name,\n" +
                    "    da.average_age AS average_age,\n" +
                    "    STRING_AGG(de.emp_name, ', ' ORDER BY de.emp_name) AS employee_list\n" +
                    "FROM dept_ages da\n" +
                    "JOIN dept_employees de\n" +
                    "    ON de.department = da.department\n" +
                    "   AND de.rn <= 10\n" +
                    "JOIN department d\n" +
                    "    ON d.department_id = da.department\n" +
                    "GROUP BY d.department_id, d.department_name, da.average_age\n" +
                    "ORDER BY d.department_id DESC;";

            SubmitSolutionRequest solutionRequest = new SubmitSolutionRequest(finalQuery);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", body.getAccessToken());

            HttpEntity<SubmitSolutionRequest> entity = new HttpEntity<>(solutionRequest, headers);

            String webhookUrl = body.getWebhook();
            log.info("Submitting final SQL query to webhook: {}", webhookUrl);

            ResponseEntity<String> submitResponse =
                    restTemplate.postForEntity(webhookUrl, entity, String.class);

            log.info("Webhook submission status: {}", submitResponse.getStatusCode());
            log.info("Webhook response body: {}", submitResponse.getBody());

        } catch (RestClientException ex) {
            log.error("Error while calling webhook APIs", ex);
        } catch (Exception ex) {
            log.error("Unexpected error in application flow", ex);
        }
    }
}
