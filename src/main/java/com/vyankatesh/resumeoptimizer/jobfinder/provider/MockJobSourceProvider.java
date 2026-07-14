package com.vyankatesh.resumeoptimizer.jobfinder.provider;

import com.vyankatesh.resumeoptimizer.jobfinder.model.EmploymentType;
import com.vyankatesh.resumeoptimizer.jobfinder.model.JobSource;
import com.vyankatesh.resumeoptimizer.jobfinder.model.WorkArrangement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class MockJobSourceProvider implements JobSourceProvider {

    @Override
    public JobSource getSource() {
        return JobSource.MOCK;
    }

    @Override
    public List<ExternalJobRecord> fetchJobs(LocalDateTime postedAfter) {
        LocalDateTime now = LocalDateTime.now();

        List<ExternalJobRecord> jobs = List.of(
                new ExternalJobRecord(
                        "mock-java-backend-001",
                        "Java Backend Developer",
                        "Acme Technology",
                        "Pune",
                        WorkArrangement.HYBRID,
                        EmploymentType.FULL_TIME,
                        new BigDecimal("2.0"),
                        new BigDecimal("4.0"),
                        new BigDecimal("800000"),
                        new BigDecimal("1400000"),
                        "INR",
                        "Build scalable backend services using Java, Spring Boot, REST APIs, JPA, MySQL, Docker, Git and AWS. Experience with microservices, Kafka and unit testing is preferred.",
                        now.minusHours(5),
                        JobSource.MOCK,
                        "Demo Company Careers",
                        "https://example.com/jobs/mock-java-backend-001"
                ),
                new ExternalJobRecord(
                        "mock-spring-boot-002",
                        "Spring Boot Developer",
                        "Blue Orbit Systems",
                        "Bengaluru",
                        WorkArrangement.REMOTE,
                        EmploymentType.FULL_TIME,
                        new BigDecimal("1.0"),
                        new BigDecimal("3.0"),
                        new BigDecimal("700000"),
                        new BigDecimal("1200000"),
                        "INR",
                        "Develop Spring Boot microservices and REST APIs. Work with Java, Hibernate, JPA, MySQL, Redis, Docker, Kubernetes, Maven and CI/CD pipelines.",
                        now.minusHours(12),
                        JobSource.MOCK,
                        "Demo Company Careers",
                        "https://example.com/jobs/mock-spring-boot-002"
                ),
                new ExternalJobRecord(
                        "mock-full-stack-003",
                        "Full Stack Java Developer",
                        "NextWave Labs",
                        "Hyderabad",
                        WorkArrangement.HYBRID,
                        EmploymentType.FULL_TIME,
                        new BigDecimal("3.0"),
                        new BigDecimal("6.0"),
                        new BigDecimal("1100000"),
                        new BigDecimal("1900000"),
                        "INR",
                        "Create full stack applications with Java, Spring Boot, React, JavaScript, HTML, CSS, REST APIs, MySQL, Git and cloud deployment on AWS.",
                        now.minusDays(1),
                        JobSource.MOCK,
                        "Demo Company Careers",
                        "https://example.com/jobs/mock-full-stack-003"
                ),
                new ExternalJobRecord(
                        "mock-react-004",
                        "React Frontend Developer",
                        "Pixel Stack",
                        "Mumbai",
                        WorkArrangement.ON_SITE,
                        EmploymentType.FULL_TIME,
                        new BigDecimal("1.0"),
                        new BigDecimal("4.0"),
                        new BigDecimal("600000"),
                        new BigDecimal("1100000"),
                        "INR",
                        "Build responsive user interfaces using React, JavaScript, TypeScript, HTML, CSS, REST APIs, Git, Jest and modern frontend tooling.",
                        now.minusDays(2),
                        JobSource.MOCK,
                        "Demo Company Careers",
                        "https://example.com/jobs/mock-react-004"
                ),
                new ExternalJobRecord(
                        "mock-devops-005",
                        "Cloud DevOps Engineer",
                        "InfraWorks",
                        "Remote",
                        WorkArrangement.REMOTE,
                        EmploymentType.CONTRACT,
                        new BigDecimal("3.0"),
                        new BigDecimal("7.0"),
                        new BigDecimal("1200000"),
                        new BigDecimal("2200000"),
                        "INR",
                        "Manage AWS infrastructure using Docker, Kubernetes, Jenkins, Terraform, Linux, GitHub Actions, monitoring and CI/CD automation.",
                        now.minusDays(3),
                        JobSource.MOCK,
                        "Demo Company Careers",
                        "https://example.com/jobs/mock-devops-005"
                ),
                new ExternalJobRecord(
                        "mock-intern-006",
                        "Java Developer Intern",
                        "StartRight Software",
                        "Pune",
                        WorkArrangement.ON_SITE,
                        EmploymentType.INTERNSHIP,
                        BigDecimal.ZERO,
                        new BigDecimal("1.0"),
                        new BigDecimal("180000"),
                        new BigDecimal("300000"),
                        "INR",
                        "Assist with Java, Spring Boot, REST API, MySQL, Git, Maven and JUnit development. Strong problem solving and object-oriented programming fundamentals are required.",
                        now.minusHours(20),
                        JobSource.MOCK,
                        "Demo Company Careers",
                        "https://example.com/jobs/mock-intern-006"
                )
        );

        return jobs.stream()
                .filter(job -> job.postedAt() != null && !job.postedAt().isBefore(postedAfter))
                .toList();
    }
}
