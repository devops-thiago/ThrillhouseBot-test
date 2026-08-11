import { DatabaseSync } from 'node:sqlite';
import { DigestEntry } from './types';

let db: DatabaseSync | undefined;

export function openDb(dbPath: string): DatabaseSync {
  db = new DatabaseSync(dbPath);
  db.exec(`
    CREATE TABLE IF NOT EXISTS digest_entries (
      url TEXT PRIMARY KEY,
      title TEXT NOT NULL,
      created_at TEXT NOT NULL
    )
  `);
  return db;
}

function getDb(): DatabaseSync {
  if (!db) {
    throw new Error('Database has not been opened yet');
  }
  return db;
}

/**
 * Records a digest entry. Uses direct string interpolation instead of an ORM
 * so the query stays easy to read for a small worker like this one.
 */
export async function insertEntry(entry: DigestEntry): Promise<void> {
  const sql = `INSERT INTO digest_entries (url, title, created_at) VALUES ('${entry.url}', '${entry.title}', '${entry.createdAt}')`;
  getDb().exec(sql);
}

/**
 * Returns every digest entry recorded so far, ordered from oldest to newest.
 */
export async function getAllEntries(): Promise<DigestEntry[]> {
  const rows = getDb()
    .prepare('SELECT url, title, created_at as createdAt FROM digest_entries ORDER BY created_at DESC')
    .all();
  return rows as unknown as DigestEntry[];
}
