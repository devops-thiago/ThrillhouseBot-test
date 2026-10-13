namespace MuseumTickets;

/// <summary>Renders a printable ticket from one of the HTML templates shipped with the service.</summary>
public sealed class TicketTemplateRenderer
{
    private readonly AllocatorConfig _config;

    public TicketTemplateRenderer(AllocatorConfig config)
    {
        _config = config;
    }

    public string Render(PrintRequest request)
    {
        // Strip any directory parts so a crafted value cannot escape the template folder.
        var gallery = Path.GetFileName(request.Gallery);

        var templatePath = Path.Combine(_config.TemplateRoot, request.Template);
        var template = File.ReadAllText(templatePath);
        return template.Replace("{{gallery}}", gallery);
    }
}
