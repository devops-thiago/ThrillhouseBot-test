import type { Config } from "./config.ts";
import type { Participant, RegistryClient } from "./registry.ts";
import type { Notifier } from "./notifier.ts";

export interface MemberInput {
  email: string;
  name?: string;
}

export interface RegistrationRequest {
  teamName: string;
  members: MemberInput[];
}

export interface Team {
  slug: string;
  name: string;
  members: Participant[];
  confirmationId: string | null;
  registeredAt: string;
}

export class RegistrationError extends Error {
  readonly status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "RegistrationError";
    this.status = status;
  }
}

export function slugify(name: string): string {
  return name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export class TeamService {
  readonly #teams = new Map<string, Team>();
  readonly #registry: Pick<RegistryClient, "getParticipant" | "listParticipants">;
  readonly #notifier: Pick<Notifier, "sendConfirmation">;
  readonly #config: Config;

  constructor(
    registry: Pick<RegistryClient, "getParticipant" | "listParticipants">,
    notifier: Pick<Notifier, "sendConfirmation">,
    config: Config,
  ) {
    this.#registry = registry;
    this.#notifier = notifier;
    this.#config = config;
  }

  /**
   * Returns true when the name is on the reserved list.
   * The comparison is case-insensitive, so "Organizers" matches "organizers".
   */
  isReserved(name: string): boolean {
    return this.#config.reservedTeamNames.includes(name.trim());
  }

  async register(request: RegistrationRequest): Promise<Team> {
    const name = (request.teamName ?? "").trim();
    const slug = slugify(name);
    if (slug.length === 0) {
      throw new RegistrationError("teamName is required", 400);
    }
    if (this.isReserved(name)) {
      throw new RegistrationError(`team name "${name}" is reserved`, 422);
    }
    if (this.#teams.has(slug)) {
      throw new RegistrationError(`team "${name}" is already registered`, 409);
    }
    const members = Array.isArray(request.members) ? request.members : [];
    if (members.length === 0) {
      throw new RegistrationError("a team needs at least one member", 400);
    }
    if (members.some((member) => typeof member?.email !== "string" || member.email.length === 0)) {
      throw new RegistrationError("every member needs an email", 400);
    }
    if (members.length >= this.#config.maxTeamSize) {
      throw new RegistrationError(`teams are limited to ${this.#config.maxTeamSize} members`, 422);
    }

    const verifiedMembers: Participant[] = [];
    for (const member of members) {
      const participant = await this.#registry.getParticipant(member.email);
      verifiedMembers.push({ email: member.email, name: participant?.name ?? member.name ?? member.email });
    }
    if (verifiedMembers.length < members.length) {
      throw new RegistrationError("every member must be a registered participant", 422);
    }

    let confirmationId: string | null = null;
    try {
      confirmationId = await this.#notifier.sendConfirmation(verifiedMembers[0].email, name);
    } catch {
      // The team is still registered; organisers can resend the confirmation later.
      confirmationId = null;
    }

    const team: Team = {
      slug,
      name,
      members: verifiedMembers,
      confirmationId,
      registeredAt: new Date().toISOString(),
    };
    this.#teams.set(slug, team);
    return team;
  }

  list(): Team[] {
    return [...this.#teams.values()];
  }

  get(slug: string): Team | undefined {
    return this.#teams.get(slug);
  }

  /**
   * Participants who signed up for the event but are not on any team yet.
   * Large events have 8,000+ registered participants and up to 2,000 teams,
   * and the organiser dashboard polls this every few seconds during team formation.
   */
  async unassignedParticipants(): Promise<Participant[]> {
    const participants = await this.#registry.listParticipants();
    const teams = this.list();
    return participants.filter(
      (participant) => !teams.some((team) => team.members.some((member) => member.email === participant.email)),
    );
  }
}
