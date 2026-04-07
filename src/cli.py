"""
Client Outreach System — CLI Entry Point

A tool for 3D artists and freelancers to find potential clients,
generate outreach materials, and track leads through the pipeline.
"""

import sys
from rich.console import Console
from rich.panel import Panel
from rich.table import Table

from . import client_finder, outreach, tracker

console = Console()

BANNER = """
 ██████╗██╗     ██╗███████╗███╗   ██╗████████╗
██╔════╝██║     ██║██╔════╝████╗  ██║╚══██╔══╝
██║     ██║     ██║█████╗  ██╔██╗ ██║   ██║   
██║     ██║     ██║██╔══╝  ██║╚██╗██║   ██║   
╚██████╗███████╗██║███████╗██║ ╚████║   ██║   
 ╚═════╝╚══════╝╚═╝╚══════╝╚═╝  ╚═══╝   ╚═╝   

 ██████╗ ██╗   ██╗████████╗██████╗ ███████╗ █████╗  ██████╗██╗  ██╗
██╔═══██╗██║   ██║╚══██╔══╝██╔══██╗██╔════╝██╔══██╗██╔════╝██║  ██║
██║   ██║██║   ██║   ██║   ██████╔╝█████╗  ███████║██║     ███████║
██║   ██║██║   ██║   ██║   ██╔══██╗██╔══╝  ██╔══██║██║     ██╔══██║
╚██████╔╝╚██████╔╝   ██║   ██║  ██║███████╗██║  ██║╚██████╗██║  ██║
 ╚═════╝  ╚═════╝    ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝
"""


def show_help():
    console.print(Panel(BANNER, style="cyan", subtitle="Freelance Client Outreach System"))

    table = Table(title="Available Commands", border_style="cyan", width=80)
    table.add_column("Command", style="bold green", width=25)
    table.add_column("Description", width=53)

    commands = [
        ("markets", "Show all target markets with strategies"),
        ("checklist", "Generate weekly prospecting checklist"),
        ("add-lead", "Add a new potential client/lead"),
        ("leads", "Show all leads with scores"),
        ("pipeline", "Show leads organized by status"),
        ("lead-detail", "Show detailed info about a lead"),
        ("update-status", "Update a lead's status"),
        ("log-activity", "Log an outreach activity for a lead"),
        ("email-templates", "List available email templates"),
        ("call-scripts", "List available call scripts"),
        ("preview-email <template_id>", "Preview an email template"),
        ("preview-script <script_id>", "Preview a call script"),
        ("generate-email", "Generate a personalized email for a lead"),
        ("generate-script", "Generate a personalized call script"),
        ("services", "Show your service offerings"),
        ("profile", "Show your profile summary"),
        ("help", "Show this help message"),
    ]

    for cmd, desc in commands:
        table.add_row(cmd, desc)

    console.print(table)
    console.print(
        "\n[dim]Usage: python -m src.cli <command>[/dim]"
        "\n[dim]Example: python -m src.cli markets[/dim]"
        "\n[dim]Example: python -m src.cli generate-email[/dim]"
    )


def show_services():
    from .config_loader import load_profile

    profile = load_profile()

    console.print()
    console.print(Panel("[bold]Your Service Offerings[/bold]", style="cyan"))

    for service in profile["services"]:
        table = Table(
            title=f"\n{service['name']}",
            show_header=False,
            border_style="green",
            width=80,
        )
        table.add_column("Field", style="bold cyan", width=16)
        table.add_column("Details", width=62)

        table.add_row("ID", service["id"])
        table.add_row("Description", service["description"])
        table.add_row("Ideal For", ", ".join(service["ideal_for"]))
        table.add_row("Deliverables", "\n".join(f"• {d}" for d in service["deliverables"]))

        console.print(table)


def show_profile():
    from .config_loader import load_profile

    profile = load_profile()

    console.print()
    table = Table(title="Your Profile", show_header=False, border_style="cyan", width=80)
    table.add_column("Field", style="bold cyan", width=16)
    table.add_column("Value", width=62)

    table.add_row("Name", profile["name"])
    table.add_row("Title", profile["title"])
    table.add_row("Email", profile["email"])
    table.add_row("Phone", ", ".join(profile["phone"]))
    table.add_row("Location", profile["location"])
    table.add_row("Experience", f"{profile['years_experience']}+ years")
    table.add_row("Behance", profile["portfolio"]["behance"])
    table.add_row("GitHub", profile["portfolio"]["github"])
    table.add_row("LinkedIn", profile["portfolio"]["linkedin"])
    table.add_row("Live Demo", profile["portfolio"]["live_demo"])

    console.print(table)

    console.print("\n[bold]Key Highlights:[/bold]")
    for h in profile["highlights"]:
        console.print(f"  • {h}")

    console.print(f"\n[bold]Services ({len(profile['services'])}):[/bold]")
    for s in profile["services"]:
        console.print(f"  • {s['name']}")


