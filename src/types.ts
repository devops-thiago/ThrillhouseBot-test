export interface GitHubLabel {
  name: string;
}

export interface Issue {
  id: number;
  number: number;
  title: string;
  body: string | null;
  html_url: string;
  labels: GitHubLabel[];
  created_at: string;
}

export interface DigestEntry {
  url: string;
  title: string;
  createdAt: string;
}

export interface DigestResult {
  processed: number;
  failedNotifications: Issue[];
}
