## Alpine Exclude Files

These exclude files are used **in addition to** to the usual ProblemList_openjdk##.txt file, but only on Alpine.

This is done this way because JTReg currently has no mechanism for excluding a test on Alpine linux only.

Users are advised to use these platforms:

- linux-all
- linux-x64
- linux-aarch64

**Caution:** If a test is excluded in the usual ProblemList as well, then this may supersede that exclusion during Alpine testing.
So if I exclude a test on linux-all in the usual ProblemList, and then exclude it here on linux-x64, then this test will run on aarch64 Alpine.