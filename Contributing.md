# Contributing to aqa-tests

Thanks for your interest in this project.

## Project description

aqa-tests is the central project for AQAvit and contains the definition files for the AQA test suites and scripts for running automated tests in CI systems.

* https://github.com/adoptium/aqa-tests

## Developer resources

The AQAvit project maintains the following source code repositories:

* https://github.com/adoptium/aqa-tests - the central project for AQAvit
* https://github.com/adoptium/TKG - a lightweight test harness for running a diverse set of tests or commands
* https://github.com/adoptium/aqa-systemtest - system verification tests
* https://github.com/adoptium/aqa-test-tools - various test tools that improve workflow
* https://github.com/adoptium/STF - system Test Framework
* https://github.com/adoptium/bumblebench - microbenchmarking test framework
* https://github.com/adoptium/run-aqa - run-aqa GitHub action

## Eclipse Contributor Agreement

Before your contribution can be accepted by the project team contributors must
electronically sign the Eclipse Contributor Agreement (ECA).

* http://www.eclipse.org/legal/ECA.php

Commits that are provided by non-committers must have a Signed-off-by field in
the footer indicating that the author is aware of the terms by which the
contribution has been provided to the project. The non-committer must
additionally have an Eclipse Foundation account and must have a signed Eclipse
Contributor Agreement (ECA) on file.

For more information, please see the Eclipse Committer Handbook and Adoptium documentation:
https://www.eclipse.org/projects/handbook/#resources-commit

https://adoptium.net/docs/eca-sign-off

## Submitting a contribution to AQAvit

After signing ECA, you can propose contributions by sending pull requests (PRs) through GitHub.

1. If this is your first time contributing to the project, fork the repo by clicking on the `Fork` button in the top-right corner of the git repo page.
This creates a copy of the repo under your GitHub account: `https://github.com/<YourGitUserName>/aqa-tests.git`

2. Git clone the your aqa-tests repo:
```
git clone https://github.com/<YourGitUserName>/aqa-tests.git
```

3. Create a new branch to work on:
```
cd aqa-tests
git checkout -b my_new_branch
```

4. Add https://github.com/adoptium/aqa-tests.git as your upstream:
```
git remote add upstream https://github.com/adoptium/aqa-tests.git
```

5. Before you start working on the issue, plese make sure the local branch is up to date:
```
git fetch upstream
git rebase upstream/master
```
or 
```
git reset --hard upstream/master
```

6. Once you are done with your work, track your changes and commit. 
```
git add .
git commit -s -m "message about this PR"
```

Commit message example:

```
Update jtreg in openjdk test

This patch updates jtreg version from 5.1 to 6.1.

Fixes: #1234

Signed-off-by: Full Name <email>
```

- The first line is the PR title. It should describe the change made. Please keep it short and simple.
- The body should include detailed information about your PR. You may want to include designs, rationale, and a brief explanation of what you have changed. Please keep it concise.
- When a commit has related issues, please use the Git keywords to automatically close or relate to the issues.
https://help.github.com/articles/closing-issues-using-keywords/
- Please sign off on your commit in the footer. This can be automatically added to your commit by passing `-s` to `git commit` (see above example).

7. Push the change into your Git repo:
```
git push -f origin my_new_branch
```

You can skip `-f` in `git push` if you've never pushed your branch before.

8. We would like to encourage you to open a pull request early and use `Create draft pull request` option. This allows others to check the PR, get early feedback, and helps create a better end product.

9. If you have been given access to run test jobs in our Jenkins server, run [Grinder](https://ci.adoptium.net/job/Grinder/) to validate your PR. Your can find recordings about how to use Grinder in AQA Lightning Talk Series: https://github.com/eclipse-openj9/openj9/wiki/AQA-Lightning-Talk-Series.  If you do not have access, the reviewers of your PR will run some tests.  Reviewers may ask you to run extra tests depending on what changes you have made in your PR.

10. Ensure all related Grinder jobs pass and provide the Grinder links in the PR comment. Your changes must also pass the auto PR builds that will be applied to your pull request.

11. Convert PR to `Ready for review` once the PR is ready.

## Contact

Contact the Eclipse Foundation Webdev team via webdev@eclipse-foundation.org.
