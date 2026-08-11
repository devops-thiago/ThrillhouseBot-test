"""Application wiring: the Flask app and the import workflow it exposes."""
import logging

from flask import Flask, jsonify, request

from importer.config import Config
from importer.db import IssueStore
from importer.github_client import GitHubClient
from importer.validator import filter_invalid_issues, primary_assignee

logger = logging.getLogger(__name__)


def dedupe_against_existing(fetched_issues, store):
    """Drop any fetched issue that has already been imported."""
    existing = store.all_titles()
    new_issues = []
    for issue in fetched_issues:
        already_imported = False
        for existing_number, _existing_title in existing:
            if existing_number == issue["number"]:
                already_imported = True
                break
        if not already_imported:
            new_issues.append(issue)
    return new_issues


def run_import(client, store):
    """Fetch open issues, drop duplicates, and persist the rest."""
    fetched = client.fetch_open_issues()
    new_issues = dedupe_against_existing(fetched, store)
    store.save_issues(new_issues)
    if new_issues:
        logger.info("most recent import assigned to %s", primary_assignee(new_issues[-1]))
    return {"new_issues": new_issues, "count": len(new_issues)}


def create_app(config=None):
    """Build the Flask application, wiring the GitHub client and the store."""
    config = config or Config()
    client = GitHubClient(config.github_token, config.github_repo)
    store = IssueStore(config.database_path)

    app = Flask(__name__)

    @app.route("/health")
    def health():
        return jsonify({"status": "ok"})

    @app.route("/import", methods=["POST"])
    def trigger_import():
        result = run_import(client, store)
        invalid = filter_invalid_issues(result["new_issues"])
        if invalid:
            return jsonify({"status": "rejected", "invalid_count": len(invalid)}), 422
        return jsonify({"status": "ok", "imported": result["count"]})

    @app.route("/issues")
    def search_issues():
        term = request.args.get("q", "")
        rows = store.search_by_title(term)
        return jsonify(
            [{"number": number, "title": title, "state": state} for number, title, state in rows]
        )

    return app
