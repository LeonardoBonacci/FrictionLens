package com.frictionlens.config;

import com.frictionlens.model.FrictionReport;
import com.frictionlens.model.Severity;
import com.frictionlens.repository.FrictionReportRepository;
import com.frictionlens.service.OllamaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2) // run after VectorIndexInitializer
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final FrictionReportRepository reportRepository;
    private final OllamaService ollamaService;

    public DataSeeder(FrictionReportRepository reportRepository, OllamaService ollamaService) {
        this.reportRepository = reportRepository;
        this.ollamaService = ollamaService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (reportRepository.count() > 0) {
            log.info("Database already contains friction reports, skipping seed data");
            return;
        }

        log.info("Seeding database with sample friction reports...");

        List<SeedReport> seeds = List.of(
                // 1 - Tooling / CI
                new SeedReport("Software Engineer", "Platform", "Tooling", Severity.HIGH,
                        "Our CI/CD pipeline takes over 45 minutes for a full build. Developers lose focus waiting for feedback and often context-switch to other tasks, which kills productivity. We need parallel test execution and better caching."),

                // 2 - Process / Approvals
                new SeedReport("Senior Developer", "Payments", "Process", Severity.CRITICAL,
                        "Deploying to production requires sign-off from three separate teams, even for a one-line config change. The approval chain adds 2-3 business days of delay. Last sprint we missed our release window because one approver was on vacation."),

                // 3 - Testing / Mobile
                new SeedReport("QA Engineer", "Mobile", "Testing", Severity.MEDIUM,
                        "We have no automated end-to-end tests for the mobile app. Every release requires two full days of manual regression testing across 6 device configurations. Bugs keep slipping through because testers are fatigued by repetitive manual work."),

                // 4 - Communication / Alignment
                new SeedReport("Product Manager", "Search", "Communication", Severity.HIGH,
                        "Engineering priorities are decided in meetings that product managers are not invited to. We often discover mid-sprint that the team pivoted to different work than what was agreed upon in planning. Alignment between product and engineering is severely broken."),

                // 5 - Tooling / Monitoring
                new SeedReport("DevOps Engineer", "Infrastructure", "Tooling", Severity.CRITICAL,
                        "Our monitoring stack is unreliable. Alerts fire for non-issues at 3 AM, causing on-call fatigue, while real outages go undetected because dashboards have stale metrics. We had a 2-hour production outage last month that nobody noticed until a customer reported it."),

                // 6 - Documentation / APIs
                new SeedReport("Frontend Developer", "Checkout", "Documentation", Severity.MEDIUM,
                        "The internal API documentation is always out of date. I spent an entire day last week trying to integrate with the order service only to find out the endpoints had changed three months ago. There is no single source of truth for API contracts."),

                // 7 - Process / Retros
                new SeedReport("Engineering Manager", "Data", "Process", Severity.HIGH,
                        "Our sprint retrospectives never lead to actionable change. The same friction points come up every two weeks — flaky tests, unclear requirements, too many meetings — but nothing gets prioritized for improvement. Teams are losing faith in the process."),

                // 8 - Dependencies / Auth
                new SeedReport("Backend Developer", "Auth", "Dependencies", Severity.HIGH,
                        "We are blocked by a shared authentication library that is maintained by one person who left the company. Nobody else understands the codebase. Every team that depends on it is stuck on a version with known security vulnerabilities because nobody can safely upgrade it."),

                // 9 - Communication / Design handoff
                new SeedReport("Designer", "Growth", "Communication", Severity.MEDIUM,
                        "Design handoff is a constant source of friction. Engineers say they cannot find the specs in Figma, designers say the implementation does not match the designs. We go back and forth for days on each feature because there is no structured review process."),

                // 10 - Infrastructure / K8s
                new SeedReport("Site Reliability Engineer", "Platform", "Infrastructure", Severity.CRITICAL,
                        "Our Kubernetes cluster runs out of memory at least once a week during peak traffic. Services get OOM-killed and restart, causing cascading failures. We have requested a resource quota increase for months but capacity planning keeps getting deprioritized."),

                // 11 - Tooling / Dev environment
                new SeedReport("Software Engineer", "Payments", "Tooling", Severity.MEDIUM,
                        "Local development environment setup takes a full day for new team members. The README is outdated, Docker containers conflict with each other, and environment variables are scattered across three different config files with no clear documentation."),

                // 12 - Architecture / Monolith
                new SeedReport("Tech Lead", "Search", "Architecture", Severity.HIGH,
                        "Our monolithic search service has become unmaintainable. A single change requires understanding 200k lines of tightly coupled code. Deployments are risky because there are no module boundaries — a bug in ranking logic can break autocomplete suggestions."),

                // 13 - Process / Onboarding
                new SeedReport("Software Engineer", "Growth", "Process", Severity.HIGH,
                        "New engineer onboarding takes over three weeks before someone can make their first meaningful contribution. There is no structured onboarding plan, mentors are assigned but too busy to help, and tribal knowledge is not documented anywhere."),

                // 14 - Tooling / Flaky tests
                new SeedReport("Senior Developer", "Platform", "Testing", Severity.HIGH,
                        "About 15 percent of our test suite is flaky. Tests pass or fail randomly depending on timing and test ordering. Engineers have started ignoring test failures entirely, which means real bugs slip through to production unnoticed."),

                // 15 - Communication / Cross-team
                new SeedReport("Product Manager", "Checkout", "Communication", Severity.MEDIUM,
                        "There is no clear escalation path when teams have conflicting priorities. Last month the Payments team and Checkout team both needed the same backend change but wanted different implementations. It took two weeks of meetings to resolve."),

                // 16 - Infrastructure / Database
                new SeedReport("Database Administrator", "Data", "Infrastructure", Severity.CRITICAL,
                        "Our primary PostgreSQL database is running at 90 percent capacity during peak hours. Query performance has degraded significantly over the past quarter. We need to either vertically scale or implement read replicas, but there is no budget approval process that moves fast enough."),

                // 17 - Dependencies / Vendor lock-in
                new SeedReport("Backend Developer", "Payments", "Dependencies", Severity.HIGH,
                        "We are deeply locked into a payment gateway vendor whose SDK has not been updated in two years. They deprecated three APIs we rely on, and migrating to a new provider would require rewriting the entire checkout flow because of tight coupling."),

                // 18 - Tooling / IDE and local tools
                new SeedReport("Frontend Developer", "Mobile", "Tooling", Severity.LOW,
                        "Our linting and formatting rules are inconsistent across repositories. Some projects use ESLint, others use Biome, and the configurations conflict. Pull requests get blocked by style debates instead of focusing on logic and correctness."),

                // 19 - Process / Meetings
                new SeedReport("Software Engineer", "Search", "Process", Severity.MEDIUM,
                        "Engineers spend an average of 12 hours per week in meetings. Most of these meetings lack agendas, run over time, and could have been async updates. Focus time is fragmented into 30-minute blocks that are too short for deep work."),

                // 20 - Documentation / Internal wikis
                new SeedReport("Tech Lead", "Auth", "Documentation", Severity.MEDIUM,
                        "Our internal wiki has become a graveyard of outdated pages. Teams created documentation during project launches but never maintained it. Engineers now distrust the wiki entirely and resort to asking in Slack, which creates repeated interruptions."),

                // 21 - Architecture / Microservices sprawl
                new SeedReport("Site Reliability Engineer", "Infrastructure", "Architecture", Severity.HIGH,
                        "We have 87 microservices and only 40 engineers. Most services are owned by one person, and when that person is out, nobody can troubleshoot issues. Service-to-service communication is a tangled web of synchronous HTTP calls with no circuit breakers."),

                // 22 - Communication / Remote friction
                new SeedReport("Engineering Manager", "Mobile", "Communication", Severity.MEDIUM,
                        "Our team is split across three time zones with only a two-hour overlap window. Important decisions get made in local hallway conversations that remote team members are not part of. By the time remote engineers catch up, the context is lost."),

                // 23 - Testing / Performance
                new SeedReport("QA Engineer", "Checkout", "Testing", Severity.HIGH,
                        "We have zero performance testing in our pipeline. Last Black Friday, the checkout service fell over at 3x normal traffic. We only discover performance regressions when customers complain in production. We need load testing as part of CI."),

                // 24 - Infrastructure / Secrets management
                new SeedReport("DevOps Engineer", "Auth", "Infrastructure", Severity.CRITICAL,
                        "API keys and database credentials are stored in plain text in environment variables across 12 different services. There is no centralized secrets management. When we need to rotate a credential, it takes a full day to find and update every reference."),

                // 25 - Process / Code review
                new SeedReport("Senior Developer", "Data", "Process", Severity.MEDIUM,
                        "Code reviews take an average of three days to get approved. Reviewers are overloaded and PRs pile up. By the time feedback arrives, the author has moved on to other work and context-switching back is expensive. Small PRs get the same delay as large ones."),

                // 26 - Tooling / Feature flags
                new SeedReport("Product Manager", "Growth", "Tooling", Severity.MEDIUM,
                        "We have no feature flag system. Every feature goes directly to 100 percent of users on deploy. When something breaks, our only option is a full rollback. Product cannot run A/B tests or gradual rollouts, which limits our ability to experiment safely."),

                // 27 - Dependencies / Internal libraries
                new SeedReport("Software Engineer", "Checkout", "Dependencies", Severity.HIGH,
                        "Our shared UI component library has breaking changes in almost every minor release. There is no changelog, no migration guide, and no deprecation warnings. Upgrading takes two or three days because we have to reverse-engineer what changed by reading diffs."),

                // 28 - Documentation / Runbooks
                new SeedReport("Site Reliability Engineer", "Infrastructure", "Documentation", Severity.CRITICAL,
                        "On-call runbooks are either missing or hopelessly outdated. During a recent incident, the runbook referenced a service that was decommissioned six months ago. New on-call engineers are left to figure things out by themselves at 2 AM under pressure."),

                // 29 - Architecture / Data pipeline
                new SeedReport("Data Engineer", "Data", "Architecture", Severity.HIGH,
                        "Our ETL pipeline is a fragile chain of cron jobs, bash scripts, and Python notebooks with no error handling. When one step fails, downstream jobs run on stale data without anyone noticing. We have had dashboards showing incorrect metrics for days before someone caught it."),

                // 30 - Communication / Stakeholder expectations
                new SeedReport("Engineering Manager", "Payments", "Communication", Severity.HIGH,
                        "Stakeholders frequently change requirements after development has started, but timelines are never adjusted. Engineers feel set up to fail because they are held accountable for deadlines based on original scope while scope keeps expanding informally through Slack messages."),

                // 31 - Testing / Security
                new SeedReport("Security Engineer", "Auth", "Testing", Severity.CRITICAL,
                        "We have no automated security scanning in our CI pipeline. Dependency vulnerabilities are only discovered during quarterly manual audits. Last quarter we found 23 critical CVEs in production dependencies that had been there for months."),

                // 32 - Process / Incident management
                new SeedReport("Site Reliability Engineer", "Platform", "Process", Severity.HIGH,
                        "Our incident management process is chaotic. There is no clear incident commander role, communication happens across Slack, email, and phone simultaneously, and post-mortems are written but action items are never tracked or completed."),

                // 33 - Infrastructure / Cost management
                new SeedReport("DevOps Engineer", "Infrastructure", "Infrastructure", Severity.MEDIUM,
                        "Our cloud spend has tripled in the past year but nobody owns cost optimization. Orphaned resources, oversized instances, and forgotten dev environments run 24/7. We estimate 40 percent of our AWS bill is waste, but there is no tooling to identify or clean it up."),

                // 34 - Tooling / Logging
                new SeedReport("Backend Developer", "Search", "Tooling", Severity.MEDIUM,
                        "Our logging is inconsistent across services. Some use structured JSON, others use plain text. There is no correlation ID to trace requests across microservices. Debugging a production issue requires manually stitching logs from five different services."),

                // 35 - Architecture / Frontend state
                new SeedReport("Frontend Developer", "Growth", "Architecture", Severity.MEDIUM,
                        "Our React application has become a state management nightmare. We use Redux, React Query, and local state inconsistently across components. The same data is fetched and cached in multiple places, leading to stale UI and hard-to-reproduce bugs."),

                // 36 - Process / Technical debt
                new SeedReport("Tech Lead", "Platform", "Process", Severity.HIGH,
                        "Technical debt is never prioritized in sprint planning. Product always wins the prioritization battle, so shortcuts accumulate quarter after quarter. Our velocity has dropped 30 percent over the past year because every feature now requires navigating around years of accumulated hacks."),

                // 37 - Communication / Documentation culture
                new SeedReport("Senior Developer", "Auth", "Communication", Severity.LOW,
                        "There is a cultural resistance to writing things down. Senior engineers prefer verbal explanations, which means knowledge stays in people's heads. When those people go on vacation or leave, entire subsystems become black boxes to the rest of the team."),

                // 38 - Dependencies / Database migrations
                new SeedReport("Backend Developer", "Data", "Dependencies", Severity.HIGH,
                        "Database schema migrations are terrifying. We have no rollback strategy, migrations run in production without being tested against a production-like dataset first, and a bad migration last month locked a table for 20 minutes during peak traffic."),

                // 39 - Testing / Contract testing
                new SeedReport("Software Engineer", "Checkout", "Testing", Severity.MEDIUM,
                        "We have no contract tests between our frontend and backend services. The API changes without warning and the frontend breaks in production. We find out from user complaints rather than from our test suite. Integration testing only happens manually before major releases."),

                // 40 - Infrastructure / Networking
                new SeedReport("DevOps Engineer", "Platform", "Infrastructure", Severity.HIGH,
                        "Inter-service latency has been creeping up but we have no visibility into network performance. Services make synchronous calls to downstream dependencies without timeouts, so one slow service causes cascading latency across the entire platform."),

                // 41 - Tooling / Dependency updates
                new SeedReport("Software Engineer", "Mobile", "Tooling", Severity.MEDIUM,
                        "Nobody owns dependency updates. Our mobile app runs on React Native 0.68 while the latest is 0.74. Each version we skip makes the eventual upgrade harder. We have postponed it for over a year and now face a multi-week migration effort with breaking changes in every intermediate version."),

                // 42 - Process / Knowledge silos
                new SeedReport("Engineering Manager", "Search", "Process", Severity.CRITICAL,
                        "Critical business logic for our ranking algorithm is understood by exactly one engineer. There are no design documents, no tests that explain the intent, and the code comments are sparse. If that person were to leave tomorrow, we would lose the ability to meaningfully evolve our core product differentiator.")
        );

        int seeded = 0;
        for (SeedReport seed : seeds) {
            try {
                FrictionReport report = new FrictionReport();
                report.setJobTitle(seed.jobTitle);
                report.setTeam(seed.team);
                report.setCategory(seed.category);
                report.setSeverity(seed.severity);
                report.setBlockerText(seed.blockerText);

                float[] embedding = ollamaService.generateEmbedding(seed.blockerText);
                report.setEmbedding(embedding);

                reportRepository.save(report);
                seeded++;
            } catch (Exception e) {
                log.warn("Failed to seed report '{}...': {}", seed.blockerText.substring(0, 40), e.getMessage());
            }
        }

        log.info("Seeded {} friction reports", seeded);
    }

    private record SeedReport(String jobTitle, String team, String category, Severity severity, String blockerText) {}
}
