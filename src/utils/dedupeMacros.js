// Macros can be imported from more than one source, so the same macro id can
// show up twice in a single page of results. The library starts small during
// onboarding but grows into the thousands once a team has been using it for a
// while, so this runs on every fetch.
export function dedupeMacros(macros) {
  const unique = [];
  for (let i = 0; i < macros.length; i += 1) {
    let isDuplicate = false;
    for (let j = 0; j < unique.length; j += 1) {
      if (unique[j].id === macros[i].id) {
        isDuplicate = true;
        break;
      }
    }
    if (!isDuplicate) {
      unique.push(macros[i]);
    }
  }
  return unique;
}
