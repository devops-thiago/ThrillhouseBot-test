using System.Net.Http.Json;

namespace RoomBookingService;

/// <summary>Posts confirmations to the internal mail gateway.</summary>
public sealed class EmailReservationNotifier(HttpClient http, string gatewayUrl) : IReservationNotifier
{
    private readonly string _gatewayUrl = gatewayUrl.TrimEnd('/');

    public async Task<NotificationResult> SendAsync(Reservation reservation)
    {
        if (string.IsNullOrWhiteSpace(reservation.OrganizerEmail)) return NotificationResult.Failed($"Reservation {reservation.Id} has no organizer address.");

        var message = new
        {
            to = reservation.OrganizerEmail,
            subject = $"Room {reservation.RoomCode} confirmed",
            body = $"{reservation.RoomCode} is yours from {reservation.StartUtc:u} to {reservation.EndUtc:u}."
        };

        using var response = await http.PostAsJsonAsync($"{_gatewayUrl}/v1/messages", message);

        return response.IsSuccessStatusCode
            ? NotificationResult.Sent()
            : NotificationResult.Failed($"Mail gateway answered {(int)response.StatusCode}.");
    }
}
