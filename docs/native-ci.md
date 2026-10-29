# Native continuous integration

The `Native CI` GitHub Actions workflow compiles the native libraries, both demo
programs and all registered tests, then runs the full CTest suite. Each relevant
run uses two independent GitHub-hosted runners: Ubuntu 22.04 and Ubuntu 24.04,
both with a Debug build. A failure on one version does not cancel the other.

## When it runs

- When a pull request targeting `develop` or `main` is opened, updated or reopened.
- After a push to `develop` or `main`.
- When requested manually with **Actions > Native CI > Run workflow**. GitHub
  displays this button once the workflow exists on the default branch (`main`).

The workflow always reports a final result. A small change-detection job decides
whether the two builds are necessary. It watches all of `native/`, the workflow,
its report scripts, `.gitattributes` and `.gitmodules`. Frontend/backend-only PRs
therefore receive a successful "builds not needed" result. Native documentation
changes also trigger builds; this intentionally keeps the rule easy to maintain.
The author of a change does not affect which checks run.

PR detection compares the PR head with its merge base, while the build tests
GitHub's proposed merge with the target branch. Push detection compares the
before/after revisions. Manual runs always build. If a comparison is unavailable,
the workflow runs the builds rather than risk skipping relevant changes.

Updating a PR cancels its older unfinished Native CI runs. Push and manual runs
have separate concurrency groups and are not superseded by PR updates.

## Reading a result

Open the PR's checks or the workflow run in **Actions**. Each Ubuntu job has
separate dependency, configure, compile and test steps. Its summary lists the
stage outcomes, test modules, durations and counts such as `Passed | 15/15`.
Expand a module below the table to see the name and result of every individual
test/check directly in GitHub. No artifact download is needed. The **Run all
registered native tests** step also prints successful individual results.

The four CTest registrations are test modules. Their counts come from the
executables' `[PASS]`/`[FAIL]` records and final totals: named tests/groups for
crypto and audit, and individual export checks for the symbol modules. These
counts do not count assertions or iterations within a test case. If a module
crashes before reporting a final total, the summary marks its observed count
as potentially incomplete. Missing or inconsistent counts are never invented.
CTest saves up to 1 MiB of output per module for both passing and failing tests,
so larger suites retain their individual results and final totals in the report.

Reports and logs are attached as `native-ubuntu-22.04-debug` and
`native-ubuntu-24.04-debug`, retained for seven days. They include CTest JUnit XML,
environment details and configure/build/test logs, including failure details.
Missing reports, zero tests, failed tests and skipped suites cannot produce a
passing native build. Compiled binaries and Docker images are not uploaded.

`Native CI result` is the stable aggregate check. It passes only when change
detection succeeds and either both Ubuntu jobs pass or neither is needed. It
does not hide failed or cancelled builds. Each build has a 15-minute limit and
each CTest suite has a 120-second timeout.

## Reproduce locally

On Ubuntu with `build-essential`, `cmake` (3.21 or newer for JUnit output) and
`libssl-dev` installed, run from the repository root:

```bash
cmake -S native -B build/native-ci -DCMAKE_BUILD_TYPE=Debug -DBUILD_TESTING=ON -DRESURS_BUILD_CRYPTO_DEMO=ON -DRESURS_BUILD_AUDIT_DEMO=ON
cmake --build build/native-ci --parallel 2
ctest --test-dir build/native-ci --verbose --output-on-failure --no-tests=error --timeout 120 --test-output-size-passed 1048576 --test-output-size-failed 1048576 --output-junit "$PWD/build/native-ci/ctest.xml"
```

Windows developers can continue using the existing Docker instructions in
`native/DOCKER.md` or run `bash native/test-linux.sh` from a Docker-enabled shell.
CI uses CMake/CTest directly on Ubuntu so that failures and reports are available
as separate steps; it does not validate the Dockerfile or Windows DLLs.

Tests subsequently added to CMake/CTest are picked up automatically. The CI
branch does not depend on the pending native test or documentation branches.

## Merge protection and deployment

After the first successful GitHub run, configure a branch ruleset for `develop`
and `main` to require PRs and the `Native CI result` status check. Select the
check actually recorded by GitHub. Consider requiring branches to be up to date
before merging so the checked merge includes the latest target-branch changes.
If a merge queue is introduced later, add and validate its `merge_group` trigger.

This change does not enable merge restrictions itself. It also does not change
the existing infra guard or course deployment platform. According to `DRIFT.md`,
pushes to `develop` deploy stage and pushes to `main` deploy production. A failing
post-push CI run cannot stop a deployment already triggered by that same push;
requiring checks before merging provides the protection.

This first workflow covers native compilation and tests. Java/JNA integration,
backend/frontend tests, Windows builds, Release builds, sanitizer checks and
Docker packaging can be added separately.

## Usage and maintenance

Standard GitHub-hosted runner execution is free for public repositories. Storage
has separate allowances, which is why this workflow retains only small reports
for seven days. It uses read-only repository permissions, no deployment secrets,
and official actions pinned to reviewed release commits. Update those pins when
adopting a newer action release.

References: [GitHub Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions),
[workflow concurrency](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency),
and [CTest reporting](https://cmake.org/cmake/help/latest/manual/ctest.1.html#cmdoption-ctest-output-junit).
