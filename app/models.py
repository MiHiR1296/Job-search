from datetime import datetime
from app.database import db


class Lead(db.Model):
    __tablename__ = "leads"

    id = db.Column(db.Integer, primary_key=True)
    # Basic info
    name = db.Column(db.String(255), nullable=False)
    business_type = db.Column(db.String(100))
    address = db.Column(db.String(500))
    city = db.Column(db.String(100))
    website = db.Column(db.String(300))
    phone = db.Column(db.String(50))
    email = db.Column(db.String(200))
    # Source
    source = db.Column(db.String(50), default="google_places")
    place_id = db.Column(db.String(200), unique=True)
    # Scoring
    match_score = db.Column(db.Integer, default=0)   # 0-100
    matched_service_id = db.Column(db.String(50))    # best-fit service
    match_reason = db.Column(db.Text)
    # Status tracking
    status = db.Column(db.String(30), default="new")
    # new | emailed | called | replied | converted | archived
    notes = db.Column(db.Text)
    # Generated outreach
    email_subject = db.Column(db.String(300))
    email_body = db.Column(db.Text)
    call_script = db.Column(db.Text)
    # Timestamps
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    outreach_generated_at = db.Column(db.DateTime)

    def to_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "business_type": self.business_type,
            "address": self.address,
            "city": self.city,
            "website": self.website,
            "phone": self.phone,
            "email": self.email,
            "source": self.source,
            "place_id": self.place_id,
            "match_score": self.match_score,
            "matched_service_id": self.matched_service_id,
            "match_reason": self.match_reason,
            "status": self.status,
            "notes": self.notes,
            "email_subject": self.email_subject,
            "email_body": self.email_body,
            "call_script": self.call_script,
            "created_at": self.created_at.isoformat() if self.created_at else None,
            "updated_at": self.updated_at.isoformat() if self.updated_at else None,
        }

    def __repr__(self):
        return f"<Lead {self.name} [{self.status}] score={self.match_score}>"
