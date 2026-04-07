"""
Outreach Module

Generates personalized email templates and call scripts based on
the target market and service being offered.
"""

from datetime import datetime

from jinja2 import Template
from rich.console import Console
from rich.panel import Panel
from rich.table import Table

from .config_loader import load_profile, load_target_markets

console = Console()


EMAIL_TEMPLATES = {
    "cold_intro": {
        "name": "Cold Introduction",
        "subject": "{{ service_name }} for {{ client_name }} — quick idea",
        "body": """Hi {{ client_contact_name }},

I came across {{ client_name }}{{ ' on ' + source if source else '' }} and was impressed by {{ compliment }}.

I'm Mihir, a 3D artist with 9+ years of experience working with brands like Amazon, Wayfair, and FYND. I specialize in {{ service_name | lower }}.

I noticed {{ pain_point }}. I can help with that.

Here's what I can do for you:
{% for d in deliverables %}• {{ d }}
{% endfor %}
{{ proof_point }}

Would you be open to a quick 15-minute call this week? I'd love to understand your needs and share a few ideas — no commitment.

{% if free_offer %}I'd also be happy to {{ free_offer }} so you can see the quality firsthand.

{% endif %}Best regards,
Mihir Botle
3D Generalist & Technical Artist
{{ email }}
{{ phone }}

Portfolio: {{ behance }}
Live Demo: {{ live_demo }}""",
    },
    "follow_up_1": {
        "name": "First Follow-up (3-4 days later)",
        "subject": "Re: {{ service_name }} for {{ client_name }}",
        "body": """Hi {{ client_contact_name }},

Just wanted to follow up on my previous email. I know you're busy, so I'll keep this brief.

I put together a quick breakdown of how {{ service_name | lower }} could work for {{ client_name }}:

{{ specific_value_prop }}

Here's a relevant example from my portfolio: {{ relevant_link }}

If this isn't the right time, no worries at all. But if you're curious, I'm happy to jump on a quick call or send over a free sample.

Cheers,
Mihir""",
    },
    "follow_up_2": {
        "name": "Second Follow-up (1 week later)",
        "subject": "One last note — {{ client_name }}",
        "body": """Hi {{ client_contact_name }},

I don't want to be a bother, so this will be my last email on this.

I genuinely think {{ client_name }} could benefit from {{ key_benefit }}. If you ever need 3D visualization work in the future — product renders, architectural walkthroughs, interactive 3D experiences, or explainer videos — I'd love to be your go-to person.

I'm bookmarking {{ client_name }} and would be happy to help whenever the time is right.

All the best,
Mihir Botle
{{ email }}""",
    },
    "referral_ask": {
        "name": "Referral Request (after completed project)",
        "subject": "Quick favor — know anyone who needs 3D work?",
        "body": """Hi {{ client_contact_name }},

I really enjoyed working on {{ project_name }} with you. Thanks again for the opportunity!

If you know anyone in your network who might benefit from 3D visualization services — product renders, animations, interactive web experiences, or architectural visualization — I'd appreciate a warm intro.

I'm looking to work with more businesses like yours who value quality visuals.

No pressure at all. Either way, I'm here if you ever need anything in the future.

Thanks,
Mihir""",
    },
}


