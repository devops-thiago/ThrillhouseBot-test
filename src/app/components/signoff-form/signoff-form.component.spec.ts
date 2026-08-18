import { FormsModule } from '@angular/forms';
import { TestBed } from '@angular/core/testing';

import { SignoffFormComponent } from './signoff-form.component';
import { SignoffRequest } from '../../models/postmortem.model';

describe('SignoffFormComponent', () => {
  let component: SignoffFormComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [SignoffFormComponent],
    });

    component = TestBed.createComponent(SignoffFormComponent).componentInstance;
  });

  it('refuses an approval while a blocker is outstanding', () => {
    component.blockers = ['no action items were recorded'];

    expect(component.canSubmit).toBeFalse();
  });

  it('allows an approval once the blockers are cleared, with or without a note', () => {
    expect(component.canSubmit).toBeTrue();
  });

  it('requires a note when changes are requested', () => {
    component.verdict = 'changes_requested';
    expect(component.canSubmit).toBeFalse();

    component.note = '  Please add the detection timeline.  ';
    expect(component.canSubmit).toBeTrue();
  });

  it('emits the trimmed note and clears the box', () => {
    const emitted: SignoffRequest[] = [];
    component.signoff.subscribe((request) => emitted.push(request));

    component.verdict = 'changes_requested';
    component.note = '  Add the detection timeline.  ';
    component.submit();

    expect(emitted).toEqual([
      { verdict: 'changes_requested', note: 'Add the detection timeline.' },
    ]);
    expect(component.note).toBe('');
  });

  it('does not let the same reviewer sign off twice', () => {
    component.alreadySignedOff = true;
    component.submit();

    expect(component.canSubmit).toBeFalse();
  });
});
