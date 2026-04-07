# Client Outreach System for 3D Freelancers

A CLI tool built for **Mihir Botle** (and adaptable by any freelance 3D artist) to systematically find potential clients, generate personalized outreach materials, and track leads through a pipeline — from discovery to conversion.

## The Problem

You're a skilled 3D artist who can make product renders, architectural visualizations, explainer videos, interactive web experiences, and marketing content. But finding the *right* clients — small businesses who genuinely need your services and can afford them — is a different skill entirely.

This system gives you a structured, repeatable process for client acquisition instead of hoping clients find you.

## What It Does

### 1. Target Market Intelligence (`markets`)
Shows you **8 carefully researched market segments** where your skills are most needed, including:
- Why each market is a good fit
- Their specific pain points you can solve
- Where to find these businesses (specific platforms and methods)
- Recommended approach strategy
- Budget ranges to expect

### 2. Prospecting Checklist (`checklist`)
Generates a **weekly action checklist** prioritized by market potential, so you always know what to do next.

### 3. Lead Management (`add-lead`, `leads`, `pipeline`)
A lightweight CRM to track every potential client through the pipeline:
```
new → contacted → replied → meeting → converted
                                    → rejected
                                    → dormant
```

### 4. Email Templates (`email-templates`, `generate-email`)
Pre-written, customizable email templates for every stage:
- **Cold Introduction** — First contact with a potential client
- **Follow-up 1** — 3-4 days later if no response
- **Follow-up 2** — Final gentle nudge after a week
- **Referral Ask** — After completing a project successfully

Each template automatically fills in your portfolio links, service details, and the client's specific pain points.

### 5. Call Scripts (`call-scripts`, `generate-script`)
Conversation guides for phone outreach:
- **Cold Call Introduction** — Concise pitch for first contact
- **Discovery Call** — Deeper conversation when they show interest
- **Objection Handling** — Ready responses for common pushbacks ("too expensive", "we have a photographer", etc.)

### 6. Service Portfolio (`services`, `profile`)
Your complete service catalog with descriptions, deliverables, and ideal client profiles — all configured in one place.

## Quick Start

### Install dependencies
```bash
pip install -r requirements.txt
```

### See all commands
```bash
python3 -m src.cli help
```

### Explore your target markets
```bash
python3 -m src.cli markets
```

### Get your weekly prospecting checklist
```bash
python3 -m src.cli checklist
```

### Preview an outreach email
```bash
python3 -m src.cli preview-email cold_intro
```

### Preview a call script
```bash
python3 -m src.cli preview-script cold_call_intro
```

### Add a lead you found
```bash
python3 -m src.cli add-lead
```

### View your pipeline
```bash
python3 -m src.cli pipeline
```

### Generate a personalized email for a specific lead
```bash
python3 -m src.cli generate-email
```

## All Commands

| Command | What It Does |
|---|---|
| `markets` | Show all target markets with detailed strategies |
| `checklist` | Generate weekly prospecting checklist |
| `add-lead` | Add a new potential client interactively |
| `leads` | Show all leads with fit scores |
| `pipeline` | Show leads organized by status (CRM view) |
| `lead-detail` | Show full details + history for a lead |
| `update-status` | Move a lead to a new status |
| `log-activity` | Log an email sent, call made, meeting, etc. |
| `email-templates` | List available email templates |
| `call-scripts` | List available call scripts |
| `preview-email <id>` | Preview an email template with sample data |
| `preview-script <id>` | Preview a call script with sample data |
| `generate-email` | Generate a personalized email for a real lead |
| `generate-script` | Generate a personalized call script for a lead |
| `services` | Show your service offerings |
| `profile` | Show your profile summary |

## Project Structure

```
├── config/
│   ├── profile.yaml          # Your info, services, portfolio, skills
│   └── target_markets.yaml   # Target industries and strategies
├── src/
│   ├── cli.py                # Main CLI entry point
│   ├── config_loader.py      # YAML config loading
│   ├── client_finder.py      # Market intelligence & lead scoring
│   ├── outreach.py           # Email templates & call scripts
│   └── tracker.py            # Lead tracking (CRM-lite)
├── data/
│   └── leads.json            # Your leads (auto-created)
├── templates/                # For future custom templates
├── requirements.txt
└── README.md
```

## Target Markets (Built-in)

| Market | Priority | Why |
|---|---|---|
| E-commerce & D2C Brands | HIGH | Need product visuals constantly, can't afford full-time artists |
| Real Estate & Developers | HIGH | Need to sell properties before construction, 3D makes it possible |
| Interior Designers & Architects | HIGH | Small firms can't afford in-house 3D but need renders to close deals |
| Jewelry & Fashion Brands | HIGH | Your jewelry configurator is a perfect portfolio piece |
| Tech Startups & SaaS | MEDIUM | Need explainer videos and marketing visuals |
| Manufacturing & Industrial | MEDIUM | Have products but poor digital presence |
| Marketing Agencies | MEDIUM | Need freelance 3D talent for client projects |
| Restaurants & Hospitality | LOW | Growing businesses needing better online presence |

## Customization

### Edit your profile
Update `config/profile.yaml` with your details, services, and portfolio links.

### Add or modify target markets
Edit `config/target_markets.yaml` to add new industries or adjust strategies.

### Extend email templates
Add new templates in `src/outreach.py` following the existing Jinja2 template format.

## Future Ideas

- [ ] Google Maps API integration to auto-find businesses
- [ ] LinkedIn scraper for lead discovery
- [ ] Auto-send emails via Gmail/SMTP
- [ ] Web dashboard for visual pipeline tracking
- [ ] Portfolio website generator
- [ ] Invoice and proposal generation
- [ ] Calendar integration for meeting scheduling
- [ ] Analytics on outreach performance (open rates, reply rates)

## Built For

This system is designed for freelance 3D artists targeting **small businesses** — the ones that would genuinely benefit from professional 3D work but don't have their own team. These are the clients where your skills create the most value and where relationships are built on trust rather than corporate procurement.