CALL_SCRIPTS = {
    "cold_call_intro": {
        "name": "Cold Call Introduction",
        "script": """
[OPENING — Keep it under 20 seconds]
"Hi, is this {{ client_contact_name }}? My name is Mihir, I'm a 3D artist. 
I came across {{ client_name }} and had a quick idea that might help with 
{{ pain_point_short }}. Do you have 2 minutes?"

[IF YES — Pitch in 60 seconds]
"Great! So I noticed {{ observation }}. 

What I do is create {{ service_description_short }}. 
For example, I recently {{ relevant_example }}.

The result? {{ result_statement }}.

I was thinking something similar could work really well for {{ client_name }}."

[ASK]
"Would you be interested in seeing a quick sample of what this could 
look like for your business? I could put something together — no cost, 
no obligation."

[IF INTERESTED]
"Perfect! What's the best email to send it to? I'll also include a link 
to my portfolio so you can see more of my work. 
When would be a good time for a longer conversation — maybe 15-20 minutes?"

[IF NOT INTERESTED RIGHT NOW]
"Totally understand! Can I send you my portfolio link in case something 
comes up in the future? What's the best email?"

[CLOSING]
"Thanks for your time, {{ client_contact_name }}. I'll send that over today. 
Have a great day!"
""",
    },
    "discovery_call": {
        "name": "Discovery Call (after they show interest)",
        "script": """
[OPENING — Build rapport, 2-3 minutes]
"Hi {{ client_contact_name }}, thanks for taking the time to chat! 
Before I dive in, I'd love to learn more about {{ client_name }}. 
Tell me a bit about your business and what you're working on."

[LISTEN — Let them talk. Take notes on:]
• What they do
• Their current challenges with visuals
• What they've tried before
• Their timeline
• Decision-making process

[DISCOVERY QUESTIONS — Pick 3-4 relevant ones]
1. "How are you currently handling your product photography / visual content?"
2. "What's the biggest challenge you face with your current approach?"
3. "Have you ever considered 3D renders instead of traditional photography?"
4. "What does your typical timeline look like for new visual content?"
5. "Who else is involved in decisions about your visual content?"
6. "What's your budget range for this kind of work?"
7. "Is there a specific project or launch coming up that you need visuals for?"

[PRESENT YOUR SOLUTION — Tailor to what they shared]
"Based on what you've told me, here's what I'm thinking..."
• Connect their specific pain points to your specific services
• Show relevant portfolio examples
• Explain the process step by step
• Give a rough timeline

[PRICING — Be transparent but flexible]
"For a project like this, it would typically be in the range of {{ budget_range }}. 
But let me put together a detailed proposal based on exactly what we discussed."

[CLOSE]
"Here's what I suggest as next steps:
1. I'll send you a proposal with scope, timeline, and pricing by [date]
2. You review it with your team
3. We hop on a quick call to finalize details
4. I can start as soon as we align on everything.

Sound good?"

[ALWAYS END WITH]
"Is there anything else you'd like to know about my work or process?"
""",
    },
    "objection_handling": {
        "name": "Common Objections & Responses",
        "script": """
[OBJECTION: "It's too expensive"]
"I understand budget is important. Let me ask — how much are you currently 
spending on product photography / visual content? 

With 3D renders, you get unlimited angles, easy updates for color variants, 
and you never need to reshoot. Many of my clients find it actually saves 
money in the long run.

We could also start with a smaller scope — maybe just your top 3-5 products 
— so you can see the ROI before committing to a larger project."

[OBJECTION: "We already have a photographer"]
"That's great! I'm not suggesting replacing photography entirely. 
3D renders are perfect for things photography can't do easily — 
like showing products in context, creating exploded views, 
or making interactive 3D experiences for your website.

Many brands use both. I can complement what your photographer does."

[OBJECTION: "We don't have time right now"]
"Totally get it. When would be a better time? Is there a product launch 
or season coming up?

In the meantime, I'd love to send you a sample render — maybe one of 
your best-selling products — so you can see the quality whenever 
you're ready to move forward."

[OBJECTION: "3D renders won't look realistic enough"]
"That's a common concern! Let me show you some examples — these are 
100% 3D rendered, no photography at all.
[Share portfolio link]

I've created renders for Amazon and Wayfair product catalogs that are 
indistinguishable from photos. The technology has come a long way."

[OBJECTION: "We need to think about it"]
"Of course. What specific things would you like to think over? 
Maybe I can help clarify anything.

I'll also send you a summary of what we discussed along with my 
portfolio link, so you have everything you need."

[OBJECTION: "Can you do it cheaper?"]
"I want to make this work for you. Let's talk about scope — 
maybe we can adjust the deliverables to fit your budget.

For example, instead of 10 renders we could start with 5, 
or I can do standard quality instead of ultra-high-res. 
What's most important to you?"
""",
    },
}


def generate_email(
    template_id: str,
    client_name: str,
    client_contact_name: str,
    market_id: str,
    service_id: str = "",
    custom_vars: dict = None,
) -> str:
    """Generate a personalized email from a template."""
    profile = load_profile()
    markets_data = load_target_markets()

    market = next(
        (m for m in markets_data["target_markets"] if m["id"] == market_id), {}
    )
    service = next(
        (s for s in profile["services"] if s["id"] == service_id),
        profile["services"][0],
    )

    template_data = EMAIL_TEMPLATES.get(template_id)
    if not template_data:
        return f"Template '{template_id}' not found."

    variables = {
        "client_name": client_name,
        "client_contact_name": client_contact_name,
        "service_name": service["name"],
        "deliverables": service.get("deliverables", []),
        "pain_point": market.get("pain_points", ["improving your visual content"])[0].lower(),
        "compliment": "what you're building",
        "proof_point": f"You can check out a live demo of my work here: {profile['portfolio']['live_demo']}",
        "free_offer": "",
        "source": "",
        "email": profile["email"],
        "phone": profile["phone"][0],
        "behance": profile["portfolio"]["behance"],
        "live_demo": profile["portfolio"]["live_demo"],
        "specific_value_prop": "",
        "relevant_link": profile["portfolio"]["behance"],
        "key_benefit": service["description"][:100],
        "project_name": "the project",
        "budget_range": market.get("budget_range", "varies based on scope"),
    }

    if custom_vars:
        variables.update(custom_vars)

    subject_tmpl = Template(template_data["subject"])
    body_tmpl = Template(template_data["body"])

    subject = subject_tmpl.render(**variables)
    body = body_tmpl.render(**variables)

    return f"Subject: {subject}\n\n{body}"


