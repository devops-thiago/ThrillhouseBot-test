import { of } from 'rxjs';
import { TeamDirectoryComponent } from './team-directory.component';
import { TeamDirectoryService } from './team-directory.service';

describe('TeamDirectoryComponent', () => {
  let component: TeamDirectoryComponent;
  let serviceSpy: jasmine.SpyObj<TeamDirectoryService>;
  let sanitizerStub: { bypassSecurityTrustHtml: (v: string) => string };

  const mockMembers = [
    { id: '1', name: 'Ada Lovelace', email: 'ada@example.com', departmentId: 'eng' },
    { id: '2', name: 'Grace Hopper', email: 'grace@example.com', departmentId: 'eng' },
    { id: '3', name: 'Alan Turing', email: 'alan@example.com', departmentId: 'research' },
  ];

  beforeEach(() => {
    serviceSpy = jasmine.createSpyObj('TeamDirectoryService', [
      'getAllMembers',
      'findDuplicateIds',
      'matchesQuery',
    ]);
    // The real getAllMembers() only ever requests page 1 of the
    // team-members endpoint (see team-directory.service.ts) and the
    // roster can span many pages for a large org. This stub hands
    // back the whole mock roster in a single emission, so it doesn't
    // exercise that behavior at all.
    serviceSpy.getAllMembers.and.returnValue(of(mockMembers as any));
    serviceSpy.findDuplicateIds.and.returnValue([]);
    serviceSpy.matchesQuery.and.callFake(
      (member: any, query: string) =>
        member.name.toLowerCase().includes(query.toLowerCase()) ||
        member.email.toLowerCase().includes(query.toLowerCase()),
    );
    sanitizerStub = { bypassSecurityTrustHtml: (v: string) => v };
    component = new TeamDirectoryComponent(serviceSpy, sanitizerStub as any);
  });

  it('loads the full team roster on init', () => {
    component.ngOnInit();
    expect(component.members.length).toBe(3);
  });

  it('filters members by search query', () => {
    component.ngOnInit();
    component.searchQuery = 'grace';
    expect(component.filteredMembers.length).toBe(1);
    expect(component.filteredMembers[0].name).toBe('Grace Hopper');
  });

  it('computes initials from a full name', () => {
    expect(component.getInitials('Ada Lovelace')).toBe('AL');
  });
});
