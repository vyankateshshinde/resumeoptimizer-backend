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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "jobfinder.adzuna",
        name = "enabled",
        havingValue = "true"
)
public class AdzunaJobSourceProvider implements JobSourceProvider {

    private static final Logger log =
            LoggerFactory.getLogger(AdzunaJobSourceProvider.class);

    private final RestTemplate restTemplate;

    private final String baseUrl;
    private final String appId;
    private final String appKey;
    private final String country;
    private final String currency;

    private final int resultsPerPage;
    private final int pages;

    private final List<String> queries;

    public AdzunaJobSourceProvider(
            RestTemplateBuilder restTemplateBuilder,

            @Value("${jobfinder.adzuna.base-url:https://api.adzuna.com/v1/api/jobs}")
            String baseUrl,

            @Value("${jobfinder.adzuna.app-id:}")
            String appId,

            @Value("${jobfinder.adzuna.app-key:}")
            String appKey,

            @Value("${jobfinder.adzuna.country:in}")
            String country,

            @Value("${jobfinder.adzuna.currency:INR}")
            String currency,

            @Value("${jobfinder.adzuna.results-per-page:50}")
            int resultsPerPage,

            @Value("${jobfinder.adzuna.pages:2}")
            int pages,

            @Value("${jobfinder.adzuna.queries:java developer,spring boot developer,java full stack developer,backend developer}")
            String queries
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();

        this.baseUrl = removeTrailingSlash(baseUrl);
        this.appId = clean(appId);
        this.appKey = clean(appKey);
        this.country = defaultIfBlank(country, "in")
                .toLowerCase(Locale.ROOT);
        this.currency = defaultIfBlank(currency, "INR")
                .toUpperCase(Locale.ROOT);

        this.resultsPerPage = clamp(
                resultsPerPage,
                1,
                50
        );

        this.pages = clamp(
                pages,
                1,
                5
        );

        this.queries = parseQueries(queries);
    }

    @Override
    public JobSource getSource() {
        return JobSource.ADZUNA;
    }

