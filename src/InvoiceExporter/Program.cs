using InvoiceExporter.Clients;
using InvoiceExporter.Data;
using InvoiceExporter.Export;
using InvoiceExporter.Services;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

var builder = Host.CreateApplicationBuilder(args);

var options = ExportOptions.FromEnvironment();
builder.Services.AddSingleton(options);

var connectionString = Environment.GetEnvironmentVariable("EXPORT_DB_CONNECTION_STRING")
    ?? throw new InvalidOperationException("EXPORT_DB_CONNECTION_STRING is required.");
builder.Services.AddSingleton<IInvoiceRepository>(new InvoiceRepository(connectionString));

builder.Services.AddHttpClient<IAccountingApiClient, AccountingApiClient>(client =>
{
    client.BaseAddress = new Uri(options.AccountingApiUrl);
});

builder.Services.AddHttpClient();
builder.Services.AddSingleton<IExportNotifier>(sp =>
{
    var httpClientFactory = sp.GetRequiredService<IHttpClientFactory>();
    return new WebhookExportNotifier(httpClientFactory.CreateClient(), options.WebhookUrl);
});

builder.Services.AddSingleton<ICsvExportWriter, CsvExportWriter>();
builder.Services.AddHostedService<InvoiceExportWorker>();

Directory.CreateDirectory(options.OutputDirectory);

var host = builder.Build();
await host.RunAsync();
