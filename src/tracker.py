"""
Lead Tracker Module (CRM-lite)

Track leads through the outreach pipeline:
new -> contacted -> replied -> meeting -> converted
                                        -> rejected
                                        -> dormant
"""

import json
from datetime import datetime
from pathlib import Path

from rich.console import Console
from rich.table import Table
from rich.panel import Panel

from .config_loader import get_data_dir, load_target_markets

console = Console()

STATUSES = ["new", "contacted", "replied", "meeting", "converted", "rejected", "dormant"]


def _load_leads() -> list:
    filepath = get_data_dir() / "leads.json"
    if not filepath.exists():
        return []
    with open(filepath, "r") as f:
        return json.load(f)


def _save_leads(leads: list):
    filepath = get_data_dir() / "leads.json"
    with open(filepath, "w") as f:
        json.dump(leads, f, indent=2, default=str)


def add_lead_interactive():
    """Add a new lead interactively."""
    markets_data = load_target_markets()
    market_ids = [m["id"] for m in markets_data["target_markets"]]
    market_names = {m["id"]: m["name"] for m in markets_data["target_markets"]}

    console.print("\n[bold cyan]Add New Lead[/bold cyan]\n")

    name = input("Business/Client Name: ").strip()
    if not name:
        console.print("[red]Name is required.[/red]")
        return

    console.print("\nAvailable markets:")
    for i, mid in enumerate(market_ids, 1):
        console.print(f"  {i}. {market_names[mid]} ({mid})")

    try:
        market_choice = int(input("\nSelect market (number): ").strip()) - 1
        market_id = market_ids[market_choice]
    except (ValueError, IndexError):
        console.print("[red]Invalid choice. Using first market.[/red]")
        market_id = market_ids[0]

    contact_name = input("Contact Person Name (optional): ").strip()
    contact_email = input("Contact Email (optional): ").strip()
    contact_phone = input("Contact Phone (optional): ").strip()
    website = input("Website (optional): ").strip()
    source = input("Where did you find them? ").strip()
    notes = input("Notes (optional): ").strip()

    contact_method = "email" if contact_email else "phone" if contact_phone else "other"
    contact_info = contact_email or contact_phone or ""

    lead = {
        "name": name,
        "contact_person": contact_name,
        "market_id": market_id,
        "contact_method": contact_method,
        "contact_info": contact_info,
        "contact_phone": contact_phone,
        "contact_email": contact_email,
        "website": website,
        "notes": notes,
        "source": source,
        "has_budget": False,
        "budget_unknown": True,
        "easy_to_reach": bool(contact_email),
        "contact_available": bool(contact_info),
        "created_at": datetime.now().isoformat(),
        "status": "new",
        "history": [
            {
                "date": datetime.now().isoformat(),
                "action": "created",
                "notes": f"Added from {source}" if source else "Added manually",
            }
        ],
    }

    leads = _load_leads()
    leads.append(lead)
    _save_leads(leads)

    console.print(f"\n[green]Lead '{name}' added successfully![/green]")
    console.print(f"[dim]Market: {market_names[market_id]}[/dim]")
    console.print(f"[dim]Total leads: {len(leads)}[/dim]")


def update_lead_status(lead_index: int = None):
    """Update a lead's status."""
    leads = _load_leads()
    if not leads:
        console.print("[yellow]No leads found.[/yellow]")
        return

    if lead_index is None:
        display_leads_compact(leads)
        try:
            lead_index = int(input("\nSelect lead number to update: ").strip()) - 1
        except ValueError:
            console.print("[red]Invalid number.[/red]")
            return

    if lead_index < 0 or lead_index >= len(leads):
        console.print("[red]Invalid lead number.[/red]")
        return

    lead = leads[lead_index]
    console.print(f"\nCurrent status of '{lead['name']}': [bold]{lead['status']}[/bold]")
    console.print("\nAvailable statuses:")
    for i, status in enumerate(STATUSES, 1):
        console.print(f"  {i}. {status}")

    try:
        status_choice = int(input("\nNew status (number): ").strip()) - 1
        new_status = STATUSES[status_choice]
    except (ValueError, IndexError):
        console.print("[red]Invalid choice.[/red]")
        return

    notes = input("Notes about this update (optional): ").strip()

    lead["status"] = new_status
    if "history" not in lead:
        lead["history"] = []
    lead["history"].append(
        {
            "date": datetime.now().isoformat(),
            "action": f"status changed to {new_status}",
            "notes": notes,
        }
    )

    _save_leads(leads)
    console.print(f"\n[green]'{lead['name']}' updated to '{new_status}'[/green]")


