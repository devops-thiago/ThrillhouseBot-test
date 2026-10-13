import { describe, it } from "node:test";
import assert from "node:assert/strict";
import type { Config } from "../src/config.ts";
import type { Participant } from "../src/registry.ts";
import { RegistrationError, TeamService, slugify } from "../src/teams.ts";

const config: Config = {
  registryUrl: "http://registry.test",
  registryToken: "test-token",
  maxTeamSize: 4,
  reservedTeamNames: ["organizers", "staff"],
};

const participants: Participant[] = [
  { email: "ada@example.com", name: "Ada" },
  { email: "grace@example.com", name: "Grace" },
  { email: "linus@example.com", name: "Linus" },
  { email: "margaret@example.com", name: "Margaret" },
  { email: "ken@example.com", name: "Ken" },
];

const registry = {
  async getParticipant(email: string): Promise<Participant | null> {
    return participants.find((p) => p.email === email) ?? null;
  },
  async listParticipants(): Promise<Participant[]> {
    return participants;
  },
};

function makeService() {
  const sent: { to: string; teamName: string }[] = [];
  const notifier = {
    async sendConfirmation(to: string, teamName: string) {
      sent.push({ to, teamName });
      return { id: "msg-1", accepted: true };
    },
  };
  return { service: new TeamService(registry, notifier, config), sent };
}

describe("slugify", () => {
  it("lowercases and hyphenates team names", () => {
    assert.equal(slugify("  Null Pointer Exceptions! "), "null-pointer-exceptions");
  });
});

describe("TeamService.register", () => {
  it("registers a team and sends the captain a confirmation", async () => {
    const { service, sent } = makeService();
    const team = await service.register({
      teamName: "Byte Club",
      members: [{ email: "ada@example.com" }, { email: "grace@example.com" }],
    });
    assert.equal(team.slug, "byte-club");
    assert.deepEqual(
      team.members.map((m) => m.name),
      ["Ada", "Grace"],
    );
    assert.deepEqual(sent, [{ to: "ada@example.com", teamName: "Byte Club" }]);
    assert.deepEqual(team.confirmationId, { id: "msg-1", accepted: true });
  });

  it("accepts a team of exactly maxTeamSize members", async () => {
    const { service } = makeService();
    const team = await service.register({
      teamName: "Full House",
      members: participants.slice(0, config.maxTeamSize).map((p) => ({ email: p.email })),
    });
    assert.equal(team.members.length, config.maxTeamSize);
  });

  it("rejects a team larger than maxTeamSize", async () => {
    const { service } = makeService();
    await assert.rejects(
      service.register({
        teamName: "Crowd",
        members: participants.map((p) => ({ email: p.email })),
      }),
      (error: unknown) => error instanceof RegistrationError && error.status === 422,
    );
  });

  it("rejects reserved team names", async () => {
    const { service } = makeService();
    await assert.rejects(
      service.register({ teamName: "staff", members: [{ email: "ken@example.com" }] }),
      (error: unknown) => error instanceof RegistrationError && error.status === 422,
    );
  });

  it("rejects a second team with the same name", async () => {
    const { service } = makeService();
    await service.register({ teamName: "Byte Club", members: [{ email: "ada@example.com" }] });
    await assert.rejects(
      service.register({ teamName: "byte club", members: [{ email: "ken@example.com" }] }),
      (error: unknown) => error instanceof RegistrationError && error.status === 409,
    );
  });
});

describe("TeamService.unassignedParticipants", () => {
  it("lists participants who are not on a team", async () => {
    const { service } = makeService();
    await service.register({
      teamName: "Byte Club",
      members: [{ email: "ada@example.com" }, { email: "grace@example.com" }],
    });
    const unassigned = await service.unassignedParticipants();
    assert.deepEqual(
      unassigned.map((p) => p.email),
      ["linus@example.com", "margaret@example.com", "ken@example.com"],
    );
  });
});
