"""Ensure incomplete or failed test runs never produce a passing summary."""

from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET

from native_ci_report import render_report


class ReportTests(unittest.TestCase):
    def report(self, xml):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "ctest.xml"
            path.write_text(xml, encoding="utf-8")
            return render_report(path)

    def output_report(self, output, marker=None):
        root = ET.Element("testsuite")
        case = ET.SubElement(root, "testcase", name="crypto", time="0.1")
        ET.SubElement(case, "system-out").text = output
        if marker:
            ET.SubElement(case, marker)
        return self.report(ET.tostring(root, encoding="unicode"))

    def test_passing_suites(self):
        content, passed = self.report('<testsuite><testcase name="crypto" time="0.1"/><testcase name="audit" time="0.2"/></testsuite>')
        self.assertTrue(passed)
        self.assertIn("2/2 test modules passed", content)
        self.assertIn("Not reported", content)

    def test_failure_error_and_skip_do_not_pass(self):
        for marker in ("failure", "error", "skipped"):
            with self.subTest(marker=marker):
                content, passed = self.report(f'<testsuites><testsuite><testcase name="audit"><{marker}/></testcase></testsuite></testsuites>')
                self.assertFalse(passed)
                self.assertIn("0/1 test modules passed", content)

    def test_counts_and_individual_results_for_each_runner_format(self):
        for label in ("tests", "test groups", "symbol checks"):
            with self.subTest(label=label):
                content, passed = self.output_report(
                    f"[PASS] round trip\n[PASS] rejects bad input\n2/2 {label} passed\n")
                self.assertTrue(passed)
                self.assertIn("| crypto | Passed | 2/2 |", content)
                self.assertIn("2/2 named tests/checks passed", content)
                self.assertIn("<details>", content)
                self.assertIn("| round trip | Passed |", content)
                self.assertIn("| rejects bad input | Passed |", content)

    def test_failed_named_test_and_details_are_visible(self):
        content, passed = self.output_report(
            "[PASS] round trip\n[FAIL] invalid key: expected rejection\n1/2 tests passed\n",
            marker="failure")
        self.assertFalse(passed)
        self.assertIn("| crypto | Failed | 1/2 |", content)
        self.assertIn("| invalid key: expected rejection | Failed |", content)

    def test_ctest_failure_is_not_hidden_by_passing_named_tests(self):
        content, passed = self.output_report("[PASS] round trip\n1/1 tests passed\n", marker="error")
        self.assertFalse(passed)
        self.assertIn("| crypto | Failed | 1/1 |", content)

    def test_named_failure_is_not_hidden_by_ctest_success(self):
        _, passed = self.output_report("[FAIL] round trip\n0/1 tests passed\n")
        self.assertFalse(passed)

    def test_inconsistent_counts_do_not_pass(self):
        content, passed = self.output_report("[PASS] round trip\n2/2 tests passed\n")
        self.assertFalse(passed)
        self.assertIn("Incomplete report", content)

    def test_zero_named_tests_do_not_pass(self):
        _, passed = self.output_report("0/0 tests passed\n")
        self.assertFalse(passed)

    def test_partial_output_does_not_claim_a_complete_count(self):
        content, passed = self.output_report("[PASS] round trip\n", marker="error")
        self.assertFalse(passed)
        self.assertIn("1/1 reported", content)
        self.assertIn("list may be incomplete", content)
        self.assertNotIn("named tests/checks passed", content)

    def test_names_and_failure_output_are_escaped(self):
        content, _ = self.output_report("[FAIL] input <script> | invalid\n0/1 tests passed\n", marker="failure")
        self.assertNotIn("<script>", content)
        self.assertIn("&lt;script&gt;", content)
        self.assertIn("\\| invalid", content)

    def test_empty_report_does_not_pass(self):
        _, passed = self.report("<testsuite/>")
        self.assertFalse(passed)

    def test_malformed_report_does_not_pass(self):
        _, passed = self.report("not XML")
        self.assertFalse(passed)

    def test_missing_report_does_not_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            _, passed = render_report(Path(directory) / "missing.xml")
            self.assertFalse(passed)


if __name__ == "__main__":
    unittest.main()
