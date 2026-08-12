using CertWatch;

const string EmailGatewayBaseUrl = "http://email-gateway.svc.internal";

var caBaseUrl = RequireEnv("CERTWATCH_CA_BASE_URL");
var dbConnectionString = RequireEnv("CERTWATCH_DB_CONNECTION_STRING");
var thresholdDays = ParseThresholdDays(Environment.GetEnvironmentVariable("CERTWATCH_EXPIRY_THRESHOLD_DAYS"));
var alertEmails = ParseAlertEmails(Environment.GetEnvironmentVariable("CERTWATCH_ALERT_EMAILS"));

using var caHttpClient = new HttpClient { BaseAddress = new Uri(caBaseUrl) };
using var emailHttpClient = new HttpClient { BaseAddress = new Uri(EmailGatewayBaseUrl) };

var caApiClient = new CaApiClient(caHttpClient);
var certificateService = new CertificateService(caApiClient);
var expiryAnalyzer = new ExpiryAnalyzer();
var ownerLookup = new OwnerLookupRepository(dbConnectionString);
var emailClient = new EmailGatewayClient(emailHttpClient);
var notificationSender = new NotificationSender(ownerLookup, emailClient);

var certificates = await certificateService.FetchAllCertificatesAsync();
Console.WriteLine($"Loaded {certificates.Count} certificates from the CA inventory.");

var duplicates = expiryAnalyzer.DetectDuplicateCommonNames(certificates);
if (duplicates.Count > 0)
{
    Console.WriteLine($"Found {duplicates.Count} certificates with a duplicate common name.");
}

var expiringSoon = expiryAnalyzer.GetCertificatesExpiringWithin(certificates, thresholdDays);
Console.WriteLine($"{expiringSoon.Count} certificates expire within {thresholdDays} days.");

var failedNotifications = await notificationSender.SendExpiryNotificationsAsync(expiringSoon);

if (failedNotifications.Count > 0)
{
    Console.Error.WriteLine(
        $"Warning: {failedNotifications.Count} expiry notifications failed to send: " +
        string.Join(", ", failedNotifications));

    foreach (var operatorEmail in alertEmails)
    {
        await emailClient.SendAsync(
            operatorEmail,
            "CertWatch notification failures",
            $"{failedNotifications.Count} certificate expiry notifications failed to send: " +
            string.Join(", ", failedNotifications));
    }

    return 1;
}

return 0;

static string RequireEnv(string name) =>
    Environment.GetEnvironmentVariable(name)
        ?? throw new InvalidOperationException($"Missing required environment variable '{name}'.");

static int ParseThresholdDays(string? raw) =>
    raw is null ? 30 : int.Parse(raw);

static string[] ParseAlertEmails(string? raw) =>
    raw is null
        ? Array.Empty<string>()
        : raw.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
