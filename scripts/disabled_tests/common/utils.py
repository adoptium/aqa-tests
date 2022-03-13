import dataclasses as datacls


DEFAULT_TARGET = "jdk_custom"


def to_shallow_dict(dt) -> dict:
    """
    Convert a dataclass instance to a shallow (only 1 level) dictionary
    """
    return {field.name: getattr(dt, field.name) for field in datacls.fields(dt)}
