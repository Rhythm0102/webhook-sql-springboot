# Webhook SQL Spring Boot Solution

This project is a Spring Boot console application that:

1. On startup, calls the `generateWebhook` API to get a webhook URL and JWT access token.
2. Prepares the final SQL query answer for the assignment.
3. Sends the SQL query to the returned webhook URL using the JWT in the `Authorization` header.

No HTTP controller or endpoint is exposed; everything runs automatically on application startup.

## Tech Stack

- Java 17
- Spring Boot 3
- Maven
- RestTemplate for HTTP calls

## Final SQL Query (for reference)

```sql
WITH high_earners AS (
    SELECT DISTINCT
        e.emp_id,
        e.first_name || ' ' || e.last_name AS emp_name,
        e.dob,
        e.department,
        p.payment_time::date AS payment_date
    FROM employee e
    JOIN payments p ON p.emp_id = e.emp_id
    WHERE p.amount > 70000
),
dept_ages AS (
    SELECT
        h.department,
        AVG(EXTRACT(YEAR FROM age(current_date, h.dob))) AS average_age
    FROM high_earners h
    GROUP BY h.department
),
dept_employees AS (
    SELECT
        h.department,
        h.emp_name,
        ROW_NUMBER() OVER (PARTITION BY h.department ORDER BY h.emp_name) AS rn
    FROM high_earners h
)
SELECT
    d.department_name,
    da.average_age AS average_age,
    STRING_AGG(de.emp_name, ', ' ORDER BY de.emp_name) AS employee_list
FROM dept_ages da
JOIN dept_employees de
    ON de.department = da.department
   AND de.rn <= 10
JOIN department d
    ON d.department_id = da.department
GROUP BY d.department_id, d.department_name, da.average_age
ORDER BY d.department_id DESC;
```

This query:

- Filters salary payments to only those with `amount > 70000`.
- Finds unique employees per department who qualify.
- Computes the average age of those employees per department.
- Builds a comma-separated list of **up to 10** employee names (`FIRST_NAME || ' ' || LAST_NAME`) per department.
- Orders the output by `department_id` in descending order.

(PostgreSQL-compatible syntax is used: `STRING_AGG`, `age`, `EXTRACT`, window functions.)

## How to Run

### Prerequisites

- **Java 17** installed and on your PATH  
- **Maven 3.x** installed

You can check versions with:

```bash
java -version
mvn -version
```

### Steps

1. **Extract the ZIP**

   Unzip the archive you downloaded so that you have a folder, for example:

   ```text
   webhook-sql-springboot/
   ```

2. **Open a terminal/command prompt** in the project root (where `pom.xml` is located).

3. **Build the JAR**

   From the project root, run:

   ```bash
   mvn clean package
   ```

   After a successful build, the runnable JAR will be under:

   ```text
   target/webhook-sql-solution-0.0.1-SNAPSHOT.jar
   ```

4. **Run the application**

   Still from the project root, or from inside `target/`, run:

   ```bash
   java -jar target/webhook-sql-solution-0.0.1-SNAPSHOT.jar
   ```

   What happens:

   - The Spring Boot app starts.
   - On startup, it calls:
     `https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA`
   - It receives:
     - A `webhook` URL
     - An `accessToken` (JWT)
   - It then POSTs the final SQL query as JSON to the `webhook` with header:
     - `Authorization: <accessToken>`
     - `Content-Type: application/json`

   Logs in the console will show:
   - The received webhook URL
   - The access token (JWT)
   - The status and body of the webhook submission response

## Files Overview

- `pom.xml` – Maven configuration, dependencies, and build plugin
- `src/main/java/com/example/webhooksql/WebhookSqlApplication.java` – Main Spring Boot app and startup flow
- `src/main/java/com/example/webhooksql/client/GenerateWebhookRequest.java` – Request model for generateWebhook API
- `src/main/java/com/example/webhooksql/client/GenerateWebhookResponse.java` – Response model holding `webhook` and `accessToken`
- `src/main/java/com/example/webhooksql/client/SubmitSolutionRequest.java` – Request model for submitting the final SQL query
- `src/main/resources/application.properties` – Basic logging configuration
