"""
outreach.py
-----------
Generates personalised cold-email copy and a call script for a given lead.

Two modes:
  1. AI mode  — uses OpenAI GPT-4o-mini when an API key is configured.
  2. Template mode — produces a polished, variable-substituted template when
                     no OpenAI key is available (fully functional offline).
"""

import os
from datetime import datetime
from app.config import ARTIST_PROFILE, SERVICES


def _get_service(service_id: str) -> dict:
    for s in SERVICES:
        if s["id"] == service_id:
            return s
    return SERVICES[0]


# ---------------------------------------------------------------------------
# Template-based generation (no API key required)
# ---------------------------------------------------------------------------

_EMAIL_TEMPLATES = {
    "explainer_video": {
        "subject": "Bring your product to life with a 3D explainer video — {business_name}",
        "body": """Hi {contact_name},

I came across {business_name} and I think a short 3D explainer video could really help you convert more visitors into customers — especially for landing pages and paid ads.

I'm Mihir, a 3D Generalist with 9+ years of experience building animations and visual systems for companies like FYND, Amazon, and Wayfair. I create concise, visually sharp explainer videos that show exactly what a product does without needing an expensive film crew.

A few things I can put together for you:
• 30-60 second animated explainer (product walkthrough or concept breakdown)
• Multiple format exports — web, Instagram Reels, YouTube Shorts
• Revisions until you're happy

You can see examples of my work here: {portfolio}

Would you be open to a quick 15-minute chat to see if it's a fit? No pressure — just want to understand your goals first.

Warm regards,
{your_name}
{your_email} | {your_phone}
{portfolio}""",
    },
    "arch_viz": {
        "subject": "Photorealistic renders for {business_name} — help clients visualise before they buy",
        "body": """Hi {contact_name},

I noticed {business_name} and thought I could add real value with high-quality 3D architectural renders or virtual walkthroughs.

I'm Mihir — a 3D artist based in Mumbai with 9+ years of experience. I've created archviz work for international offices and real-estate projects, delivering renders that help clients commit faster because they can see exactly what they're getting.

What I offer:
• Photorealistic exterior & interior renders
• 360° virtual tour animations
• Fast turnaround (typically 3-7 days per scene)
• Competitive pricing for independent developers & studios

Portfolio & examples: {portfolio}

Happy to share a couple of relevant samples or jump on a quick call at your convenience.

Best,
{your_name}
{your_email} | {your_phone}""",
    },
    "product_render": {
        "subject": "Studio-quality product renders for {business_name} — no photoshoot needed",
        "body": """Hi {contact_name},

I'm reaching out because I think {business_name} could benefit from professional 3D product renders — especially if you're selling online or running ad campaigns.

My name is Mihir, a 3D artist with 9+ years of experience creating product renders for Amazon, Wayfair, and various e-commerce brands. I produce photorealistic images at a fraction of traditional photography costs, with the flexibility to change colours, backgrounds, and angles on demand.

Here's what I can do for you:
• Clean white-background shots for listings
• Lifestyle / contextual renders for ads
• Packaging visualisations
• Unlimited colour/variant renders from a single 3D model

See my work: {portfolio}

If you're interested I'm happy to do a free sample render of one of your products so you can judge quality before committing.

Let me know!
{your_name}
{your_email} | {your_phone}""",
    },
    "marketing_content": {
        "subject": "3D visuals & motion content for {business_name}'s marketing",
        "body": """Hi {contact_name},

I came across {business_name} and wanted to reach out — I create 3D visuals and short motion-graphic clips specifically for small businesses that want to stand out on social media and in digital ads.

I'm Mihir, a 3D generalist based in Mumbai. I've worked with brands on everything from product animations to branded social content, and I focus on making things look polished without the big-agency price tag.

I can help with:
• Short 3D animations for Instagram / Facebook / YouTube
• Product showcase clips
• Branded visuals for campaigns
• One-off or retainer arrangements

Portfolio: {portfolio}

Would love to understand your current marketing setup and see if there's a fit. 15 minutes is all I'd need.

Cheers,
{your_name}
{your_email} | {your_phone}""",
    },
    "small_website": {
        "subject": "A modern website for {business_name} — first impression matters",
        "body": """Hi {contact_name},

I was looking for {business_type} in {city} and noticed {business_name} — wanted to reach out because I think a clean, well-built website could bring you more customers.

I'm Mihir, a developer & 3D artist based in Mumbai. I build fast, mobile-friendly small-business websites that actually reflect the quality of your business. I can also add interactive 3D elements if that fits your brand.

What I deliver:
• Custom-designed, mobile-responsive site
• Google-ready (basic SEO setup)
• Booking / contact forms
• Hosting guidance
• Optional 3D / animation features

I keep costs reasonable for small businesses and offer ongoing maintenance if needed.

Portfolio: {portfolio}

Happy to do a free mockup of your homepage so you can see the direction before deciding.

Best,
{your_name}
{your_email} | {your_phone}""",
    },
}

