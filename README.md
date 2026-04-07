# Freelance Client Outreach System

A lightweight web app built for **Mihir Botle** (3D Generalist & Technical Artist) to find, score, and approach small-business clients for freelance work.

## What it does

| Feature | Details |
|---|---|
| **Find Clients** | Searches for small businesses via Google Places API (or uses built-in demo data — no key needed to try it out) |
| **Smart Scoring** | Each business is scored 0–100 based on how well it matches your 5 services |
| **Personalised Outreach** | Generates a cold email + phone call script tailored to each business, using GPT-4o-mini (or polished templates if no OpenAI key) |
| **Lead Tracker** | Track every lead through: New → Emailed → Called → Replied → Converted |
| **Dashboard** | Live stats and top leads at a glance |

## Services covered

1. 3D Explainer Videos — for SaaS/tech startups, apps, e-learning
2. Architectural Visualization — for real-estate developers, architects, interior designers
3. Product Renders — for e-commerce, jewellery, furniture, consumer goods
4. Marketing & Social Content — for digital agencies, local businesses
5. Small Business Websites — for cafes, salons, gyms, consultants

---

## Setup

### 1. Clone and enter the repo

```bash
git clone https://github.com/MiHiR1296/Job-search
cd Job-search
```

### 2. Create a virtual environment

```bash
python3 -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
```

### 3. Install dependencies

```bash
pip install -r requirements.txt
```

### 4. Configure environment (optional but recommended)

```bash
cp .env.example .env
```

Edit `.env` and fill in:

| Variable | Required? | What it does |
|---|---|---|
| `OPENAI_API_KEY` | Optional | Enables AI-generated email & call scripts (uses templates otherwise) |
| `GOOGLE_PLACES_API_KEY` | Optional | Enables real business search (uses 10 demo leads otherwise) |
| `SECRET_KEY` | Recommended | Flask session security |

You can also paste your Google Places API key directly in the search form without adding it to `.env`.

### 5. Run the app

```bash
python run.py
```

Open **http://localhost:5000** in your browser.

---

## How to use it

1. **Dashboard** — see your stats and top leads
2. **Find Clients** → type a business category (e.g. `jewelry store`, `real estate agency`, `yoga studio`) and a city → click Search
3. **Import** the leads that look promising (or Import All)
4. **Open a lead** → click **Generate Outreach** — you'll get a personalised email + call script
5. **Copy the email**, send it, then update the status to `Emailed`
6. Use the **Call Script** tab when you follow up by phone
7. Mark as **Converted** when you land the job 🎉

---

## Getting API keys (free)

### Google Places API
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project → enable **Places API**
3. Create an API key → restrict it to Places API for security
4. Free tier: 200 USD/month credit (covers thousands of searches)

### OpenAI API
1. Sign up at [platform.openai.com](https://platform.openai.com)
2. Go to API Keys → create a new key
3. GPT-4o-mini is very cheap (~$0.15/1M tokens) — generating 100 outreach packs costs under ₹5

---

## Project structure

```
Job-search/
├── run.py                  # Entry point
├── requirements.txt
├── .env.example
├── app/
│   ├── __init__.py         # Flask app factory
│   ├── config.py           # Profile, services, API keys
│   ├── database.py         # SQLAlchemy setup
│   ├── models.py           # Lead model
│   ├── lead_finder.py      # Google Places search + scoring
│   ├── outreach.py         # Email & call script generation
│   ├── routes.py           # All Flask routes
│   ├── templates/
│   │   ├── base.html
│   │   ├── dashboard.html
│   │   ├── find.html
│   │   ├── leads.html
│   │   ├── lead_detail.html
│   │   ├── outreach.html
│   │   └── add_lead.html
│   └── static/
│       ├── css/style.css
│       └── js/app.js
```

---

## What's next (planned)

- [ ] Email sending integration (Gmail / SMTP)
- [ ] Bulk outreach with per-lead personalisation
- [ ] LinkedIn profile scraping for contact names
- [ ] Follow-up reminder scheduler
- [ ] Export leads to CSV
- [ ] More industry categories
- [ ] WhatsApp message template generator
