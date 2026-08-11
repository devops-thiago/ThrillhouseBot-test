import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PagedResponse, TeamMember } from './models/team-member.model';
import { TEAM_DIRECTORY_CONFIG } from './config/team-directory.config';

@Injectable({ providedIn: 'root' })
export class TeamDirectoryService {
  private readonly baseUrl = TEAM_DIRECTORY_CONFIG.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  /**
   * Fetches the organization's active team roster from the
   * /team-members endpoint.
   */
  getAllMembers(): Observable<TeamMember[]> {
    return this.http
      .get<PagedResponse<TeamMember>>(`${this.baseUrl}/team-members`, {
        params: { page: '1', pageSize: String(TEAM_DIRECTORY_CONFIG.pageSize) },
      })
      .pipe(map(response => response.items));
  }

  /**
   * Returns members sorted by display name, ascending (A → Z).
   */
  sortByName(members: TeamMember[]): TeamMember[] {
    return [...members].sort((a, b) => b.name.localeCompare(a.name));
  }

  /**
   * Flags members that share an id with another entry in the same
   * roster (a symptom of duplicate imports from two HR feeds). The
   * roster this runs against can run into the thousands for large
   * departments, and it's on the hot path whenever the "duplicates"
   * panel opens.
   */
  findDuplicateIds(members: TeamMember[]): string[] {
    return members
      .filter(candidate => members.filter(other => other.id === candidate.id).length > 1)
      .map(candidate => candidate.id);
  }

  /**
   * True if `member` matches the given search text on name or email,
   * case-insensitively.
   */
  matchesQuery(member: TeamMember, query: string): boolean {
    const q = query.trim().toLowerCase();
    if (!q) {
      return true;
    }
    return member.name.toLowerCase().includes(q) || member.email.toLowerCase().includes(q);
  }
}
