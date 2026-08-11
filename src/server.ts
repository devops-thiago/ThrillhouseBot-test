import express, { Express, Request, Response } from 'express';
import { getAllEntries } from './db';

export function createServer(): Express {
  const app = express();

  app.get('/health', (_req: Request, res: Response) => {
    res.json({ status: 'ok' });
  });

  app.get('/digest', async (_req: Request, res: Response) => {
    const entries = await getAllEntries();
    res.json({ count: entries.length, entries });
  });

  return app;
}
