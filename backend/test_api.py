"""
Black-box integration tests for FrictionLens backend.

Assumes:
  - docker-compose is up (postgres + ollama)
  - backend is running on localhost:8080
  - Ollama has llama3.1:latest pulled

Usage:
  pip install requests pytest
  pytest test_api.py -v
"""

import time
import uuid

import pytest
import requests

BASE_URL = "http://localhost:8080"
REPORTS_URL = f"{BASE_URL}/api/reports"
QUERY_URL = f"{BASE_URL}/api/query"
TRENDS_URL = f"{BASE_URL}/api/trends"
HEALTH_URL = f"{BASE_URL}/api/health"

# ---------------------------------------------------------------------------
# Fixtures & helpers
# ---------------------------------------------------------------------------

SAMPLE_REPORTS = [
    {
        "jobTitle": "Software Engineer",
        "team": "Platform",
        "category": "Tooling",
        "severity": "HIGH",
        "blockerText": "The CI/CD pipeline takes over 45 minutes to complete a simple build. "
                       "Developers are losing almost an hour of productivity per push.",
    },
    {
        "jobTitle": "Software Engineer",
        "team": "Platform",
        "category": "Tooling",
        "severity": "CRITICAL",
        "blockerText": "Our local development environment setup requires 23 manual steps and "
                       "frequently breaks after OS updates. New engineers spend their entire "
                       "first week just getting the environment running.",
    },
    {
        "jobTitle": "Designer",
        "team": "UX",
        "category": "Process",
        "severity": "MEDIUM",
        "blockerText": "Design reviews are scheduled only once every two weeks. By the time "
                       "feedback comes back, the implementation has already moved on, causing "
                       "costly rework.",
    },
    {
        "jobTitle": "Product Manager",
        "team": "Growth",
        "category": "Communication",
        "severity": "HIGH",
        "blockerText": "There is no single source of truth for feature prioritization. "
                       "Different stakeholders reference different spreadsheets and Jira boards, "
                       "leading to conflicting priorities.",
    },
    {
        "jobTitle": "QA Engineer",
        "team": "Platform",
        "category": "Tooling",
        "severity": "MEDIUM",
        "blockerText": "The test automation framework has flaky Selenium tests that fail "
                       "roughly 20% of the time without any code changes. This erodes trust "
                       "in the test suite.",
    },
    {
        "jobTitle": "Backend Developer",
        "team": "Payments",
        "category": "Process",
        "severity": "CRITICAL",
        "blockerText": "Deployments to production require approval from three different teams. "
                       "The approval chain often takes 2-3 business days, even for hotfixes.",
    },
    {
        "jobTitle": "Frontend Developer",
        "team": "UX",
        "category": "Tooling",
        "severity": "LOW",
        "blockerText": "The shared component library is poorly documented. Developers spend "
                       "extra time reading source code to understand prop interfaces.",
    },
    {
        "jobTitle": "Engineering Manager",
        "team": "Growth",
        "category": "Communication",
        "severity": "MEDIUM",
        "blockerText": "Standup meetings regularly run over 30 minutes because team members "
                       "go into detailed technical discussions instead of giving status updates.",
    },
    {
        "jobTitle": "DevOps Engineer",
        "team": "Infrastructure",
        "category": "Tooling",
        "severity": "HIGH",
        "blockerText": "Our monitoring and alerting system generates too many false positives. "
                       "On-call engineers are experiencing alert fatigue and sometimes miss "
                       "real incidents.",
    },
    {
        "jobTitle": "Data Scientist",
        "team": "Analytics",
        "category": "Process",
        "severity": "LOW",
        "blockerText": "Access to production data for analysis requires a formal request that "
                       "takes about a week to get approved, slowing down any data exploration.",
    },
]


@pytest.fixture(scope="session")
def submitted_report_ids():
    """Submit all sample reports and return their IDs and responses."""
    results = []
    for report in SAMPLE_REPORTS:
        resp = requests.post(REPORTS_URL, json=report, timeout=120)
        assert resp.status_code == 201, (
            f"Failed to submit report: {resp.status_code} {resp.text}"
        )
        data = resp.json()
        results.append(data)
    return results


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------


