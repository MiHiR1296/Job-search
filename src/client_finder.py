"""
Client Finder Module

Identifies potential clients from target markets, scores them based on
fit with your services, and generates actionable lead lists.
"""

import json
from datetime import datetime
from pathlib import Path

from rich.console import Console
from rich.table import Table
from rich.panel import Panel
from rich.columns import Columns

from .config_loader import load_profile, load_target_markets, get_data_dir

console = Console()


def get_service_map(profile: dict) -> dict:
    return {s["id"]: s for s in profile["services"]}


def score_lead(lead: dict, market: dict, profile: dict) -> int:
    """
    Score a potential lead from 0-100 based on fit.

    Scoring factors:
    - Market priority (high=30, medium=20, low=10)
    - Number of matching services (up to 30 points)
    - Budget alignment (up to 20 points)
    - Accessibility (up to 20 points)
    """
    score = 0

    priority_scores = {"high": 30, "medium": 20, "low": 10}
    score += priority_scores.get(market.get("priority", "low"), 10)

    matching_services = len(market.get("services_match", []))
    score += min(matching_services * 10, 30)

    if lead.get("has_budget"):
        score += 20
    elif lead.get("budget_unknown"):
        score += 10

    if lead.get("easy_to_reach"):
        score += 20
    elif lead.get("contact_available"):
        score += 10

    return min(score, 100)


def create_lead(
    name: str,
    market_id: str,
    contact_method: str = "",
    contact_info: str = "",
    notes: str = "",
    website: str = "",
    has_budget: bool = False,
    budget_unknown: bool = True,
    easy_to_reach: bool = False,
    contact_available: bool = True,
    source: str = "",
) -> dict:
    return {
        "name": name,
        "market_id": market_id,
        "contact_method": contact_method,
        "contact_info": contact_info,
        "notes": notes,
        "website": website,
        "has_budget": has_budget,
        "budget_unknown": budget_unknown,
        "easy_to_reach": easy_to_reach,
        "contact_available": contact_available,
        "source": source,
        "created_at": datetime.now().isoformat(),
        "status": "new",
    }


def save_leads(leads: list, filename: str = "leads.json"):
    filepath = get_data_dir() / filename
    existing = []
    if filepath.exists():
        with open(filepath, "r") as f:
            existing = json.load(f)

    existing.extend(leads)

    with open(filepath, "w") as f:
        json.dump(existing, f, indent=2, default=str)

    return filepath


def load_leads(filename: str = "leads.json") -> list:
    filepath = get_data_dir() / filename
    if not filepath.exists():
        return []
    with open(filepath, "r") as f:
        return json.load(f)


def display_markets():
    """Display all target markets with details."""
    markets_data = load_target_markets()
    profile = load_profile()
    service_map = get_service_map(profile)

    console.print()
    console.print(
        Panel(
            "[bold]Target Markets for Your 3D Services[/bold]\n"
            "These are industries and business types that would benefit most from your skills.",
            style="cyan",
        )
    )

    for market in markets_data["target_markets"]:
        priority_colors = {"high": "green", "medium": "yellow", "low": "red"}
        color = priority_colors.get(market["priority"], "white")

        table = Table(
            title=f"\n{market['name']}",
            title_style="bold",
            show_header=False,
            border_style=color,
            width=90,
        )
        table.add_column("Field", style="bold cyan", width=20)
        table.add_column("Details", width=68)

        table.add_row("Priority", f"[{color}]{market['priority'].upper()}[/{color}]")
        table.add_row("Why Target", market["why"])
        table.add_row("Budget Range", market.get("budget_range", "Varies"))
        table.add_row(
            "Services to Offer",
            ", ".join(
                service_map[s]["name"]
                for s in market["services_match"]
                if s in service_map
            ),
        )
        table.add_row("Pain Points", "\n".join(f"• {p}" for p in market["pain_points"]))
        table.add_row(
            "Where to Find",
            "\n".join(f"• {w}" for w in market["where_to_find"]),
        )
        table.add_row("Approach Strategy", market["approach"])

        console.print(table)


def display_leads():
    """Display all saved leads."""
    leads = load_leads()
    markets_data = load_target_markets()
    profile = load_profile()

    market_map = {m["id"]: m for m in markets_data["target_markets"]}

    if not leads:
        console.print("\n[yellow]No leads saved yet. Add leads using 'add-lead' command.[/yellow]")
        return

    table = Table(title="\nYour Leads", border_style="cyan")
    table.add_column("#", style="dim", width=4)
    table.add_column("Name", style="bold", width=25)
    table.add_column("Market", width=20)
    table.add_column("Score", width=8, justify="center")
    table.add_column("Status", width=12)
    table.add_column("Contact", width=25)
    table.add_column("Source", width=15)

    for i, lead in enumerate(leads, 1):
        market = market_map.get(lead["market_id"], {})
        score = score_lead(lead, market, profile)

        score_color = "green" if score >= 70 else "yellow" if score >= 40 else "red"
        status_colors = {
            "new": "white",
            "contacted": "yellow",
            "replied": "green",
            "meeting": "cyan",
            "converted": "bold green",
            "rejected": "red",
            "dormant": "dim",
        }
        status_color = status_colors.get(lead.get("status", "new"), "white")

        table.add_row(
            str(i),
            lead["name"],
            market.get("name", lead["market_id"])[:20],
            f"[{score_color}]{score}[/{score_color}]",
            f"[{status_color}]{lead.get('status', 'new')}[/{status_color}]",
            lead.get("contact_info", "")[:25] or lead.get("contact_method", ""),
            lead.get("source", "")[:15],
        )

    console.print(table)
    console.print(f"\n[dim]Total leads: {len(leads)}[/dim]")


def generate_prospecting_checklist():
    """Generate a daily/weekly prospecting checklist based on target markets."""
    markets_data = load_target_markets()

    console.print()
    console.print(
        Panel(
            "[bold]Weekly Prospecting Checklist[/bold]\n"
            "Follow this checklist each week to consistently find new leads.",
            style="green",
        )
    )

    high_priority = [m for m in markets_data["target_markets"] if m["priority"] == "high"]
    medium_priority = [m for m in markets_data["target_markets"] if m["priority"] == "medium"]

    console.print("\n[bold green]HIGH PRIORITY (do these first):[/bold green]")
    for market in high_priority:
        console.print(f"\n  [bold]{market['name']}[/bold]")
        for place in market["where_to_find"][:3]:
            console.print(f"    [ ] Search: {place}")
        console.print(f"    [ ] Add 2-3 leads from this market")
        console.print(f"    [ ] Send outreach to new leads")

    console.print("\n[bold yellow]MEDIUM PRIORITY (do these if time permits):[/bold yellow]")
    for market in medium_priority:
        console.print(f"\n  [bold]{market['name']}[/bold]")
        for place in market["where_to_find"][:2]:
            console.print(f"    [ ] Search: {place}")
        console.print(f"    [ ] Add 1-2 leads from this market")

    console.print(
        "\n[bold cyan]DAILY TASKS:[/bold cyan]"
        "\n  [ ] Check email for responses"
        "\n  [ ] Follow up on leads contacted 3+ days ago"
        "\n  [ ] Post one portfolio piece on Instagram/LinkedIn"
        "\n  [ ] Engage with 5 potential client posts on social media"
    )
