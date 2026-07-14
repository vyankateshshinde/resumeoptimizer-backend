package com.vyankatesh.resumeoptimizer.jobfinder.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "jobfinder.himalayas",
        name = "enabled",
        havingValue = "true"
)
public class HimalayasJobSourceProvider implements JobSourceProvider {

    private static final Logger log =
            LoggerFactory.getLogger(
                    HimalayasJobSourceProvider.class
            );

    private final RestTemplate restTemplate;
    private final String searchUrl;
    private final String country;
    private final boolean includeWorldwide;
    private final int pages;
    private final List<String> queries;

    public HimalayasJobSourceProvider(
            RestTemplateBuilder restTemplateBuilder,

            @Value(
                    "${jobfinder.himalayas.search-url:"
                            + "https://himalayas.app/jobs/api/search}"
            )
            String searchUrl,

            @Value("${jobfinder.himalayas.country:India}")
            String country,

            @Value("${jobfinder.himalayas.include-worldwide:true}")
            boolean includeWorldwide,

            @Value("${jobfinder.himalayas.pages:2}")
            int pages,

            @Value(
                    "${jobfinder.himalayas.queries:"
                            + "java developer,"
                            + "spring boot developer,"
                            + "java full stack developer,"
                            + "backend developer}"
            )
            String queries
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(25))
                .build();

        this.searchUrl = defaultIfBlank(
                searchUrl,
                "https://himalayas.app/jobs/api/search"
        );

        this.country = clean(country);
        this.includeWorldwide = includeWorldwide;

        this.pages = clamp(
                pages,
                1,
                5
        );

