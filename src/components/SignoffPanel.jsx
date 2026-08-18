import { useReducer } from 'react';

const initialState = { verdict: 'approved', note: '' };

function reducer(state, action) {
  switch (action.type) {
    case 'verdict':
      return { ...state, verdict: action.verdict };
    case 'note':
      return { ...state, note: action.note };
    case 'reset':
      return initialState;
    default:
      return state;
  }
}

/**
 * The reviewer's verdict. Approving is only offered once the process checks
 * pass; asking for changes is always available but needs a note, since a bare
 * "changes requested" is what sends a postmortem round the houses.
 */
export default function SignoffPanel({ checks, alreadyReviewed, submitting, onSubmit }) {
  const [form, dispatch] = useReducer(reducer, initialState);

  const noteRequired = form.verdict === 'changes_requested';
  const canSubmit =
    !alreadyReviewed &&
    !submitting &&
    (noteRequired ? form.note.trim().length > 0 : checks.length === 0);

  function handleSubmit(event) {
    event.preventDefault();
    if (!canSubmit) {
      return;
    }
    onSubmit({ verdict: form.verdict, note: form.note.trim() });
    dispatch({ type: 'reset' });
  }

  return (
    <form className="signoff" onSubmit={handleSubmit}>
      <h3>Record your review</h3>

      {checks.length > 0 && (
        <ul className="checks">
          {checks.map((check) => (
            <li key={check}>{check}</li>
          ))}
        </ul>
      )}

      {alreadyReviewed && <p className="done">You have already reviewed this postmortem.</p>}

      <fieldset>
        <label>
          <input
            type="radio"
            name="verdict"
            value="approved"
            checked={form.verdict === 'approved'}
            disabled={checks.length > 0}
            onChange={() => dispatch({ type: 'verdict', verdict: 'approved' })}
          />
          Approve
        </label>
        <label>
          <input
            type="radio"
            name="verdict"
            value="changes_requested"
            checked={form.verdict === 'changes_requested'}
            onChange={() => dispatch({ type: 'verdict', verdict: 'changes_requested' })}
          />
          Request changes
        </label>
      </fieldset>

      <textarea
        aria-label="Review note"
        rows={4}
        value={form.note}
        placeholder={noteRequired ? 'What the author needs to change' : 'Optional note'}
        onChange={(event) => dispatch({ type: 'note', note: event.target.value })}
      />

      <button type="submit" disabled={!canSubmit}>
        {submitting ? 'Recording…' : 'Submit review'}
      </button>
    </form>
  );
}
