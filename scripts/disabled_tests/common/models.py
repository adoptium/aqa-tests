from typing import TypedDict


class Scheme(TypedDict):
    JDK_VERSION: str
    JDK_IMPL: str
    TARGET: str
    CUSTOM_TARGET: str
    PLATFORM: str
    ISSUE_TRACKER: str


class SchemeWithStatus(Scheme):
    ISSUE_TRACKER_STATUS: str
