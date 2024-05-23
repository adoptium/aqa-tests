# AQAvit Terminology 

---

In AQAvit, we attempt to treat all types of test targets in the same way, to make it easier to automate and also easier to add new tests and onboard new people. This normalization starts with some agreed-upon common terminology, naming and labeling conventions, and repeatable/predictable behaviour applied across all types of test material.



#### Quick review of the terminology used in AQAvit

<img width="949" alt="Screenshot 2024-03-14 at 10 25 01 AM" src="https://github.com/adoptium/aqa-tests/assets/2836948/286dfdfc-39ac-4a4f-a9af-a487ade90836">


* **group** indicates a collection of tests that are related in some way
    * For example the openjdk group is related by where those tests are pulled from, they are the jtreg tests from the upstream OpenJDK project.
* **level** refers to an arbitrarily divided subset of the tests in a group. The different levels that we divide tests into are sanity, extended, special and dev
    * **sanity** level is meant to contain short-running tests that test regularly changing code and have shown to be often finding defects.
    * **extended** level is meant to contain longer tests or tests that exercise code that is not changing as often or do not find defects as often
    * **special** level is reserved for tests that need special setup or need to be handled in a special way
    * **dev** level is for newly added tests and tests that are under-development or are somehow not stable and not yet ready to graduate to other levels