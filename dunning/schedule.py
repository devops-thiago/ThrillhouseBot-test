"""The dunning ladder.

A schedule is an ordered list of stages; a stage says how many days an invoice
has to be overdue before that reminder is due and which channel it goes out on.
The ladder is data rather than code because collections change it about twice a
year and we do not want a release for that.
"""

from dataclasses import dataclass
from typing import List, Optional

DEFAULT_SCHEDULE = "3:reminder:email,10:notice:email,21:final_notice:email+sms,35:handover:none"

# The stage that hands the invoice to the collections team instead of sending
# the customer anything.
HANDOVER_CHANNEL = "none"


@dataclass(frozen=True)
class Stage:
    """One rung of the ladder."""

    after_days: int
    name: str
    channel: str

    @property
    def is_handover(self) -> bool:
        return self.channel == HANDOVER_CHANNEL


def parse_schedule(raw: str) -> List[Stage]:
    """Parse ``days:name[:channel]`` entries into stages ordered by age.

    Raises ``ValueError`` when an entry is malformed or two stages share the
    same age, which would make the ladder ambiguous.
    """
    stages = []
    seen_days = set()
    for entry in raw.split(","):
        entry = entry.strip()
        if not entry:
            continue
        parts = [part.strip() for part in entry.split(":")]
        if len(parts) not in (2, 3) or not parts[1]:
            raise ValueError(f"malformed dunning stage: {entry!r}")
        try:
            after_days = int(parts[0])
        except ValueError:
            raise ValueError(f"stage {entry!r} does not start with a number of days") from None
        if after_days < 0:
            raise ValueError(f"stage {entry!r} has a negative age")
        if after_days in seen_days:
            raise ValueError(f"two stages are due after {after_days} days")
        seen_days.add(after_days)
        stages.append(Stage(after_days, parts[1], parts[2] if len(parts) == 3 else "email"))
    if not stages:
        raise ValueError("the dunning schedule is empty")
    return sorted(stages, key=lambda stage: stage.after_days)


def due_stage(stages: List[Stage], days_overdue: int) -> Optional[Stage]:
    """Return the furthest stage an invoice this old has reached, if any."""
    reached = [stage for stage in stages if days_overdue >= stage.after_days]
    return reached[-1] if reached else None


def stage_position(stages: List[Stage], name: Optional[str]) -> int:
    """Return the index of ``name`` in the ladder, or -1 when it is not on it.

    An unknown name is treated as "not on the ladder" so that renaming a stage
    restarts the invoice at the stage it is due for rather than skipping it.
    """
    for position, stage in enumerate(stages):
        if stage.name == name:
            return position
    return -1