_CALL_SCRIPT_TEMPLATE = """CALL SCRIPT — {business_name}
Service focus: {service_label}
==============================================

OPENING (0:00 – 0:30)
"Hi, could I speak with {contact_name} please?
...
Hi {contact_name}, my name is Mihir Botle — I'm a 3D artist based in Mumbai.
I'll only take a minute — I came across {business_name} and thought there might be a
good fit between what I do and what you might need. Is now an okay time to talk briefly?"

[If YES — proceed. If NO — "No problem, when would be a better time? I can call back."]

PROBLEM / HOOK (0:30 – 1:30)
"I specialise in {service_description}

Many {business_type} businesses I work with find that [common pain point]:
{pain_point}

Is that something you've run into as well?"

[Listen. Let them talk. Don't interrupt.]

YOUR SOLUTION (1:30 – 2:30)
"What I do is {solution_pitch}

I've done similar work for [type of clients] — I can share some examples via email
after our call if that would help."

SOCIAL PROOF (2:30 – 3:00)
"I've worked with companies like FYND, Amazon product teams, and real-estate developers
in Mumbai — so I understand the kind of quality and turnaround you'd expect."

CALL TO ACTION (3:00 – 3:30)
"Would it make sense to set up a 20-minute video call this week?
I can walk you through some relevant examples and we can see if there's a fit.
No obligation — just want to understand your goals."

[If yes → get a specific date/time]
[If unsure → "Could I send you a couple of examples by email first?"]

CLOSE
"Great — I'll send a calendar invite to [email address].
Looking forward to it, {contact_name}. Have a great day!"

==============================================
QUICK REFERENCE
Business:    {business_name}
Type:        {business_type}
Service:     {service_label}
Your name:   {your_name}
Your email:  {your_email}
Portfolio:   {portfolio}
"""

_PAIN_POINTS = {
    "explainer_video": "visitors landing on your site or app store page don't fully understand the product, so they bounce without converting",
    "arch_viz": "clients struggle to commit to a project when they can't see what it will look like — renders remove that uncertainty",
    "product_render": "product photography is expensive and slow, and you can't easily produce variant shots for every colour or configuration",
    "marketing_content": "it's hard to consistently produce fresh, high-quality visual content for social media without a big team or budget",
    "small_website": "potential customers search online but either can't find you or land on an outdated page that doesn't reflect your quality",
}

_SOLUTION_PITCHES = {
    "explainer_video": "create a crisp 30-60 second 3D animation that shows exactly what your product does — the kind that doubles click-through rates on ads",
    "arch_viz": "produce photorealistic renders or walkthroughs of your spaces so clients can see — and love — the design before a single brick is laid",
    "product_render": "build a 3D model of your product once, then generate unlimited render variants — different angles, colours, backgrounds — all at a flat rate",
    "marketing_content": "deliver a pack of 3D visuals and short clips every month that you can post across all your channels without needing an in-house designer",
    "small_website": "build a clean, fast, mobile-first website that reflects the real quality of your business and turns visitors into calls and bookings",
}


def _fill_template(template: str, context: dict) -> str:
    for key, value in context.items():
        template = template.replace(f"{{{key}}}", str(value))
    return template


