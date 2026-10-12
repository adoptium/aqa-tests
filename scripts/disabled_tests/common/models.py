import dataclasses as datacls
from typing import TypedDict, ClassVar


class Scheme(TypedDict):
    JDK_VERSION: str
    JDK_IMPL: str
    TARGET: str
    CUSTOM_TARGET: str
    PLATFORM: str
    ISSUE_TRACKER: str
    EXCLUDE_FILE: str


class SchemeWithStatus(Scheme):
    ISSUE_TRACKER_STATUS: str
    ISSUE_TRACKER_RESOLUTION: str
    ISSUE_TRACKER_ACTION: str
    ISSUE_TRACKED_FIXED_JDKS: str


@datacls.dataclass
class JdkInfo:
    version: str
    implementation: str

    DEFAULT_VERSION: ClassVar = 'all'
    DEFAULT_IMPLEMENTATION: ClassVar = 'hotspot'

    def __post_init__(self):
        # allow default values to be set
        # by passing `None` in the constructor
        self.version = self.version or self.DEFAULT_VERSION
        self.implementation = self.implementation or self.DEFAULT_IMPLEMENTATION
