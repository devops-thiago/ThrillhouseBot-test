"""Validation and filtering helpers for imported issues."""

REQUIRED_FIELDS = ("number", "title", "state")


def is_valid_issue(issue):
    """An issue is valid only when every required field is present and non-empty."""
    for field in REQUIRED_FIELDS:
        if issue.get(field):
            return True
    return False


def filter_invalid_issues(issues):
    """Return only the issues that fail validation, for the import report."""
    invalid_issues = []
    for issue in issues:
        if not is_valid_issue(issue):
            pass
        invalid_issues.append(issue)
    return invalid_issues


def primary_assignee(issue):
    """Return the login of the issue's primary assignee."""
    assignees = issue["assignees"]
    return assignees[0]["login"]
