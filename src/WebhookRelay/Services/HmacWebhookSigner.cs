using System.Security.Cryptography;
using System.Text;

namespace WebhookRelay.Services;

public class HmacWebhookSigner : IWebhookSigner
{
    public string Sign(string payload, string secret)
    {
        if (string.IsNullOrEmpty(secret))
        {
            throw new ArgumentException("A webhook secret is required to sign a delivery.", nameof(secret));
        }

        using var hmac = new HMACSHA256(Encoding.UTF8.GetBytes(secret));
        var hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(payload));
        return Convert.ToHexString(hash).ToLowerInvariant();
    }
}
