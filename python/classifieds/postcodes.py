"""Postcode to sales-region lookup."""
from dataclasses import dataclass
from typing import Dict


class PostcodeNotFound(LookupError):
    """Raised when a postcode does not map to any sales region."""


@dataclass(frozen=True)
class Region:
    name: str


class PostcodeLookup:
    """Maps the outward part of a postcode (e.g. ``SW1A``) to a sales region.

    ``lookup`` raises :class:`PostcodeNotFound` for any postcode that is not in
    the table. It never returns ``None``.
    """

    def __init__(self, table: Dict[str, str]):
        self._table = {k.upper(): v for k, v in table.items()}

    def lookup(self, postcode: str) -> Region:
        outward = postcode.strip().upper().split(" ")[0]
        try:
            return Region(self._table[outward])
        except KeyError:
            raise PostcodeNotFound(postcode) from None