        this.queries = parseQueries(queries);
    }

    @Override
    public JobSource getSource() {
        return JobSource.HIMALAYAS;
    }

    @Override
    public List<ExternalJobRecord> fetchJobs(
            LocalDateTime postedAfter
    ) {
        Map<String, ExternalJobRecord> uniqueJobs =
                new LinkedHashMap<>();

        for (String query : queries) {
            for (int page = 1; page <= pages; page++) {

                List<ExternalJobRecord> jobs =
                        fetchPage(
                                query,
                                page,
                                postedAfter
                        );

                for (ExternalJobRecord job : jobs) {
                    uniqueJobs.putIfAbsent(
                            job.externalId(),
                            job
                    );
                }
            }
        }

        log.info(
                "Himalayas ingestion fetched {} unique recent jobs",
                uniqueJobs.size()
        );

        return List.copyOf(uniqueJobs.values());
    }

    private List<ExternalJobRecord> fetchPage(
            String query,
            int page,
            LocalDateTime postedAfter
    ) {
        try {
            UriComponentsBuilder builder =
                    UriComponentsBuilder
                            .fromHttpUrl(searchUrl)
                            .queryParam("q", query)
                            .queryParam("sort", "recent")
                            .queryParam("page", page);

            if (StringUtils.hasText(country)) {
                builder.queryParam(
                        "country",
                        country
                );
            }

            if (includeWorldwide) {
                builder.queryParam(
                        "worldwide",
                        true
                );
            }

            URI uri = builder
                    .build()
                    .encode()
                    .toUri();

            JsonNode response = restTemplate.getForObject(
                    uri,
                    JsonNode.class
            );

            if (response == null
                    || !response.path("jobs").isArray()) {

                log.warn(
                        "Himalayas returned no jobs array "
                                + "for query '{}' page {}",
                        query,
                        page
                );

                return List.of();
            }

            return mapJobs(
                    response.path("jobs"),
                    postedAfter
            );

        } catch (
                HttpClientErrorException.TooManyRequests exception
        ) {
            log.warn(
                    "Himalayas rate limit reached for "
                            + "query '{}' page {}",
                    query,
                    page
            );

            return List.of();

        } catch (RestClientResponseException exception) {
            log.warn(
                    "Himalayas request failed for query '{}' "
                            + "page {}. Status: {}",
                    query,
                    page,
                    exception.getStatusCode()
            );

            return List.of();

        } catch (ResourceAccessException exception) {
            log.warn(
                    "Himalayas connection failed for query '{}' "
                            + "page {}: {}",
                    query,
                    page,
                    exception.getMessage()
            );

            return List.of();

        } catch (Exception exception) {
            log.error(
                    "Unexpected Himalayas error for "
                            + "query '{}' page {}",
                    query,
                    page,
                    exception
            );

            return List.of();
        }
    }

    private List<ExternalJobRecord> mapJobs(
            JsonNode results,
            LocalDateTime postedAfter
    ) {
        List<ExternalJobRecord> jobs =
                new ArrayList<>();

        for (JsonNode result : results) {
            ExternalJobRecord job =
                    mapJob(
                            result,
                            postedAfter
                    );

            if (job != null) {
                jobs.add(job);
            }
        }

        return jobs;
    }

    private ExternalJobRecord mapJob(
            JsonNode result,
            LocalDateTime postedAfter
    ) {
        String externalId = readText(
                result,
                "guid"
        );

        String title = cleanHtml(
                readText(result, "title")
        );

        String company = cleanHtml(
                readText(result, "companyName")
        );

        String applicationLink = readText(
                result,
                "applicationLink"
        );

        String description = cleanHtml(
                readText(result, "description")
        );

        if (!StringUtils.hasText(description)) {
            description = cleanHtml(
                    readText(result, "excerpt")
            );
        }

        LocalDateTime postedAt =
                parseEpochDateTime(
                        result.path("pubDate")
                );

        if (postedAfter != null
                && postedAt != null
                && postedAt.isBefore(postedAfter)) {
            return null;
        }

        LocalDateTime expiryDate =
                parseEpochDateTime(
                        result.path("expiryDate")
                );

        if (expiryDate != null
                && expiryDate.isBefore(
                LocalDateTime.now()
        )) {
            return null;
        }

        if (!StringUtils.hasText(externalId)
                || !StringUtils.hasText(title)
                || !StringUtils.hasText(company)
                || !StringUtils.hasText(description)
                || !StringUtils.hasText(applicationLink)) {
            return null;
        }

        String location =
                buildLocation(result);

        EmploymentType employmentType =
                mapEmploymentType(
                        readText(
                                result,
                                "employmentType"
                        )
                );

        BigDecimal minimumSalary =
                readDecimal(
                        result,
                        "minSalary"
                );

        BigDecimal maximumSalary =
                readDecimal(
                        result,
                        "maxSalary"
                );

        String salaryPeriod = readText(
                result,
                "salaryPeriod"
        );

        minimumSalary = convertToAnnualSalary(
                minimumSalary,
                salaryPeriod
        );

        maximumSalary = convertToAnnualSalary(
                maximumSalary,
                salaryPeriod
        );

        String currency = defaultIfBlank(
                readText(result, "currency"),
                "USD"
        ).toUpperCase(Locale.ROOT);

        return new ExternalJobRecord(
                externalId,
                title,
                company,
                location,
                WorkArrangement.REMOTE,
                employmentType,
                null,
                null,
                minimumSalary,
                maximumSalary,
                currency,
                description,
                postedAt,
                JobSource.HIMALAYAS,
                "Himalayas",
                applicationLink
        );
    }

    private String buildLocation(JsonNode job) {
        JsonNode restrictions =
                job.path("locationRestrictions");

        if (restrictions.isArray()
                && !restrictions.isEmpty()) {

            List<String> locations =
                    new ArrayList<>();

            for (JsonNode restriction : restrictions) {
                String location = clean(
                        restriction.asText("")
                );

                if (StringUtils.hasText(location)) {
                    locations.add(location);
                }
            }

            if (!locations.isEmpty()) {
                return String.join(
                        ", ",
                        locations
                ) + " — Remote";
            }
        }

        return "Worldwide — Remote";
    }

    private EmploymentType mapEmploymentType(
            String value
    ) {
        String normalized =
                defaultIfBlank(value, "Other")
                        .toLowerCase(Locale.ROOT)
                        .replace("_", " ")
                        .replace("-", " ")
                        .trim();

        return switch (normalized) {
            case "full time", "fulltime" ->
                    EmploymentType.FULL_TIME;

            case "part time", "parttime" ->
                    EmploymentType.PART_TIME;

            case "contractor", "contract" ->
                    EmploymentType.CONTRACT;

            case "temporary" ->
                    EmploymentType.TEMPORARY;

            case "intern", "internship" ->
                    EmploymentType.INTERNSHIP;

            default ->
                    EmploymentType.OTHER;
        };
    }

    private BigDecimal convertToAnnualSalary(
            BigDecimal salary,
            String salaryPeriod
    ) {
        if (salary == null) {
            return null;
        }

        String period = defaultIfBlank(
                salaryPeriod,
                "annual"
        ).toLowerCase(Locale.ROOT);

        BigDecimal multiplier;

        switch (period) {
            case "hourly":
                multiplier = new BigDecimal("2080");
                break;

            case "weekly":
                multiplier = new BigDecimal("52");
                break;

            case "fortnightly":
                multiplier = new BigDecimal("26");
                break;

            case "monthly":
                multiplier = new BigDecimal("12");
                break;

            default:
                multiplier = BigDecimal.ONE;
                break;
        }

        return salary.multiply(multiplier);
    }

    private LocalDateTime parseEpochDateTime(
            JsonNode value
    ) {
        if (value == null
                || value.isNull()
                || value.isMissingNode()) {
            return null;
        }

        try {
            long epochSeconds;

            if (value.isNumber()) {
                epochSeconds = value.asLong();
            } else {
                String timestamp = clean(
                        value.asText("")
                );

                if (!StringUtils.hasText(timestamp)) {
                    return null;
                }

                epochSeconds = Long.parseLong(
                        timestamp
                );
            }

            return LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(epochSeconds),
                    ZoneId.systemDefault()
            );

        } catch (Exception exception) {
            log.debug(
                    "Unable to parse Himalayas timestamp: {}",
                    value
            );

            return null;
        }
    }

    private BigDecimal readDecimal(
            JsonNode node,
            String fieldName
    ) {
        JsonNode value = node.path(fieldName);

        if (value.isNumber()) {
            return value.decimalValue();
        }

        if (value.isTextual()
                && StringUtils.hasText(
                value.asText()
        )) {
            try {
                return new BigDecimal(
                        value.asText().trim()
                );

            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private String readText(
            JsonNode node,
            String fieldName
    ) {
        return clean(
                node.path(fieldName)
                        .asText("")
        );
    }

    private static List<String> parseQueries(
            String value
    ) {
        List<String> parsedQueries =
                Arrays.stream(
                                defaultIfBlank(
                                        value,
                                        "java developer"
                                ).split(",")
                        )
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();

        if (parsedQueries.isEmpty()) {
            return List.of(
                    "java developer"
            );
        }

        return parsedQueries;
    }

    private static String cleanHtml(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll("\\n\\s+", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String clean(String value) {
        return value == null
                ? ""
                : value.trim();
    }

    private static String defaultIfBlank(
            String value,
            String defaultValue
    ) {
        return StringUtils.hasText(value)
                ? value.trim()
                : defaultValue;
    }

    private static int clamp(
            int value,
            int minimum,
            int maximum
    ) {
        return Math.max(
                minimum,
                Math.min(value, maximum)
        );
    }
}