def generate_email_interactive():
    """Interactive email generation for a lead."""
    from .config_loader import load_profile, load_target_markets

    leads = tracker._load_leads()
    if not leads:
        console.print("[yellow]No leads found. Add a lead first with 'add-lead'.[/yellow]")
        return

    tracker.display_leads_compact(leads)
    try:
        idx = int(input("\nSelect lead number: ").strip()) - 1
    except ValueError:
        console.print("[red]Invalid number.[/red]")
        return

    if idx < 0 or idx >= len(leads):
        console.print("[red]Invalid lead number.[/red]")
        return

    lead = leads[idx]
    profile = load_profile()

    console.print("\nAvailable email templates:")
    for i, (tid, tdata) in enumerate(outreach.EMAIL_TEMPLATES.items(), 1):
        console.print(f"  {i}. {tdata['name']} ({tid})")

    try:
        tmpl_choice = int(input("\nSelect template (number): ").strip()) - 1
        template_id = list(outreach.EMAIL_TEMPLATES.keys())[tmpl_choice]
    except (ValueError, IndexError):
        template_id = "cold_intro"

    console.print("\nAvailable services:")
    for i, s in enumerate(profile["services"], 1):
        console.print(f"  {i}. {s['name']}")

    try:
        svc_choice = int(input("\nSelect service to pitch (number): ").strip()) - 1
        service_id = profile["services"][svc_choice]["id"]
    except (ValueError, IndexError):
        service_id = profile["services"][0]["id"]

    compliment = input("\nWhat impressed you about them? (optional): ").strip()
    free_offer = input("Free offer to include? (optional, e.g. 'create a free render of your product'): ").strip()

    custom_vars = {}
    if compliment:
        custom_vars["compliment"] = compliment
    if free_offer:
        custom_vars["free_offer"] = free_offer
    if lead.get("source"):
        custom_vars["source"] = lead["source"]

    email = outreach.generate_email(
        template_id,
        lead["name"],
        lead.get("contact_person", lead["name"]),
        lead["market_id"],
        service_id,
        custom_vars,
    )

    console.print()
    console.print(Panel(email, title="Generated Email", border_style="cyan"))
    console.print("\n[dim]Copy this email and personalize further before sending.[/dim]")


def generate_script_interactive():
    """Interactive call script generation for a lead."""
    from .config_loader import load_profile

    leads = tracker._load_leads()
    if not leads:
        console.print("[yellow]No leads found. Add a lead first with 'add-lead'.[/yellow]")
        return

    tracker.display_leads_compact(leads)
    try:
        idx = int(input("\nSelect lead number: ").strip()) - 1
    except ValueError:
        console.print("[red]Invalid number.[/red]")
        return

    if idx < 0 or idx >= len(leads):
        console.print("[red]Invalid lead number.[/red]")
        return

    lead = leads[idx]
    profile = load_profile()

    console.print("\nAvailable call scripts:")
    for i, (sid, sdata) in enumerate(outreach.CALL_SCRIPTS.items(), 1):
        console.print(f"  {i}. {sdata['name']} ({sid})")

    try:
        script_choice = int(input("\nSelect script (number): ").strip()) - 1
        script_id = list(outreach.CALL_SCRIPTS.keys())[script_choice]
    except (ValueError, IndexError):
        script_id = "cold_call_intro"

    console.print("\nAvailable services:")
    for i, s in enumerate(profile["services"], 1):
        console.print(f"  {i}. {s['name']}")

    try:
        svc_choice = int(input("\nSelect service (number): ").strip()) - 1
        service_id = profile["services"][svc_choice]["id"]
    except (ValueError, IndexError):
        service_id = profile["services"][0]["id"]

    script = outreach.generate_call_script(
        script_id,
        lead["name"],
        lead.get("contact_person", lead["name"]),
        lead["market_id"],
        service_id,
    )

    console.print()
    console.print(Panel(script, title="Call Script", border_style="green"))


def main():
    if len(sys.argv) < 2:
        show_help()
        return

    command = sys.argv[1].lower()

    if command == "help":
        show_help()
    elif command == "markets":
        client_finder.display_markets()
    elif command == "checklist":
        client_finder.generate_prospecting_checklist()
    elif command == "add-lead":
        tracker.add_lead_interactive()
    elif command == "leads":
        client_finder.display_leads()
    elif command == "pipeline":
        tracker.display_pipeline()
    elif command == "lead-detail":
        tracker.display_lead_detail()
    elif command == "update-status":
        tracker.update_lead_status()
    elif command == "log-activity":
        tracker.log_activity()
    elif command == "email-templates":
        outreach.display_email_templates()
    elif command == "call-scripts":
        outreach.display_call_scripts()
    elif command == "preview-email":
        template_id = sys.argv[2] if len(sys.argv) > 2 else "cold_intro"
        outreach.preview_email(template_id)
    elif command == "preview-script":
        script_id = sys.argv[2] if len(sys.argv) > 2 else "cold_call_intro"
        outreach.preview_call_script(script_id)
    elif command == "generate-email":
        generate_email_interactive()
    elif command == "generate-script":
        generate_script_interactive()
    elif command == "services":
        show_services()
    elif command == "profile":
        show_profile()
    else:
        console.print(f"[red]Unknown command: {command}[/red]")
        console.print("[dim]Run 'python -m src.cli help' to see available commands.[/dim]")


if __name__ == "__main__":
    main()
