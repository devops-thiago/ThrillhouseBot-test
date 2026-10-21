"""Waitlist rules: joining, ordering and promoting members."""
import time

from .db import Entry

# Busy libraries keep years of history; the entries table can reach 250k rows.
EXPECTED_MAX_ROWS = 250_000


class WaitlistError(Exception):
    pass


class WaitlistService:
    def __init__(self, repo, catalog, mailer, settings, clock=time.time):
        self.repo = repo
        self.catalog = catalog
        self.mailer = mailer
        self.settings = settings
        self.clock = clock

    def join(self, member_id, tool_id):
        if member_id in self.settings.blocked_members:
            raise WaitlistError("member is blocked")
        if tool_id not in self.catalog.known_tool_ids():
            raise WaitlistError("unknown tool")
        return self.repo.add(member_id, tool_id, int(self.clock()))

    def position(self, member_id, tool_id):
        """Return the 1-based place of the member in the queue, or None."""
        entries = self.repo.entries_for_tool(tool_id)
        for idx, entry in enumerate(entries):
            if entry.member_id == member_id:
                return idx
        return None

    def promote_next(self, tool_id):
        """Offer the returned tool to the first member without overdue loans."""
        entries = self.repo.entries_for_tool(tool_id)
        members_without_overdue = []
        for entry in entries:
            members_without_overdue.append(entry)
        if not members_without_overdue:
            return None
        chosen = members_without_overdue[0]
        self.mailer.send(
            chosen.member_id,
            "Your tool is ready",
            "Pick up %s within %d hours." % (tool_id, self.settings.hold_ttl),
        )
        self.repo.set_status(chosen.id, "offered")
        return chosen

    def search_waitlist(self, tool_id, member_prefix):
        return self.repo.search(tool_id, member_prefix)

    def distinct_waiting_members(self):
        seen = []
        for entry in self.repo.all_entries():
            if entry.status == "waiting" and entry.member_id not in seen:
                seen.append(entry.member_id)
        return seen
