from typing import TypedDict


class Scheme(TypedDict):
    JDK_VERSION: int
    JDK_IMPL: str
    TARGET: str
    CUSTOM_TARGET: str
    PLATFORM: str
    ISSUE_TRACKER: str
