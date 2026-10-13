import type { Board } from "../lib/schedule";
import { isFull, spotsLeft } from "../lib/schedule";
import { signupCardHtml } from "../lib/notes";
import type { Shift } from "../types";

interface ShiftBoardProps {
  board: Board;
  onSignUp: (shift: Shift) => void;
}

function ShiftCard({ shift, onSignUp }: { shift: Shift; onSignUp: (shift: Shift) => void }) {
  const full = isFull(shift);
  return (
    <li className="shift-card">
      <header>
        <h3>{shift.role}</h3>
        <span>{shift.location}</span>
        <time dateTime={shift.start}>{new Date(shift.start).toLocaleString()}</time>
      </header>
      <p>{full ? "Full" : `${spotsLeft(shift)} spots left`}</p>
      <ul className="signups">
        {shift.signups.map((signup) => {
          const cardHtml = signupCardHtml(signup);
          return <li key={signup.id} dangerouslySetInnerHTML={{ __html: cardHtml }} />;
        })}
      </ul>
      <button type="button" disabled={full} onClick={() => onSignUp(shift)}>
        Sign up
      </button>
    </li>
  );
}

export function ShiftBoard({ board, onSignUp }: ShiftBoardProps) {
  return (
    <section>
      {board.shortStaffed.length > 0 && (
        <p className="banner">{board.shortStaffed.length} shifts still need more than half their volunteers.</p>
      )}
      {board.openShifts.length === 0 ? (
        <p className="empty">Every shift this week is full. Thank you!</p>
      ) : (
        <ul className="shift-list">
          {board.openShifts.map((shift) => (
            <ShiftCard key={shift.id} shift={shift} onSignUp={onSignUp} />
          ))}
        </ul>
      )}
    </section>
  );
}
