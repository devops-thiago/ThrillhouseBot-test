using System.Net.Http.Json;

namespace ShiftRelay;

/// <summary>Posts the handover into the team's on-call channel through the chat webhook.</summary>
public sealed class ChatHandoverAnnouncer(HttpClient http, string webhookUrl) : IHandoverAnnouncer
{
    public async Task<AnnouncementResult> AnnounceAsync(Handover handover)
    {
        var text = $"{handover.RotaCode}: {handover.OutgoingEngineer} handed over to {handover.IncomingEngineer} " +
                   $"at {handover.EffectiveUtc:yyyy-MM-dd HH:mm} UTC. {handover.Notes}".TrimEnd();

        var response = await http.PostAsJsonAsync(webhookUrl, new { text, rota = handover.RotaCode });
        if (!response.IsSuccessStatusCode)
        {
            return AnnouncementResult.Failed($"Chat gateway answered {(int)response.StatusCode}.");
        }

        return AnnouncementResult.Sent();
    }
}
