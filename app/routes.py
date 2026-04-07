"""
routes.py
---------
All Flask routes for the Client Outreach System.
"""

from datetime import datetime
from flask import Blueprint, render_template, request, redirect, url_for, flash, jsonify
from app.database import db
from app.models import Lead
from app.lead_finder import find_leads
from app.outreach import generate_outreach
from app.config import SERVICES, ARTIST_PROFILE

bp = Blueprint("main", __name__)

STATUS_LABELS = {
    "new": ("New", "badge-new"),
    "emailed": ("Emailed", "badge-emailed"),
    "called": ("Called", "badge-called"),
    "replied": ("Replied", "badge-replied"),
    "converted": ("Converted!", "badge-converted"),
    "archived": ("Archived", "badge-archived"),
}


# ---------------------------------------------------------------------------
# Dashboard
# ---------------------------------------------------------------------------

@bp.route("/")
def dashboard():
    total = Lead.query.count()
    new_leads = Lead.query.filter_by(status="new").count()
    emailed = Lead.query.filter_by(status="emailed").count()
    converted = Lead.query.filter_by(status="converted").count()
    called = Lead.query.filter_by(status="called").count()

    top_leads = (
        Lead.query
        .filter(Lead.status.notin_(["archived"]))
        .order_by(Lead.match_score.desc())
        .limit(5)
        .all()
    )

    service_stats = []
    for s in SERVICES:
        count = Lead.query.filter_by(matched_service_id=s["id"]).count()
        service_stats.append({"label": s["label"], "count": count, "id": s["id"]})

    return render_template(
        "dashboard.html",
        total=total,
        new_leads=new_leads,
        emailed=emailed,
        converted=converted,
        called=called,
        top_leads=top_leads,
        service_stats=service_stats,
        status_labels=STATUS_LABELS,
        profile=ARTIST_PROFILE,
    )


# ---------------------------------------------------------------------------
# Lead list
# ---------------------------------------------------------------------------

@bp.route("/leads")
def leads():
    status_filter = request.args.get("status", "")
    service_filter = request.args.get("service", "")
    sort = request.args.get("sort", "score")

    query = Lead.query

    if status_filter:
        query = query.filter_by(status=status_filter)
    if service_filter:
        query = query.filter_by(matched_service_id=service_filter)

    if sort == "score":
        query = query.order_by(Lead.match_score.desc())
    elif sort == "name":
        query = query.order_by(Lead.name.asc())
    elif sort == "date":
        query = query.order_by(Lead.created_at.desc())

    all_leads = query.all()

    return render_template(
        "leads.html",
        leads=all_leads,
        services=SERVICES,
        status_filter=status_filter,
        service_filter=service_filter,
        sort=sort,
        status_labels=STATUS_LABELS,
    )


# ---------------------------------------------------------------------------
# Find new leads
# ---------------------------------------------------------------------------

@bp.route("/find", methods=["GET", "POST"])
def find():
    results = []
    query = ""
    location = ""
    error = ""

    if request.method == "POST":
        query = request.form.get("query", "").strip()
        location = request.form.get("location", "Mumbai").strip()
        api_key = request.form.get("api_key", "").strip()

        # Allow API key override via form (useful without .env)
        import os
        if not api_key:
            api_key = os.getenv("GOOGLE_PLACES_API_KEY", "")

        raw_leads = find_leads(query, location, api_key, max_results=20)

        for lead_data in raw_leads:
            # Skip already imported leads
            existing = Lead.query.filter_by(place_id=lead_data["place_id"]).first()
            lead_data["already_imported"] = existing is not None

        results = raw_leads

        if not results:
            error = "No results found. Try a different search term or location."

    return render_template(
        "find.html",
        results=results,
        query=query,
        location=location,
        error=error,
        services=SERVICES,
        status_labels=STATUS_LABELS,
    )


# ---------------------------------------------------------------------------
# Import lead from search results
# ---------------------------------------------------------------------------

@bp.route("/leads/import", methods=["POST"])
def import_lead():
    data = request.form
    place_id = data.get("place_id")

    if Lead.query.filter_by(place_id=place_id).first():
        flash("Lead already imported.", "warning")
        return redirect(url_for("main.find"))

    lead = Lead(
        name=data.get("name"),
        business_type=data.get("business_type"),
        address=data.get("address"),
        city=data.get("city"),
        website=data.get("website"),
        phone=data.get("phone"),
        email=data.get("email", ""),
        place_id=place_id,
        match_score=int(data.get("match_score", 50)),
        matched_service_id=data.get("matched_service_id"),
        match_reason=data.get("match_reason"),
        source=data.get("source", "google_places"),
        status="new",
    )
    db.session.add(lead)
    db.session.commit()
    flash(f"'{lead.name}' imported successfully!", "success")
    return redirect(url_for("main.lead_detail", lead_id=lead.id))


# ---------------------------------------------------------------------------
# Import ALL results at once
# ---------------------------------------------------------------------------

@bp.route("/leads/import-all", methods=["POST"])
def import_all():
    import json
    leads_json = request.form.get("leads_json", "[]")
    try:
        leads_data = json.loads(leads_json)
    except Exception:
        flash("Could not parse leads data.", "danger")
        return redirect(url_for("main.find"))

    imported = 0
    skipped = 0
    for ld in leads_data:
        if Lead.query.filter_by(place_id=ld.get("place_id")).first():
            skipped += 1
            continue
        lead = Lead(
            name=ld.get("name"),
            business_type=ld.get("business_type"),
            address=ld.get("address"),
            city=ld.get("city"),
            website=ld.get("website", ""),
            phone=ld.get("phone", ""),
            email=ld.get("email", ""),
            place_id=ld.get("place_id"),
            match_score=int(ld.get("match_score", 50)),
            matched_service_id=ld.get("matched_service_id"),
            match_reason=ld.get("match_reason"),
            source=ld.get("source", "google_places"),
            status="new",
        )
        db.session.add(lead)
        imported += 1

    db.session.commit()
    flash(f"Imported {imported} leads. {skipped} already existed.", "success")
    return redirect(url_for("main.leads"))


