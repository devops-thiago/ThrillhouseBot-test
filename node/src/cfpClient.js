// Client for the CFP API. Each page holds at most `pageSize` proposals and
// the response carries `nextPage` (null on the last page).
export class CfpClient {
  constructor(fetchPage) {
    this.fetchPage = fetchPage;
  }

  async listPage(page, pageSize) {
    return this.fetchPage({ page, pageSize });
  }
}

export async function importProposals(client, pageSize) {
  const first = await client.listPage(1, pageSize);
  return first.items;
}
