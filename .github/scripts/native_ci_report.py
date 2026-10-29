"""Write a GitHub job summary from CTest JUnit XML (Python standard library only)."""

import argparse
import os
from pathlib import Path
import xml.etree.ElementTree as ET


def markdown(value):
    return (str(value).replace("\r", " ").replace("\n", " ")
            .replace("|", "\\|").replace("<", "&lt;").replace(">", "&gt;"))


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
    passed = 0
    for case in cases:
        if case.find("failure") is not None or case.find("error") is not None:
            status = "Failed"
        elif case.find("skipped") is not None:
            status = "Skipped"
        else:
            status = "Passed"
            passed += 1
        rows.append(f"| {markdown(case.get('name', 'unnamed'))} | {status} | {markdown(case.get('time', '?'))} |")

    content = f"**{passed}/{len(cases)} CTest suites passed.**\n\n"
    content += "| CTest suite | Result | Seconds |\n| --- | --- | --- |\n"
    content += "\n".join(rows) + "\n\n"
    content += "Each CTest suite can contain multiple internal test cases. See the logs for their details.\n"
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
