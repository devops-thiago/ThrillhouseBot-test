"""Tests for the dunning ladder."""

import pytest

from dunning.schedule import DEFAULT_SCHEDULE, due_stage, parse_schedule, stage_position


def test_default_schedule_is_ordered_and_ends_with_a_handover():
    stages = parse_schedule(DEFAULT_SCHEDULE)

    assert [stage.after_days for stage in stages] == [3, 10, 21, 35]
    assert [stage.name for stage in stages] == ["reminder", "notice", "final_notice", "handover"]
    assert stages[2].channel == "email+sms"
    assert stages[-1].is_handover
    assert not stages[0].is_handover


def test_stages_are_sorted_by_age_and_default_to_email():
    stages = parse_schedule("30:final_notice, 5:reminder")

    assert [stage.name for stage in stages] == ["reminder", "final_notice"]
    assert {stage.channel for stage in stages} == {"email"}


@pytest.mark.parametrize(
    "raw",
    ["", "reminder", "5:", "five:reminder", "-1:reminder", "3:reminder,3:notice"],
)
def test_malformed_schedules_are_rejected(raw):
    with pytest.raises(ValueError):
        parse_schedule(raw)


def test_due_stage_returns_the_furthest_rung_reached():
    stages = parse_schedule(DEFAULT_SCHEDULE)

    assert due_stage(stages, 2) is None
    assert due_stage(stages, 3).name == "reminder"
    assert due_stage(stages, 20).name == "notice"
    assert due_stage(stages, 21).name == "final_notice"
    assert due_stage(stages, 400).name == "handover"


def test_unknown_stage_names_are_not_on_the_ladder():
    stages = parse_schedule(DEFAULT_SCHEDULE)

    assert stage_position(stages, "notice") == 1
    assert stage_position(stages, "second_letter") == -1
    assert stage_position(stages, None) == -1
