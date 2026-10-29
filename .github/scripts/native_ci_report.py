"""Write a GitHub job summary from CTest JUnit XML (Python standard library only)."""

import argparse
from html import escape
import os
from pathlib import Path
import re
import xml.etree.ElementTree as ET


def markdown(value):
    return (str(value).replace("\r", " ").replace("\n", " ")
            .replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;"))


def named_results(output):
    """Read the runners' named results; never infer test counts from assertions."""
    checks = re.findall(r"^\[(PASS|FAIL)\] (.+)$", output, re.MULTILINE)
    passed = sum(status == "PASS" for status, _ in checks)
    totals = re.findall(r"^(\d+)/(\d+) (?:tests|test groups|symbol checks) passed\s*$",
                        output, re.MULTILINE)
    if totals:
        declared = tuple(map(int, totals[-1]))
        if not checks or declared != (passed, len(checks)):
            return checks, "Incomplete report", "Reported totals disagree with the named results.", False
        return checks, f"{passed}/{len(checks)}", "", True
    if checks:
        return checks, f"{passed}/{len(checks)} reported", "The runner did not report a final total; the list may be incomplete.", True
    return checks, "Not reported", "No individual test results were reported by this executable.", True


def render_report(report):
    """Return Markdown and a pass/fail flag; missing/empty reports cannot pass."""
    try:
        root = ET.parse(report).getroot()
    except (OSError, ET.ParseError) as error:
        return f"No readable CTest report: {markdown(error)}. See the build/test logs.\n", False

    cases = list(root.iter("testcase"))
    if not cases:
        return "CTest reported no test suites. This is a failure, not a passing run.\n", False

    rows = []
    details = []
    passed = 0
    named_passed = 0
    named_total = 0
    complete_counts = True
    for case in cases:
        name = case.get("name", "unnamed")
        output = "\n".join(
            element.text or "" for element in case if element.tag in ("system-out", "system-err"))
        checks, counts, warning, valid = named_results(output)
        if case.find("failure") is not None or case.find("error") is not None:
            status = "Failed"
        elif case.find("skipped") is not None:
            status = "Skipped"
        elif not valid or any(result == "FAIL" for result, _ in checks):
            status = "Failed"
        else:
            status = "Passed"
            passed += 1
        rows.append(f"| {markdown(name)} | {status} | {counts} | {markdown(case.get('time', '?'))} |")
        complete_counts = complete_counts and bool(checks) and not warning
        named_passed += sum(result == "PASS" for result, _ in checks)
        named_total += len(checks)

        detail = f"<details>\n<summary>{escape(name)}: {status} ({escape(counts)})</summary>\n\n"
        if checks:
            detail += "| Individual test / check | Result |\n| --- | --- |\n"
            detail += "\n".join(f"| {markdown(label)} | {'Passed' if result == 'PASS' else 'Failed'} |"
                                for result, label in checks) + "\n\n"
        if warning:
            detail += warning + "\n\n"
        if output and (status != "Passed" or warning):
            detail += f"<pre>{escape(output)}</pre>\n\n"
        details.append(detail + "</details>\n")

    content = f"**{passed}/{len(cases)} test modules passed.**\n\n"
    if complete_counts:
        content += f"**{named_passed}/{named_total} named tests/checks passed.**\n\n"
    content += "| Test module | Result | Tests/checks passed | Seconds |\n| --- | --- | --- | --- |\n"
    content += "\n".join(rows) + "\n\n"
    content += "Counts refer to named test cases/groups or symbol checks, not individual assertions. "
    content += "Expand a module below to see each result.\n\n"
    content += "\n".join(details)
    return content, passed == len(cases)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--summary", type=Path, required=True)
    args = parser.parse_args()

    content, passed = render_report(args.report)
    platform = markdown(os.environ.get("NATIVE_PLATFORM", "Native tests"))
    stages = [(name, os.environ.get(variable, "unknown")) for name, variable in (
        ("Configure", "CONFIGURE_RESULT"), ("Build", "BUILD_RESULT"), ("Tests", "TEST_RESULT"))]
    with args.summary.open("a", encoding="utf-8") as summary:
        summary.write(f"### {platform}\n\n")
        summary.write("| Stage | Result |\n| --- | --- |\n")
        for name, result in stages:
            summary.write(f"| {name} | {markdown(result)} |\n")
        summary.write("\n" + content)
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
