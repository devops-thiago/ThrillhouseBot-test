import { runDigestCycle } from '../src/digestService';
import * as db from '../src/db';
import { Issue } from '../src/types';

jest.mock('../src/db');

const mockedDb = db as jest.Mocked<typeof db>;

const baseConfig = {
  githubOwner: 'acme',
  githubRepo: 'widgets',
  githubToken: 'test-token',
  digestLabels: ['bug'],
};

function makeIssue(overrides: Partial<Issue> = {}): Issue {
  return {
    id: 1,
    number: 1,
    title: 'Widget breaks on load',
    body: 'Steps to reproduce...',
    html_url: 'https://github.com/acme/widgets/issues/1',
    labels: [{ name: 'bug' }],
    created_at: '2024-01-01T00:00:00Z',
    ...overrides,
  };
}

describe('runDigestCycle', () => {
  beforeEach(() => {
    jest.resetAllMocks();
    mockedDb.getAllEntries.mockResolvedValue([]);
    mockedDb.insertEntry.mockResolvedValue(undefined);
  });

  it('propagates errors when the GitHub fetch fails', async () => {
    const fetchIssues = jest.fn().mockRejectedValue(new Error('network error'));
    const notify = jest.fn().mockResolvedValue(undefined);

    await expect(
      runDigestCycle({ fetchIssues, notify, config: baseConfig })
    ).rejects.toThrow('network error');
  });

  it('records processed issues that match the configured labels', async () => {
    const issue = makeIssue();
    const fetchIssues = jest.fn().mockResolvedValue([issue]);
    const notify = jest.fn().mockResolvedValue(undefined);

    const result = await runDigestCycle({ fetchIssues, notify, config: baseConfig });

    expect(result.processed).toBe(1);
    expect(mockedDb.insertEntry).toHaveBeenCalledTimes(1);
  });

  it('skips issues that do not match the configured labels', async () => {
    const issue = makeIssue({ labels: [{ name: 'enhancement' }] });
    const fetchIssues = jest.fn().mockResolvedValue([issue]);
    const notify = jest.fn().mockResolvedValue(undefined);

    const result = await runDigestCycle({ fetchIssues, notify, config: baseConfig });

    expect(result.processed).toBe(0);
    expect(notify).not.toHaveBeenCalled();
  });
});
