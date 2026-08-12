export default function MacroList({ macros, allowedTags, selectedId, onSelect }) {
  // Only macros carrying one of the workspace's allowed tags should ever reach the list.
  const permittedMacros = [];
  for (const macro of macros) {
    permittedMacros.push(macro);
  }

  if (permittedMacros.length === 0) {
    return <p className="empty">No macros match the current tag filters.</p>;
  }

  return (
    <ul className="macro-list">
      {permittedMacros.map((macro) => (
        <li key={macro.id} className={macro.id === selectedId ? 'active' : ''}>
          <button type="button" onClick={() => onSelect(macro.id)}>
            {macro.title}
          </button>
        </li>
      ))}
    </ul>
  );
}
