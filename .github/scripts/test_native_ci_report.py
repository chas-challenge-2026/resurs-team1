"""Ensure incomplete or failed test runs never produce a passing summary."""

from pathlib import Path
import tempfile
import unittest

from native_ci_report import render_report


class ReportTests(unittest.TestCase):
    def report(self, xml):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "ctest.xml"
            path.write_text(xml, encoding="utf-8")
            return render_report(path)

    def test_passing_suites(self):
        content, passed = self.report('<testsuite><testcase name="crypto" time="0.1"/><testcase name="audit" time="0.2"/></testsuite>')
        self.assertTrue(passed)
        self.assertIn("2/2 CTest suites passed", content)

    def test_failure_error_and_skip_do_not_pass(self):
        for marker in ("failure", "error", "skipped"):
            with self.subTest(marker=marker):
                content, passed = self.report(f'<testsuites><testsuite><testcase name="audit"><{marker}/></testcase></testsuite></testsuites>')
                self.assertFalse(passed)
                self.assertIn("0/1 CTest suites passed", content)

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