def generate_call_script(
    script_id: str,
    client_name: str,
    client_contact_name: str,
    market_id: str,
    service_id: str = "",
    custom_vars: dict = None,
) -> str:
    """Generate a personalized call script."""
    profile = load_profile()
    markets_data = load_target_markets()

    market = next(
        (m for m in markets_data["target_markets"] if m["id"] == market_id), {}
    )
    service = next(
        (s for s in profile["services"] if s["id"] == service_id),
        profile["services"][0],
    )

    script_data = CALL_SCRIPTS.get(script_id)
    if not script_data:
        return f"Script '{script_id}' not found."

    variables = {
        "client_name": client_name,
        "client_contact_name": client_contact_name,
        "service_name": service["name"],
        "service_description_short": service["description"][:150],
        "pain_point_short": market.get("pain_points", ["visual content needs"])[0].lower(),
        "observation": f"your current visual content could be taken to the next level",
        "relevant_example": "worked on product renders for e-commerce catalogs featured on Amazon and Wayfair",
        "result_statement": "higher quality visuals, more consistent look, and faster turnaround than traditional photography",
        "budget_range": market.get("budget_range", "varies based on scope"),
    }

    if custom_vars:
        variables.update(custom_vars)

    tmpl = Template(script_data["script"])
    return tmpl.render(**variables)


def display_email_templates():
    """Show available email templates."""
    console.print()
    console.print(
        Panel(
            "[bold]Email Templates[/bold]\n"
            "Ready-to-customize email templates for different outreach stages.",
            style="cyan",
        )
    )

    table = Table(border_style="cyan")
    table.add_column("Template ID", style="bold", width=20)
    table.add_column("Name", width=30)
    table.add_column("Use When", width=40)

    use_cases = {
        "cold_intro": "First time reaching out to a potential client",
        "follow_up_1": "3-4 days after initial email, no response",
        "follow_up_2": "1 week after first follow-up, final attempt",
        "referral_ask": "After successfully completing a project",
    }

    for tid, tdata in EMAIL_TEMPLATES.items():
        table.add_row(tid, tdata["name"], use_cases.get(tid, ""))

    console.print(table)


def display_call_scripts():
    """Show available call scripts."""
    console.print()
    console.print(
        Panel(
            "[bold]Call Scripts[/bold]\n"
            "Conversation guides for phone calls with potential clients.",
            style="green",
        )
    )

    table = Table(border_style="green")
    table.add_column("Script ID", style="bold", width=25)
    table.add_column("Name", width=35)
    table.add_column("Use When", width=35)

    use_cases = {
        "cold_call_intro": "Calling someone for the first time",
        "discovery_call": "They showed interest, this is the deeper conversation",
        "objection_handling": "Reference guide for common pushbacks",
    }

    for sid, sdata in CALL_SCRIPTS.items():
        table.add_row(sid, sdata["name"], use_cases.get(sid, ""))

    console.print(table)


def preview_email(
    template_id: str,
    client_name: str = "Acme Jewelry",
    contact_name: str = "Priya",
    market_id: str = "jewelry_fashion",
    service_id: str = "product_renders",
):
    """Preview an email template with sample data."""
    email = generate_email(
        template_id,
        client_name,
        contact_name,
        market_id,
        service_id,
        custom_vars={
            "compliment": "your beautiful jewelry collection",
            "source": "Instagram",
            "free_offer": "create a free 3D render of one of your best-selling pieces",
            "specific_value_prop": (
                "Instead of expensive product photography sessions for each new piece,\n"
                "3D renders give you unlimited angles, easy color/material variants,\n"
                "and an interactive 3D viewer your customers can play with on your website."
            ),
        },
    )

    console.print()
    console.print(Panel(email, title=f"Preview: {template_id}", border_style="cyan"))


def preview_call_script(
    script_id: str,
    client_name: str = "Sparkle Jewels",
    contact_name: str = "Rahul",
    market_id: str = "jewelry_fashion",
    service_id: str = "interactive_3d_web",
):
    """Preview a call script with sample data."""
    script = generate_call_script(
        script_id,
        client_name,
        contact_name,
        market_id,
        service_id,
    )

    console.print()
    console.print(Panel(script, title=f"Script: {script_id}", border_style="green"))
