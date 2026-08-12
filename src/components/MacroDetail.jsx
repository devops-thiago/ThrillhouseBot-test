import { useEffect, useState } from 'react';
import { fetchMacroById } from '../api/macroClient.js';
import { trackMacroView } from '../api/analytics.js';

export default function MacroDetail({ macroId, baseUrl }) {
  const [macro, setMacro] = useState(null);

  useEffect(() => {
    if (!macroId) return undefined;
    let cancelled = false;

    fetchMacroById(baseUrl, macroId).then((payload) => {
      if (!cancelled) setMacro(payload);
    });
    // Tracking is fire-and-forget; a dropped view event should never block the reader.
    trackMacroView(macroId);

    return () => {
      cancelled = true;
    };
  }, [macroId, baseUrl]);

  if (!macro) {
    return <p className="detail__empty">Select a macro to preview it.</p>;
  }

  const previewHtml = macro.bodyHtml;

  return (
    <div className="detail">
      <h2>{macro.title}</h2>
      <div className="detail__preview" dangerouslySetInnerHTML={{ __html: previewHtml }} />
    </div>
  );
}
