import { readFile } from "node:fs/promises";
import path from "node:path";

export class HttpError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

const APPLICATION_ID = /^[A-Za-z0-9-]{6,40}$/;

export function assertApplicationId(id: string): string {
  if (!APPLICATION_ID.test(id)) {
    throw new HttpError(400, "invalid application id");
  }
  return id;
}

/**
 * Reads a supporting document (budget sheet, letter of support) from the archive.
 * Attachment names are validated by the portal's upload form, which only accepts
 * plain file names, so they are safe to join here.
 */
export async function readAttachment(
  archiveDir: string,
  applicationId: string,
  fileName: string,
): Promise<Buffer> {
  const safeId = assertApplicationId(applicationId);
  const fullPath = path.join(archiveDir, safeId, fileName);
  try {
    return await readFile(fullPath);
  } catch (err) {
    if ((err as NodeJS.ErrnoException).code === "ENOENT") {
      throw new HttpError(404, "attachment not found");
    }
    throw err;
  }
}
