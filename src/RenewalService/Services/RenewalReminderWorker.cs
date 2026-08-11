using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace RenewalService.Services;

public sealed class RenewalReminderWorker : BackgroundService
{
    private readonly RenewalProcessor _processor;
    private readonly RenewalOptions _options;
    private readonly ILogger<RenewalReminderWorker> _logger;

    public RenewalReminderWorker(RenewalProcessor processor, RenewalOptions options, ILogger<RenewalReminderWorker> logger)
    {
        _processor = processor;
        _options = options;
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        using var timer = new PeriodicTimer(TimeSpan.FromSeconds(_options.CheckIntervalSeconds));

        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            try
            {
                var summary = await _processor.ProcessAsync(stoppingToken);
                _logger.LogInformation(
                    "Renewal run complete: processed {Processed}, failed {Failed}.",
                    summary.ProcessedCount,
                    summary.FailedCount);
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                _logger.LogError(ex, "Renewal run failed unexpectedly.");
            }
        }
    }
}