    @Override
    public List<ExternalJobRecord> fetchJobs(
            LocalDateTime postedAfter
    ) {
        if (!credentialsAvailable()) {
            log.warn(
                    "Adzuna provider is enabled, but app ID or app key is missing"
            );

            return List.of();
        }

        Map<String, ExternalJobRecord> uniqueJobs =
                new LinkedHashMap<>();

        for (String query : queries) {
            for (int page = 1; page <= pages; page++) {
                List<ExternalJobRecord> pageJobs =
                        fetchPage(
                                query,
                                page,
                                postedAfter
                        );

                for (ExternalJobRecord job : pageJobs) {
                    uniqueJobs.putIfAbsent(
                            job.externalId(),
                            job
                    );
                }
            }
        }

        log.info(
                "Adzuna ingestion fetched {} unique recent jobs",
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
            URI uri = UriComponentsBuilder
                    .fromHttpUrl(baseUrl)
                    .pathSegment(
                            country,
                            "search",
                            String.valueOf(page)
                    )
                    .queryParam("app_id", appId)
                    .queryParam("app_key", appKey)
                    .queryParam(
                            "results_per_page",
                            resultsPerPage
                    )
                    .queryParam("what", query)
                    .queryParam("sort_by", "date")
                    .queryParam(
                            "content-type",
                            "application/json"
                    )
                    .build()
                    .encode()
                    .toUri();

            JsonNode response = restTemplate.getForObject(
                    uri,
                    JsonNode.class
            );

            if (response == null
                    || !response.path("results").isArray()) {
                log.warn(
                        "Adzuna returned no results array for query: {}",
                        query
                );

                return List.of();
            }

            return mapJobs(
                    response.path("results"),
                    postedAfter
            );

        } catch (RestClientResponseException exception) {
            log.warn(
                    "Adzuna request failed for query '{}' page {}. Status: {}",
                    query,
                    page,
                    exception.getStatusCode()
            );

            return List.of();

        } catch (ResourceAccessException exception) {
            log.warn(
                    "Adzuna connection failed for query '{}' page {}: {}",
                    query,
                    page,
                    exception.getMessage()
            );

            return List.of();

        } catch (Exception exception) {
            log.error(
                    "Unexpected Adzuna error for query '{}' page {}",
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
        java.util.ArrayList<ExternalJobRecord> jobs =
                new java.util.ArrayList<>();

        for (JsonNode result : results) {
            ExternalJobRecord job = mapJob(
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
                "id"
        );

        String title = cleanHtml(
                readText(result, "title")
        );

        String company = cleanHtml(
                result.path("company")
                        .path("display_name")
                        .asText("")
        );

        String location = cleanHtml(
                result.path("location")
                        .path("display_name")
                        .asText("")
        );

        String description = cleanHtml(
                readText(result, "description")
        );

        String applyUrl = readText(
                result,
                "redirect_url"
        );

        LocalDateTime postedAt = parseDateTime(
                readText(result, "created")
        );

        if (postedAfter != null
                && postedAt != null
                && postedAt.isBefore(postedAfter)) {
            return null;
        }

        if (!StringUtils.hasText(externalId)
                || !StringUtils.hasText(title)
                || !StringUtils.hasText(company)
                || !StringUtils.hasText(description)
                || !StringUtils.hasText(applyUrl)) {
            return null;
        }

        String contractType = readText(
                result,
                "contract_type"
        );

        String contractTime = readText(
                result,
                "contract_time"
        );

        WorkArrangement workArrangement =
                detectWorkArrangement(
                        title,
                        location,
                        description
                );

        EmploymentType employmentType =
                detectEmploymentType(
                        title,
                        description,
                        contractType,
                        contractTime
                );

        BigDecimal minimumSalary =
                readDecimal(
                        result,
                        "salary_min"
                );

        BigDecimal maximumSalary =
                readDecimal(
                        result,
                        "salary_max"
                );

        return new ExternalJobRecord(
                externalId,
                title,
                company,
                location,
                workArrangement,
                employmentType,

                // Adzuna does not provide structured
                // minimum/maximum experience fields.
                null,
                null,

                minimumSalary,
                maximumSalary,
                currency,
                description,
                postedAt,
                JobSource.ADZUNA,
                "Adzuna",
                applyUrl
        );
    }

    private WorkArrangement detectWorkArrangement(
            String title,
            String location,
            String description
    ) {
        String searchableText = (
                defaultIfBlank(title, "")
                        + " "
                        + defaultIfBlank(location, "")
                        + " "
                        + defaultIfBlank(description, "")
        ).toLowerCase(Locale.ROOT);

        if (searchableText.contains("hybrid")) {
            return WorkArrangement.HYBRID;
        }

        if (searchableText.contains("remote")
                || searchableText.contains("work from home")
                || searchableText.contains("work-from-home")
                || searchableText.contains("wfh")) {
            return WorkArrangement.REMOTE;
        }

        return WorkArrangement.UNSPECIFIED;
    }

    private EmploymentType detectEmploymentType(
            String title,
            String description,
            String contractType,
            String contractTime
    ) {
        String searchableText = (
                defaultIfBlank(title, "")
                        + " "
                        + defaultIfBlank(description, "")
        ).toLowerCase(Locale.ROOT);

        String normalizedContractType =
                defaultIfBlank(contractType, "")
                        .toLowerCase(Locale.ROOT);

        String normalizedContractTime =
                defaultIfBlank(contractTime, "")
                        .toLowerCase(Locale.ROOT);

        if (searchableText.contains("internship")
                || searchableText.contains("intern ")) {
            return EmploymentType.INTERNSHIP;
        }

        if (normalizedContractType.contains("contract")) {
            return EmploymentType.CONTRACT;
        }

        if (normalizedContractType.contains("temporary")
                || searchableText.contains("temporary")) {
            return EmploymentType.TEMPORARY;
        }

        if (normalizedContractTime.contains("part_time")
                || normalizedContractTime.contains("part time")) {
            return EmploymentType.PART_TIME;
        }

        if (normalizedContractTime.contains("full_time")
                || normalizedContractTime.contains("full time")) {
            return EmploymentType.FULL_TIME;
        }

        return EmploymentType.OTHER;
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return OffsetDateTime
                    .parse(value)
                    .atZoneSameInstant(
                            ZoneId.systemDefault()
                    )
                    .toLocalDateTime();

        } catch (DateTimeParseException exception) {
            log.debug(
                    "Unable to parse Adzuna date: {}",
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
                && StringUtils.hasText(value.asText())) {
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
                node.path(fieldName).asText("")
        );
    }

    private boolean credentialsAvailable() {
        return StringUtils.hasText(appId)
                && StringUtils.hasText(appKey);
    }

    private static List<String> parseQueries(
            String queries
    ) {
        List<String> parsedQueries = Arrays
                .stream(defaultIfBlank(
                        queries,
                        "java developer"
                ).split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        if (parsedQueries.isEmpty()) {
            return List.of("java developer");
        }

        return parsedQueries;
    }

    private static String cleanHtml(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String removeTrailingSlash(
            String value
    ) {
        String cleaned = defaultIfBlank(
                value,
                "https://api.adzuna.com/v1/api/jobs"
        );

        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(
                    0,
                    cleaned.length() - 1
            );
        }

        return cleaned;
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