def generate_outreach_template(lead: dict) -> dict:
    """Generate email + call script using templates (no API required)."""
    service = _get_service(lead.get("matched_service_id", "marketing_content"))
    sid = service["id"]

    email_tmpl = _EMAIL_TEMPLATES.get(sid, _EMAIL_TEMPLATES["marketing_content"])

    contact_name = "there"
    city = lead.get("city", "your city")

    context = {
        "business_name": lead.get("name", "your business"),
        "contact_name": contact_name,
        "business_type": lead.get("business_type", "business"),
        "city": city,
        "your_name": ARTIST_PROFILE["name"],
        "your_email": ARTIST_PROFILE["email"],
        "your_phone": ARTIST_PROFILE["phone"],
        "portfolio": ARTIST_PROFILE["portfolio"],
        "linkedin": ARTIST_PROFILE["linkedin"],
    }

    subject = _fill_template(email_tmpl["subject"], context)
    body = _fill_template(email_tmpl["body"], context)

    call_context = {
        **context,
        "service_label": service["label"],
        "service_description": service["description"],
        "pain_point": _PAIN_POINTS.get(sid, ""),
        "solution_pitch": _SOLUTION_PITCHES.get(sid, ""),
    }
    call_script = _fill_template(_CALL_SCRIPT_TEMPLATE, call_context)

    return {
        "email_subject": subject,
        "email_body": body,
        "call_script": call_script,
        "method": "template",
    }


# ---------------------------------------------------------------------------
# AI-powered generation (OpenAI)
# ---------------------------------------------------------------------------

def generate_outreach_ai(lead: dict, api_key: str) -> dict:
    """Generate email + call script using GPT-4o-mini."""
    try:
        from openai import OpenAI
        client = OpenAI(api_key=api_key)
    except ImportError:
        return generate_outreach_template(lead)

    service = _get_service(lead.get("matched_service_id", "marketing_content"))

    system_prompt = f"""You are helping {ARTIST_PROFILE['name']}, a {ARTIST_PROFILE['tagline']} 
based in Mumbai with {ARTIST_PROFILE['experience_years']}+ years of experience.

{ARTIST_PROFILE['summary']}

Contact: {ARTIST_PROFILE['email']} | {ARTIST_PROFILE['phone']}
Portfolio: {ARTIST_PROFILE['portfolio']}

You write concise, warm, non-pushy cold outreach for small businesses.
Never use jargon. Keep emails under 200 words. Keep call scripts under 400 words.
Always sound human, not corporate."""

    user_prompt = f"""Write a cold email AND a phone call script for this potential client:

Business name: {lead.get('name')}
Business type: {lead.get('business_type')}
City: {lead.get('city')}
Best-fit service: {service['label']}
Service description: {service['description']}
Why it's a match: {lead.get('match_reason')}

Format your response EXACTLY like this:

SUBJECT: [email subject line]

EMAIL:
[email body — friendly, 150-200 words, ends with a soft CTA for a 15 min call]

CALL SCRIPT:
[structured call script with Opening, Hook, Solution, CTA — under 400 words]"""

    try:
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            max_tokens=900,
            temperature=0.7,
        )
        content = response.choices[0].message.content.strip()

        # Parse sections
        subject = ""
        email_body = ""
        call_script = ""

        if "SUBJECT:" in content:
            subject_start = content.index("SUBJECT:") + len("SUBJECT:")
            subject_end = content.index("\n", subject_start)
            subject = content[subject_start:subject_end].strip()

        if "EMAIL:" in content:
            email_start = content.index("EMAIL:") + len("EMAIL:")
            if "CALL SCRIPT:" in content:
                email_end = content.index("CALL SCRIPT:")
            else:
                email_end = len(content)
            email_body = content[email_start:email_end].strip()

        if "CALL SCRIPT:" in content:
            call_start = content.index("CALL SCRIPT:") + len("CALL SCRIPT:")
            call_script = content[call_start:].strip()

        if subject and email_body:
            return {
                "email_subject": subject,
                "email_body": email_body,
                "call_script": call_script,
                "method": "ai",
            }
    except Exception:
        pass

    return generate_outreach_template(lead)


# ---------------------------------------------------------------------------
# Main entry point
# ---------------------------------------------------------------------------

def generate_outreach(lead: dict) -> dict:
    """
    Generate outreach for a lead.  Uses AI if OPENAI_API_KEY is set,
    otherwise falls back to templates.
    """
    api_key = os.getenv("OPENAI_API_KEY", "")
    if api_key:
        return generate_outreach_ai(lead, api_key)
    return generate_outreach_template(lead)
