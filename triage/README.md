# AdoptOpenJDK Test Triage Rotas

### OpenJ9 Rotas:
- [OpenJDK8](./openj9_test_triage_rotas.md#8)
- [OpenJDK11](./openj9_test_triage_rotas.md#11)
- [OpenJDK12](./openj9_test_triage_rotas.md#12)
- [OpenJDK Head](./openj9_test_triage_rotas.md#head)

### Hotspot Rotas:
- [OpenJDK8](./hotspot_test_triage_rotas.md#8)
- [OpenJDK11](./hotspot_test_triage_rotas.md#11)
- [OpenJDK12](./hotspot_test_triage_rotas.md#12)
- [OpenJDK13](./hotspot_test_triage_rotas.md#head)

### Details:

Triage is defined here as "Exclude the failing test, raise a bug against the relevant party, and put the bug URL in the exclude file.".

Triagers named in the rotas linked above are committed to "best effort" triage on test runs which are:
- That test group.
- That platform.
- That release.
- That Virtual Machine.
- Run against release streams.
- Run as part of a predetermined test schedule (e.g. the automated nightlies).

Triagers named above are *not* committed to triaging test runs which are:
- Run by an individual.
- Run against private forks/branches.
- Run outside of the predetermined schedule.
- Run using any test material other than the primary source for that platform/release/test group.
- Run without the triager's knowledge.

(Note: These are guidelines only, and do not restrict triagers from committing to perform triage outside of these limits.)

Finally, do note that the triagers in the rotas do not have a monopoly on triaging. 

Non-rota individuals are explicitly welcome to perform triage as and when they can.