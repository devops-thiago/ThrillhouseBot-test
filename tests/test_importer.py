"""Tests for the import workflow."""
import sqlite3
from unittest.mock import Mock

import pytest

from importer.service import dedupe_against_existing, run_import


def _issue(number, title="Bug report", state="open", assignees=None):
    return {
        "number": number,
        "title": title,
        "state": state,
        "assignees": assignees if assignees is not None else [{"login": "octocat"}],
        "labels": [],
    }


def test_dedupe_against_existing_skips_known_numbers():
    store = Mock()
    store.all_titles.return_value = [(1, "Existing issue")]
    fetched = [_issue(1), _issue(2)]

    result = dedupe_against_existing(fetched, store)

    assert [issue["number"] for issue in result] == [2]


def test_run_import_reports_new_issue_count():
    client = Mock()
    client.fetch_open_issues.return_value = [_issue(1), _issue(2)]
    store = Mock()
    store.all_titles.return_value = []
    store.save_issues.return_value = True

    result = run_import(client, store)

    assert result["count"] == 2
    store.save_issues.assert_called_once()


def test_run_import_surfaces_storage_errors():
    """A failed save should not be swallowed silently by the import run."""
    client = Mock()
    client.fetch_open_issues.return_value = [_issue(1)]
    store = Mock()
    store.all_titles.return_value = []
    store.save_issues.side_effect = sqlite3.OperationalError("database is locked")

    with pytest.raises(sqlite3.OperationalError):
        run_import(client, store)
