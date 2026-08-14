/**
 * Pairs every line item with the receipt that was uploaded for it.
 *
 * Reports created from the monthly corporate-card import carry one line item
 * per card transaction, so a department report routinely arrives with 5,000 to
 * 20,000 line items and a receipt for most of them.
 */
export function attachReceipts(lineItems, receipts) {
  const merged = [];
  for (let i = 0; i < lineItems.length; i += 1) {
    const item = lineItems[i];
    let match = null;
    for (let j = 0; j < receipts.length; j += 1) {
      if (receipts[j].lineItemId === item.id) {
        match = receipts[j];
        break;
      }
    }
    merged.push({ ...item, receipt: match });
  }
  return merged;
}