def log_activity(lead_index: int = None):
    """Log an outreach activity for a lead."""
    leads = _load_leads()
    if not leads:
        console.print("[yellow]No leads found.[/yellow]")
        return

    if lead_index is None:
        display_leads_compact(leads)
        try:
            lead_index = int(input("\nSelect lead number: ").strip()) - 1
        except ValueError:
            console.print("[red]Invalid number.[/red]")
            return

    if lead_index < 0 or lead_index >= len(leads):
        console.print("[red]Invalid lead number.[/red]")
        return

    lead = leads[lead_index]
    console.print(f"\nLogging activity for: [bold]{lead['name']}[/bold]")

    activities = ["email_sent", "called", "meeting", "follow_up", "proposal_sent", "other"]
    console.print("\nActivity types:")
    for i, act in enumerate(activities, 1):
        console.print(f"  {i}. {act}")

    try:
        act_choice = int(input("\nActivity type (number): ").strip()) - 1
        activity = activities[act_choice]
    except (ValueError, IndexError):
        activity = "other"

    notes = input("Notes: ").strip()

    if "history" not in lead:
        lead["history"] = []
    lead["history"].append(
        {
            "date": datetime.now().isoformat(),
            "action": activity,
            "notes": notes,
        }
    )

    if activity == "email_sent" and lead["status"] == "new":
        lead["status"] = "contacted"

    _save_leads(leads)
    console.print(f"\n[green]Activity logged for '{lead['name']}'[/green]")


def display_leads_compact(leads: list = None):
    """Display leads in a compact list format."""
    if leads is None:
        leads = _load_leads()

    if not leads:
        console.print("[yellow]No leads yet.[/yellow]")
        return

    for i, lead in enumerate(leads, 1):
        status_icon = {
            "new": "⚪", "contacted": "🟡", "replied": "🟢",
            "meeting": "🔵", "converted": "✅", "rejected": "🔴", "dormant": "⚫",
        }.get(lead.get("status", "new"), "⚪")
        console.print(
            f"  {i}. {status_icon} {lead['name']} [{lead.get('status', 'new')}] "
            f"— {lead.get('contact_info', 'no contact')}"
        )


def display_pipeline():
    """Display the full pipeline view of all leads."""
    leads = _load_leads()
    if not leads:
        console.print("\n[yellow]No leads in pipeline. Add leads using 'add-lead' command.[/yellow]")
        return

    console.print()
    console.print(
        Panel(
            "[bold]Lead Pipeline[/bold]\n"
            "Track your leads from discovery to conversion.",
            style="cyan",
        )
    )

    pipeline = {status: [] for status in STATUSES}
    for lead in leads:
        status = lead.get("status", "new")
        if status in pipeline:
            pipeline[status].append(lead)

    for status in STATUSES:
        status_leads = pipeline[status]
        if not status_leads:
            continue

        color = {
            "new": "white", "contacted": "yellow", "replied": "green",
            "meeting": "cyan", "converted": "bold green", "rejected": "red", "dormant": "dim",
        }.get(status, "white")

        console.print(f"\n[{color}]━━━ {status.upper()} ({len(status_leads)}) ━━━[/{color}]")
        for lead in status_leads:
            last_activity = ""
            if lead.get("history"):
                last = lead["history"][-1]
                last_activity = f" | Last: {last['action']} ({last['date'][:10]})"
            console.print(
                f"  • {lead['name']} — {lead.get('contact_info', 'no contact')}{last_activity}"
            )

    total = len(leads)
    converted = len(pipeline["converted"])
    active = total - len(pipeline["rejected"]) - len(pipeline["dormant"])
    console.print(f"\n[dim]Total: {total} | Active: {active} | Converted: {converted}[/dim]")


def display_lead_detail(lead_index: int = None):
    """Show detailed information about a specific lead."""
    leads = _load_leads()
    if not leads:
        console.print("[yellow]No leads found.[/yellow]")
        return

    if lead_index is None:
        display_leads_compact(leads)
        try:
            lead_index = int(input("\nSelect lead number for details: ").strip()) - 1
        except ValueError:
            console.print("[red]Invalid number.[/red]")
            return

    if lead_index < 0 or lead_index >= len(leads):
        console.print("[red]Invalid lead number.[/red]")
        return

    lead = leads[lead_index]
    markets_data = load_target_markets()
    market = next(
        (m for m in markets_data["target_markets"] if m["id"] == lead.get("market_id")),
        {},
    )

    table = Table(
        title=f"\n{lead['name']}", show_header=False, border_style="cyan", width=80
    )
    table.add_column("Field", style="bold cyan", width=18)
    table.add_column("Value", width=60)

    table.add_row("Status", lead.get("status", "new"))
    table.add_row("Market", market.get("name", lead.get("market_id", "unknown")))
    table.add_row("Contact Person", lead.get("contact_person", "—"))
    table.add_row("Email", lead.get("contact_email", "—"))
    table.add_row("Phone", lead.get("contact_phone", "—"))
    table.add_row("Website", lead.get("website", "—"))
    table.add_row("Source", lead.get("source", "—"))
    table.add_row("Notes", lead.get("notes", "—"))
    table.add_row("Created", lead.get("created_at", "—")[:10])

    console.print(table)

    if lead.get("history"):
        console.print("\n[bold]Activity History:[/bold]")
        for entry in lead["history"]:
            console.print(
                f"  [{entry['date'][:10]}] {entry['action']}"
                + (f" — {entry['notes']}" if entry.get("notes") else "")
            )

    if market:
        console.print(f"\n[bold]Suggested approach:[/bold] {market.get('approach', '—')}")
        console.print(f"[bold]Budget range:[/bold] {market.get('budget_range', '—')}")
