import { useEffect, useState } from 'react';
import SignoffPanel from './SignoffPanel';
import { fetchPostmortem, submitSignoff } from '../api/reviewApi';
import { formatDate, toParagraphs } from '../lib/format';
import { hasReviewed, outstandingChecks, reviewersStillNeeded } from '../lib/reviewChecks';

export default function PostmortemView({ postmortemId, reviewerId, onReviewed }) {
  const [postmortem, setPostmortem] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);

  // The queue listing is a summary, so opening a postmortem fetches the full
  // record. A reviewer clicking down the queue can leave earlier requests in
  // flight, hence the guard on the stale response.
  useEffect(() => {
    let active = true;
    setPostmortem(null);
    setLoadFailed(false);

    fetchPostmortem(postmortemId)
      .then((detail) => {
        if (active) {
          setPostmortem(detail);
        }
      })
      .catch(() => {
        if (active) {
          setLoadFailed(true);
        }
      });

    return () => {
      active = false;
    };
  }, [postmortemId]);

  if (loadFailed) {
    return <section className="detail">That postmortem could not be opened.</section>;
  }

  if (!postmortem) {
    return <section className="detail">Loading the postmortem…</section>;
  }

  const checks = outstandingChecks(postmortem);
  const stillNeeded = reviewersStillNeeded(postmortem);

  async function handleSubmit(review) {
    setSubmitting(true);
    setSubmitError(null);

    try {
      const recorded = await submitSignoff(postmortem.id, review);
      setPostmortem({
        ...postmortem,
        status: recorded.status,
        signoffs: [...postmortem.signoffs, recorded.signoff],
      });
      onReviewed(postmortem.id, recorded.status);
    } catch {
      setSubmitError('That review was not recorded. Try again in a moment.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="detail">
      <h2>
        {postmortem.incidentId} · {postmortem.title}
      </h2>
      <p className="meta">
        {postmortem.severity} · {postmortem.authorName} · detected{' '}
        {formatDate(postmortem.detectedAt)} · submitted {formatDate(postmortem.submittedAt)}
      </p>

      <div className="narrative">
        {toParagraphs(postmortem.narrative).map((paragraph, index) => (
          <p key={index}>{paragraph}</p>
        ))}
      </div>

      <h3>Contributing factors</h3>
      <ul className="factors">
        {(postmortem.contributingFactors ?? []).map((factor) => (
          <li key={factor}>{factor}</li>
        ))}
      </ul>

      <h3>Action items</h3>
      <table className="action-items">
        <tbody>
          {postmortem.actionItems.map((item) => (
            <tr key={item.id}>
              <td>{item.description}</td>
              <td>{item.owner || 'no owner'}</td>
              <td>{item.dueOn ? `due ${item.dueOn}` : 'no due date'}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3>Reviews so far</h3>
      <ul className="signoffs">
        {postmortem.signoffs.map((signoff) => (
          <li key={signoff.reviewerId}>
            {signoff.reviewerName} — {signoff.verdict} on {formatDate(signoff.recordedAt)}
          </li>
        ))}
      </ul>
      <p className="outstanding">
        {stillNeeded === 0
          ? 'Every required review is in.'
          : `${stillNeeded} reviewer(s) still needed.`}
      </p>

      {submitError && <p className="warn">{submitError}</p>}

      <SignoffPanel
        checks={checks}
        alreadyReviewed={hasReviewed(postmortem, reviewerId)}
        submitting={submitting}
        onSubmit={handleSubmit}
      />
    </section>
  );
}
