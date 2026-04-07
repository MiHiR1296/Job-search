"""
lead_finder.py
--------------
Discovers potential clients using the Google Places API (Text Search & Nearby Search).
Falls back to a small curated demo dataset when no API key is configured so the UI
is still usable out of the box.

Scoring logic
~~~~~~~~~~~~~
Each business is checked against all of Mihir's services.  For every matching
keyword or business-category hit the score goes up.  The service with the
highest overlap wins and becomes `matched_service_id`.  Score is normalised
to 0-100.
"""

import re
import requests
from app.config import SERVICES

PLACES_TEXT_SEARCH = "https://maps.googleapis.com/maps/api/place/textsearch/json"
PLACES_NEARBY = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
PLACES_DETAIL = "https://maps.googleapis.com/maps/api/place/details/json"


# ---------------------------------------------------------------------------
# Scoring helpers
# ---------------------------------------------------------------------------

def _score_business(name: str, business_type: str) -> tuple[int, str, str]:
    """Return (score 0-100, matched_service_id, reason_text)."""
    combined = f"{name} {business_type}".lower()

    best_service = None
    best_hits = 0
    best_matched_kw = []

    for service in SERVICES:
        hits = 0
        matched_kw = []
        # keyword match
        for kw in service["keywords"]:
            if kw in combined:
                hits += 3
                matched_kw.append(kw)
        # ideal-client phrase match
        for phrase in service["ideal_clients"]:
            if phrase in combined:
                hits += 5
                matched_kw.append(phrase)
        if hits > best_hits:
            best_hits = hits
            best_service = service
            best_matched_kw = matched_kw

    if not best_service or best_hits == 0:
        return 10, "marketing_content", "General small business — marketing content may help"

    # Normalise: max raw score ~40 → map to 50-95 range
    raw_max = 40
    score = 50 + min(int((best_hits / raw_max) * 45), 45)

    reason = (
        f"Matched service: {best_service['label']}. "
        f"Keywords found: {', '.join(set(best_matched_kw))}."
    )
    return score, best_service["id"], reason


# ---------------------------------------------------------------------------
# Google Places helpers
# ---------------------------------------------------------------------------

def _get_place_details(place_id: str, api_key: str) -> dict:
    """Fetch website + formatted phone from the Places Detail endpoint."""
    params = {
        "place_id": place_id,
        "fields": "website,formatted_phone_number,international_phone_number",
        "key": api_key,
    }
    try:
        r = requests.get(PLACES_DETAIL, params=params, timeout=8)
        result = r.json().get("result", {})
        return {
            "website": result.get("website", ""),
            "phone": result.get("international_phone_number") or result.get("formatted_phone_number", ""),
        }
    except Exception:
        return {"website": "", "phone": ""}


def _parse_place(place: dict, api_key: str) -> dict:
    """Convert a raw Places API result to a normalised lead dict."""
    place_id = place.get("place_id", "")
    name = place.get("name", "")
    address = place.get("formatted_address") or place.get("vicinity", "")
    types = place.get("types", [])
    business_type = types[0].replace("_", " ").title() if types else "Business"

    details = _get_place_details(place_id, api_key) if api_key else {}

    score, service_id, reason = _score_business(name, business_type)

    # Rough city extraction from address
    city = ""
    parts = address.split(",")
    if len(parts) >= 2:
        city = parts[-2].strip()

    return {
        "place_id": place_id,
        "name": name,
        "business_type": business_type,
        "address": address,
        "city": city,
        "website": details.get("website", ""),
        "phone": details.get("phone", ""),
        "email": "",
        "match_score": score,
        "matched_service_id": service_id,
        "match_reason": reason,
        "source": "google_places",
    }


# ---------------------------------------------------------------------------
# Public search function
# ---------------------------------------------------------------------------