class TestHealth:
    def test_health_endpoint_returns_ok(self):
        resp = requests.get(HEALTH_URL, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert body["status"] == "ok"
        assert body["ollama"] in ("available", "unavailable")

    def test_health_ollama_available(self):
        resp = requests.get(HEALTH_URL, timeout=10)
        body = resp.json()
        assert body["ollama"] == "available", "Ollama should be reachable"


# ---------------------------------------------------------------------------
# Report submission
# ---------------------------------------------------------------------------


class TestSubmitReport:
    def test_submit_valid_report(self):
        payload = {
            "jobTitle": "Test Engineer",
            "team": "QA",
            "category": "Tooling",
            "severity": "LOW",
            "blockerText": "Integration test friction report.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=120)
        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None
        assert body["jobTitle"] == "Test Engineer"
        assert body["team"] == "QA"
        assert body["category"] == "Tooling"
        assert body["severity"] == "LOW"
        assert body["blockerText"]  # sanitized, may differ from original
        assert body["createdAt"] is not None

    def test_submit_all_severities(self):
        for severity in ("LOW", "MEDIUM", "HIGH", "CRITICAL"):
            payload = {
                "jobTitle": "Tester",
                "team": "QA",
                "category": "Testing",
                "severity": severity,
                "blockerText": f"Friction at severity {severity}.",
            }
            resp = requests.post(REPORTS_URL, json=payload, timeout=120)
            assert resp.status_code == 201
            assert resp.json()["severity"] == severity

    def test_submit_report_missing_job_title(self):
        payload = {
            "team": "QA",
            "category": "Tooling",
            "severity": "LOW",
            "blockerText": "Missing job title.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        body = resp.json()
        assert "fieldErrors" in body
        assert "jobTitle" in body["fieldErrors"]

    def test_submit_report_missing_team(self):
        payload = {
            "jobTitle": "Tester",
            "category": "Tooling",
            "severity": "LOW",
            "blockerText": "Missing team.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        assert "team" in resp.json()["fieldErrors"]

    def test_submit_report_missing_category(self):
        payload = {
            "jobTitle": "Tester",
            "team": "QA",
            "severity": "LOW",
            "blockerText": "Missing category.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        assert "category" in resp.json()["fieldErrors"]

    def test_submit_report_missing_severity(self):
        payload = {
            "jobTitle": "Tester",
            "team": "QA",
            "category": "Tooling",
            "blockerText": "Missing severity.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        assert "severity" in resp.json()["fieldErrors"]

    def test_submit_report_missing_blocker_text(self):
        payload = {
            "jobTitle": "Tester",
            "team": "QA",
            "category": "Tooling",
            "severity": "LOW",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        assert "blockerText" in resp.json()["fieldErrors"]

    def test_submit_report_blank_fields(self):
        payload = {
            "jobTitle": "",
            "team": "",
            "category": "",
            "severity": "LOW",
            "blockerText": "",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        errors = resp.json()["fieldErrors"]
        assert "jobTitle" in errors
        assert "team" in errors
        assert "category" in errors
        assert "blockerText" in errors

    def test_submit_report_invalid_severity(self):
        payload = {
            "jobTitle": "Tester",
            "team": "QA",
            "category": "Tooling",
            "severity": "EXTREME",
            "blockerText": "Invalid severity.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400

    def test_submit_report_empty_body(self):
        resp = requests.post(REPORTS_URL, json={}, timeout=10)
        assert resp.status_code == 400

    def test_submit_report_long_blocker_text(self):
        payload = {
            "jobTitle": "Tester",
            "team": "QA",
            "category": "Tooling",
            "severity": "LOW",
            "blockerText": "x" * 10000,
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=120)
        assert resp.status_code == 201

    def test_submit_report_blocker_text_exceeds_limit(self):
        payload = {
            "jobTitle": "Tester",
            "team": "QA",
            "category": "Tooling",
            "severity": "LOW",
            "blockerText": "x" * 10001,
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        assert "blockerText" in resp.json()["fieldErrors"]

    def test_submit_report_returns_uuid_id(self):
        payload = {
            "jobTitle": "Tester",
            "team": "QA",
            "category": "Tooling",
            "severity": "LOW",
            "blockerText": "Check id format.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=120)
        assert resp.status_code == 201
        report_id = resp.json()["id"]
        uuid.UUID(report_id)  # raises ValueError if not valid UUID


# ---------------------------------------------------------------------------
# List reports with filtering & pagination
# ---------------------------------------------------------------------------


class TestListReports:
    def test_list_all_reports(self, submitted_report_ids):
        resp = requests.get(REPORTS_URL, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert "content" in body
        assert body["totalElements"] >= len(SAMPLE_REPORTS)

    def test_list_reports_pagination(self, submitted_report_ids):
        resp = requests.get(REPORTS_URL, params={"page": 0, "size": 3}, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert len(body["content"]) <= 3
        assert body["size"] == 3
        assert body["number"] == 0
        assert body["totalPages"] >= 1

    def test_list_reports_second_page(self, submitted_report_ids):
        resp = requests.get(REPORTS_URL, params={"page": 1, "size": 3}, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert body["number"] == 1

    def test_filter_by_job_title(self, submitted_report_ids):
        resp = requests.get(
            REPORTS_URL, params={"jobTitle": "Software Engineer"}, timeout=10
        )
        assert resp.status_code == 200
        for report in resp.json()["content"]:
            assert report["jobTitle"] == "Software Engineer"

    def test_filter_by_team(self, submitted_report_ids):
        resp = requests.get(
            REPORTS_URL, params={"team": "Platform"}, timeout=10
        )
        assert resp.status_code == 200
        for report in resp.json()["content"]:
            assert report["team"] == "Platform"

    def test_filter_by_category(self, submitted_report_ids):
        resp = requests.get(
            REPORTS_URL, params={"category": "Tooling"}, timeout=10
        )
        assert resp.status_code == 200
        for report in resp.json()["content"]:
            assert report["category"] == "Tooling"

    def test_filter_by_severity(self, submitted_report_ids):
        resp = requests.get(
            REPORTS_URL, params={"severity": "CRITICAL"}, timeout=10
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["totalElements"] >= 2  # we submitted 2 CRITICAL
        for report in body["content"]:
            assert report["severity"] == "CRITICAL"

    def test_filter_combined(self, submitted_report_ids):
        resp = requests.get(
            REPORTS_URL,
            params={"team": "Platform", "category": "Tooling"},
            timeout=10,
        )
        assert resp.status_code == 200
        for report in resp.json()["content"]:
            assert report["team"] == "Platform"
            assert report["category"] == "Tooling"

    def test_filter_no_results(self, submitted_report_ids):
        resp = requests.get(
            REPORTS_URL, params={"team": "NonexistentTeam12345"}, timeout=10
        )
        assert resp.status_code == 200
        assert resp.json()["totalElements"] == 0
        assert resp.json()["content"] == []

    def test_list_reports_sorted_by_created_at_desc(self, submitted_report_ids):
        resp = requests.get(
            REPORTS_URL, params={"sort": "createdAt,desc"}, timeout=10
        )
        assert resp.status_code == 200
        content = resp.json()["content"]
        if len(content) >= 2:
            assert content[0]["createdAt"] >= content[1]["createdAt"]

    def test_list_reports_response_structure(self, submitted_report_ids):
        resp = requests.get(REPORTS_URL, params={"size": 1}, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        # Spring Page structure
        for key in ("content", "totalElements", "totalPages", "size", "number"):
            assert key in body, f"Missing key '{key}' in page response"
        # Report structure
        report = body["content"][0]
        for key in ("id", "jobTitle", "team", "category", "severity", "blockerText", "createdAt"):
            assert key in report, f"Missing key '{key}' in report"


# ---------------------------------------------------------------------------
# Query (semantic search + LLM summary)
# ---------------------------------------------------------------------------


class TestQuery:
    def test_query_basic(self, submitted_report_ids):
        payload = {"question": "What are the main tooling problems?"}
        resp = requests.post(QUERY_URL, json=payload, timeout=120)
        assert resp.status_code == 200
        body = resp.json()
        assert "summary" in body
        assert "supportingReports" in body
        assert "totalMatches" in body
        assert isinstance(body["supportingReports"], list)
        assert isinstance(body["totalMatches"], int)

    def test_query_with_filter(self, submitted_report_ids):
        payload = {
            "question": "What are the main problems?",
            "team": "Platform",
        }
        resp = requests.post(QUERY_URL, json=payload, timeout=120)
        assert resp.status_code == 200
        body = resp.json()
        assert body["totalMatches"] >= 1

    def test_query_with_severity_filter(self, submitted_report_ids):
        payload = {
            "question": "What critical issues exist?",
            "severity": "CRITICAL",
        }
        resp = requests.post(QUERY_URL, json=payload, timeout=120)
        assert resp.status_code == 200
        body = resp.json()
        assert body["totalMatches"] >= 1

    def test_query_with_all_filters(self, submitted_report_ids):
        payload = {
            "question": "Tell me about tooling issues",
            "jobTitle": "Software Engineer",
            "team": "Platform",
            "category": "Tooling",
            "severity": "HIGH",
        }
        resp = requests.post(QUERY_URL, json=payload, timeout=120)
        assert resp.status_code == 200

    def test_query_returns_report_snippets_structure(self, submitted_report_ids):
        payload = {"question": "deployment problems"}
        resp = requests.post(QUERY_URL, json=payload, timeout=120)
        assert resp.status_code == 200
        body = resp.json()
        if body["supportingReports"]:
            snippet = body["supportingReports"][0]
            for key in ("id", "jobTitle", "team", "category", "severity", "blockerText", "createdAt"):
                assert key in snippet, f"Missing key '{key}' in report snippet"

    def test_query_missing_question(self):
        payload = {}
        resp = requests.post(QUERY_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        assert "fieldErrors" in resp.json()
        assert "question" in resp.json()["fieldErrors"]

    def test_query_blank_question(self):
        payload = {"question": ""}
        resp = requests.post(QUERY_URL, json=payload, timeout=10)
        assert resp.status_code == 400

    def test_query_question_too_long(self):
        payload = {"question": "x" * 2001}
        resp = requests.post(QUERY_URL, json=payload, timeout=10)
        assert resp.status_code == 400
        assert "question" in resp.json()["fieldErrors"]

    def test_query_no_matching_reports(self, submitted_report_ids):
        payload = {
            "question": "quantum entanglement issues in the warp drive module",
        }
        resp = requests.post(QUERY_URL, json=payload, timeout=120)
        assert resp.status_code == 200
        body = resp.json()
        assert "summary" in body


# ---------------------------------------------------------------------------
# Trends
# ---------------------------------------------------------------------------


class TestTrends:
    def test_trends_by_category(self, submitted_report_ids):
        resp = requests.get(TRENDS_URL, params={"groupBy": "category"}, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert body["groupBy"] == "category"
        assert isinstance(body["data"], list)
        assert len(body["data"]) >= 1
        labels = {p["label"] for p in body["data"]}
        assert "Tooling" in labels
        for point in body["data"]:
            assert "label" in point
            assert "count" in point
            assert point["count"] >= 1

    def test_trends_by_team(self, submitted_report_ids):
        resp = requests.get(TRENDS_URL, params={"groupBy": "team"}, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert body["groupBy"] == "team"
        labels = {p["label"] for p in body["data"]}
        assert "Platform" in labels

    def test_trends_by_severity(self, submitted_report_ids):
        resp = requests.get(TRENDS_URL, params={"groupBy": "severity"}, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert body["groupBy"] == "severity"
        labels = {p["label"] for p in body["data"]}
        for sev in ("LOW", "MEDIUM", "HIGH", "CRITICAL"):
            assert sev in labels

    def test_trends_by_job_title(self, submitted_report_ids):
        resp = requests.get(TRENDS_URL, params={"groupBy": "jobTitle"}, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert body["groupBy"] == "jobTitle"
        assert len(body["data"]) >= 1

    def test_trends_default_group_by(self, submitted_report_ids):
        resp = requests.get(TRENDS_URL, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert body["groupBy"] == "category"

    def test_trends_counts_are_consistent(self, submitted_report_ids):
        """Total across trend points should match total reports."""
        resp_trends = requests.get(TRENDS_URL, params={"groupBy": "category"}, timeout=10)
        resp_reports = requests.get(REPORTS_URL, params={"size": 1}, timeout=10)
        total_from_trends = sum(p["count"] for p in resp_trends.json()["data"])
        total_from_reports = resp_reports.json()["totalElements"]
        assert total_from_trends == total_from_reports


# ---------------------------------------------------------------------------
# Edge cases & misc
# ---------------------------------------------------------------------------


class TestEdgeCases:
    def test_unknown_endpoint_returns_404(self):
        resp = requests.get(f"{BASE_URL}/api/nonexistent", timeout=10)
        assert resp.status_code == 404

    def test_post_reports_wrong_content_type(self):
        resp = requests.post(
            REPORTS_URL,
            data="not json",
            headers={"Content-Type": "text/plain"},
            timeout=10,
        )
        assert resp.status_code == 415  # Unsupported Media Type

    def test_submit_report_with_unicode(self):
        payload = {
            "jobTitle": "Développeur",
            "team": "Équipe 日本語",
            "category": "Outil — très important",
            "severity": "MEDIUM",
            "blockerText": "Les outils sont cassés 🔧. これはテストです。",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=120)
        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None

    def test_submit_report_with_special_characters(self):
        payload = {
            "jobTitle": "Engineer <script>alert('xss')</script>",
            "team": "Team'; DROP TABLE friction_reports;--",
            "category": "Category & \"quotes\"",
            "severity": "LOW",
            "blockerText": "Blocker with <b>HTML</b> and 'SQL injection' attempts.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=120)
        assert resp.status_code == 201
        body = resp.json()
        assert body["id"] is not None

    def test_submit_and_retrieve_report(self):
        """Submit a report, then verify it shows up in the list with filters."""
        unique_team = f"IntegrationTestTeam-{uuid.uuid4().hex[:8]}"
        payload = {
            "jobTitle": "Tester",
            "team": unique_team,
            "category": "Testing",
            "severity": "LOW",
            "blockerText": "This is a round-trip integration test.",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=120)
        assert resp.status_code == 201
        created_id = resp.json()["id"]

        # List with the unique team filter
        resp = requests.get(REPORTS_URL, params={"team": unique_team}, timeout=10)
        assert resp.status_code == 200
        body = resp.json()
        assert body["totalElements"] == 1
        assert body["content"][0]["id"] == created_id

    def test_multiple_validation_errors_at_once(self):
        payload = {
            "severity": "INVALID",
        }
        resp = requests.post(REPORTS_URL, json=payload, timeout=10)
        assert resp.status_code == 400

    def test_get_reports_with_invalid_severity_filter(self, submitted_report_ids):
        resp = requests.get(
            REPORTS_URL, params={"severity": "EXTREME"}, timeout=10
        )
        # Spring should return 400 for invalid enum
        assert resp.status_code == 400

    def test_concurrent_submissions(self):
        """Submit several reports concurrently and verify all succeed."""
        import concurrent.futures

        def submit(i):
            payload = {
                "jobTitle": "ConcurrentTester",
                "team": "Stress",
                "category": "Concurrency",
                "severity": "LOW",
                "blockerText": f"Concurrent report number {i}.",
            }
            return requests.post(REPORTS_URL, json=payload, timeout=120)

        with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
            futures = [pool.submit(submit, i) for i in range(5)]
            results = [f.result() for f in futures]

        for resp in results:
            assert resp.status_code == 201