# ---------------------------------------------------------------------------
# Lead detail
# ---------------------------------------------------------------------------

@bp.route("/leads/<int:lead_id>")
def lead_detail(lead_id):
    lead = Lead.query.get_or_404(lead_id)
    service = next((s for s in SERVICES if s["id"] == lead.matched_service_id), SERVICES[0])
    return render_template(
        "lead_detail.html",
        lead=lead,
        service=service,
        services=SERVICES,
        status_labels=STATUS_LABELS,
        profile=ARTIST_PROFILE,
    )


# ---------------------------------------------------------------------------
# Generate outreach for a lead
# ---------------------------------------------------------------------------

@bp.route("/leads/<int:lead_id>/generate", methods=["POST"])
def generate(lead_id):
    lead = Lead.query.get_or_404(lead_id)

    result = generate_outreach(lead.to_dict())

    lead.email_subject = result["email_subject"]
    lead.email_body = result["email_body"]
    lead.call_script = result["call_script"]
    lead.outreach_generated_at = datetime.utcnow()
    db.session.commit()

    flash("Outreach generated successfully!", "success")
    return redirect(url_for("main.outreach_view", lead_id=lead.id))


# ---------------------------------------------------------------------------
# Outreach view (email + call script)
# ---------------------------------------------------------------------------

@bp.route("/leads/<int:lead_id>/outreach")
def outreach_view(lead_id):
    lead = Lead.query.get_or_404(lead_id)
    service = next((s for s in SERVICES if s["id"] == lead.matched_service_id), SERVICES[0])

    if not lead.email_body:
        flash("Outreach not yet generated. Click 'Generate Outreach' first.", "warning")
        return redirect(url_for("main.lead_detail", lead_id=lead.id))

    return render_template(
        "outreach.html",
        lead=lead,
        service=service,
        status_labels=STATUS_LABELS,
        profile=ARTIST_PROFILE,
    )


# ---------------------------------------------------------------------------
# Update lead status
# ---------------------------------------------------------------------------

@bp.route("/leads/<int:lead_id>/status", methods=["POST"])
def update_status(lead_id):
    lead = Lead.query.get_or_404(lead_id)
    new_status = request.form.get("status")
    if new_status in STATUS_LABELS:
        lead.status = new_status
        db.session.commit()
        flash(f"Status updated to '{STATUS_LABELS[new_status][0]}'.", "success")
    return redirect(url_for("main.lead_detail", lead_id=lead.id))


# ---------------------------------------------------------------------------
# Update lead notes
# ---------------------------------------------------------------------------

@bp.route("/leads/<int:lead_id>/notes", methods=["POST"])
def update_notes(lead_id):
    lead = Lead.query.get_or_404(lead_id)
    lead.notes = request.form.get("notes", "")
    db.session.commit()
    flash("Notes saved.", "success")
    return redirect(url_for("main.lead_detail", lead_id=lead.id))


# ---------------------------------------------------------------------------
# Update lead email (for manual entry)
# ---------------------------------------------------------------------------

@bp.route("/leads/<int:lead_id>/email-update", methods=["POST"])
def update_email(lead_id):
    lead = Lead.query.get_or_404(lead_id)
    lead.email = request.form.get("email", "")
    db.session.commit()
    flash("Email address saved.", "success")
    return redirect(url_for("main.lead_detail", lead_id=lead.id))


# ---------------------------------------------------------------------------
# Delete lead
# ---------------------------------------------------------------------------

@bp.route("/leads/<int:lead_id>/delete", methods=["POST"])
def delete_lead(lead_id):
    lead = Lead.query.get_or_404(lead_id)
    db.session.delete(lead)
    db.session.commit()
    flash(f"'{lead.name}' removed.", "info")
    return redirect(url_for("main.leads"))


# ---------------------------------------------------------------------------
# Add manual lead
# ---------------------------------------------------------------------------

@bp.route("/leads/add", methods=["GET", "POST"])
def add_lead():
    if request.method == "POST":
        import uuid
        service_id = request.form.get("matched_service_id", "marketing_content")
        lead = Lead(
            name=request.form.get("name"),
            business_type=request.form.get("business_type"),
            address=request.form.get("address"),
            city=request.form.get("city"),
            website=request.form.get("website", ""),
            phone=request.form.get("phone", ""),
            email=request.form.get("email", ""),
            place_id=f"manual_{uuid.uuid4().hex[:8]}",
            matched_service_id=service_id,
            match_score=int(request.form.get("match_score", 60)),
            match_reason="Manually added",
            source="manual",
            status="new",
        )
        db.session.add(lead)
        db.session.commit()
        flash(f"'{lead.name}' added!", "success")
        return redirect(url_for("main.lead_detail", lead_id=lead.id))

    return render_template("add_lead.html", services=SERVICES)


# ---------------------------------------------------------------------------
# API endpoints (for JS fetch)
# ---------------------------------------------------------------------------

@bp.route("/api/leads")
def api_leads():
    leads = Lead.query.order_by(Lead.match_score.desc()).all()
    return jsonify([l.to_dict() for l in leads])


@bp.route("/api/leads/<int:lead_id>")
def api_lead(lead_id):
    lead = Lead.query.get_or_404(lead_id)
    return jsonify(lead.to_dict())