def find_leads(query: str, location: str, api_key: str, max_results: int = 20) -> list[dict]:
    """
    Search Google Places for businesses matching *query* near *location*.
    Returns a list of normalised lead dicts sorted by match_score descending.
    Falls back to demo data if api_key is empty.
    """
    if not api_key:
        return _demo_leads(query, location)

    params = {
        "query": f"{query} in {location}",
        "key": api_key,
    }

    leads = []
    seen = set()
    next_page_token = None

    while len(leads) < max_results:
        if next_page_token:
            params = {"pagetoken": next_page_token, "key": api_key}

        try:
            r = requests.get(PLACES_TEXT_SEARCH, params=params, timeout=10)
            data = r.json()
        except Exception as e:
            break

        if data.get("status") not in ("OK", "ZERO_RESULTS"):
            break

        for place in data.get("results", []):
            pid = place.get("place_id")
            if pid and pid not in seen:
                seen.add(pid)
                leads.append(_parse_place(place, api_key))

        next_page_token = data.get("next_page_token")
        if not next_page_token or len(leads) >= max_results:
            break

    leads.sort(key=lambda x: x["match_score"], reverse=True)
    return leads[:max_results]


# ---------------------------------------------------------------------------
# Demo / fallback data (no API key required)
# ---------------------------------------------------------------------------

def _demo_leads(query: str = "", location: str = "Mumbai") -> list[dict]:
    """
    A small curated set of fictional but realistic small-business leads.
    Used when no Google Places API key is configured.
    """
    demo = [
        {
            "place_id": "demo_001",
            "name": "Prestige Realty Developers",
            "business_type": "Real Estate Agency",
            "address": "Andheri West, Mumbai",
            "city": "Mumbai",
            "website": "https://example.com/prestige",
            "phone": "+91 98200 00001",
            "email": "",
        },
        {
            "place_id": "demo_002",
            "name": "Nova Tech Solutions",
            "business_type": "SaaS Startup",
            "address": "BKC, Mumbai",
            "city": "Mumbai",
            "website": "https://example.com/novatech",
            "phone": "+91 98200 00002",
            "email": "",
        },
        {
            "place_id": "demo_003",
            "name": "Luminary Jewels",
            "business_type": "Jewelry Store",
            "address": "Dadar, Mumbai",
            "city": "Mumbai",
            "website": "https://example.com/luminary",
            "phone": "+91 98200 00003",
            "email": "",
        },
        {
            "place_id": "demo_004",
            "name": "BrewBox Cafe",
            "business_type": "Cafe",
            "address": "Bandra, Mumbai",
            "city": "Mumbai",
            "website": "",
            "phone": "+91 98200 00004",
            "email": "",
        },
        {
            "place_id": "demo_005",
            "name": "PixelPulse Digital Agency",
            "business_type": "Marketing Agency",
            "address": "Lower Parel, Mumbai",
            "city": "Mumbai",
            "website": "https://example.com/pixelpulse",
            "phone": "+91 98200 00005",
            "email": "",
        },
        {
            "place_id": "demo_006",
            "name": "ZenSpace Yoga Studio",
            "business_type": "Yoga Studio",
            "address": "Powai, Mumbai",
            "city": "Mumbai",
            "website": "",
            "phone": "+91 98200 00006",
            "email": "",
        },
        {
            "place_id": "demo_007",
            "name": "ArchiForm Studios",
            "business_type": "Architecture Firm",
            "address": "Worli, Mumbai",
            "city": "Mumbai",
            "website": "https://example.com/archiform",
            "phone": "+91 98200 00007",
            "email": "",
        },
        {
            "place_id": "demo_008",
            "name": "Homecraft Furniture",
            "business_type": "Furniture Store",
            "address": "Malad, Mumbai",
            "city": "Mumbai",
            "website": "https://example.com/homecraft",
            "phone": "+91 98200 00008",
            "email": "",
        },
        {
            "place_id": "demo_009",
            "name": "Bloom Skin Clinic",
            "business_type": "Beauty Salon",
            "address": "Thane",
            "city": "Thane",
            "website": "",
            "phone": "+91 98200 00009",
            "email": "",
        },
        {
            "place_id": "demo_010",
            "name": "SwiftFit Gym",
            "business_type": "Gym",
            "address": "Kalyan, Mumbai",
            "city": "Kalyan",
            "website": "",
            "phone": "+91 98200 00010",
            "email": "",
        },
    ]

    for lead in demo:
        score, service_id, reason = _score_business(lead["name"], lead["business_type"])
        lead["match_score"] = score
        lead["matched_service_id"] = service_id
        lead["match_reason"] = reason
        lead["source"] = "demo"

    # Filter by query keyword if provided
    if query:
        q = query.lower()
        filtered = [l for l in demo if q in l["name"].lower() or q in l["business_type"].lower()]
        if filtered:
            demo = filtered

    demo.sort(key=lambda x: x["match_score"], reverse=True)
    return demo
