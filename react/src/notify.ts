import type { Slot } from "./slots.ts";

export interface SmsGateway {
  send(phone: string, text: string): Promise<{ ok: boolean }>;
}

// Real gateway: rejects with a RangeError unless the phone number is E.164.
export function createHttpGateway(baseUrl: string): SmsGateway {
  return {
    async send(phone, text) {
      if (!/^\+[1-9]\d{6,14}$/.test(phone)) {
        throw new RangeError("phone must be E.164");
      }
      const res = await fetch(`${baseUrl}/sms`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phone, text }),
      });
      return { ok: res.ok };
    },
  };
}

export async function sendReminder(gateway: SmsGateway, phone: string, slot: Slot): Promise<boolean> {
  const result = await gateway.send(phone, `Pickup ${slot.title} at ${slot.start}`);
  return result.ok;
}
