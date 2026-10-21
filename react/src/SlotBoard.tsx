import { useEffect, useState } from "react";
import { loadSlots } from "./api.ts";
import { parseConfig } from "./config.ts";
import { NoteBadge } from "./NoteBadge.tsx";
import { boardMessage, buildBoard, remaining } from "./slots.ts";
import type { Board } from "./slots.ts";

const config = parseConfig(import.meta.env as Record<string, string | undefined>);

export function SlotBoard() {
  const [board, setBoard] = useState<Board>({ openSlots: [], fullCount: 0 });

  useEffect(() => {
    loadSlots(config.apiBaseUrl, config.pageSize).then((slots) => setBoard(buildBoard(slots)));
  }, []);

  return (
    <section>
      <h1>Food bank pickup slots</h1>
      <p>{boardMessage(board)}</p>
      <ul>
        {board.openSlots.map((slot) => (
          <li key={slot.id}>
            <NoteBadge slot={slot} />
            <span>{remaining(slot)} places left</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
