"""
Auto-Discovery Module

Generates a curated list of real potential clients from researched businesses
across all target markets. Each lead includes contact info, why they're a fit,
which service to pitch, and a recommended approach.
"""

import json
from datetime import datetime

from rich.console import Console
from rich.table import Table
from rich.panel import Panel

from .config_loader import load_profile, load_target_markets, get_data_dir

console = Console()


DISCOVERED_LEADS = [
    # ── JEWELRY & FASHION (HIGH PRIORITY) ──────────────────────────────
    {
        "name": "MELO Studio",
        "market_id": "jewelry_fashion",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://melostudio.in/",
        "source": "Web Search — Indian handmade jewelry brands",
        "notes": "Handmade beaded jewelry in 925 silver with 18k gold plating. Collections include Shringaar, Trajva, nityā. Price range ₹1,980-₹6,710. Small brand, likely shooting products with basic photography. Perfect candidate for 3D product configurator like your jewelry demo.",
        "pitch_service": "interactive_3d_web",
        "pitch_angle": "Your pieces have multiple finishes and gemstone options — an interactive 3D configurator on your site would let customers see every combination before buying. I've built exactly this (live demo: jewelry configurator). One-time investment, unlimited angles.",
        "why_good_fit": "Small brand, handmade jewelry with customization options, online-first — exactly the kind of business that benefits from 3D configurator over photography.",
    },
    {
        "name": "Ayona Silver Soul Story",
        "market_id": "jewelry_fashion",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://www.ayona.in/",
        "source": "Web Search — Indian jewelry brands",
        "notes": "Sterling silver handcrafted jewelry. Ethically sourced, made in India. Pieces from ₹1,520. Brand tagline: 'feels like air, looks like art'. Clean branding but product photos could be elevated with 3D renders.",
        "pitch_service": "product_renders",
        "pitch_angle": "Your brand aesthetic is beautiful. 3D renders would give you unlimited angles, consistent lighting, and lifestyle shots for every piece without re-shooting. Plus I can create an interactive 3D viewer for your website.",
        "why_good_fit": "Small artisan jewelry brand with clean website but basic product photography. 3D renders would be a clear upgrade.",
    },
    {
        "name": "Studio Bibka",
        "market_id": "jewelry_fashion",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://www.studiobibka.com/",
        "source": "Web Search — Indian artisan jewelry",
        "notes": "Small-batch artisanal jewelry. Free shipping on orders above ₹2,000. 'Handcrafted premium jewelry designed to live with you.' Price range ₹1,700-₹7,000. Has a Shopify store.",
        "pitch_service": "product_renders",
        "pitch_angle": "Small-batch means you're always adding new designs. With 3D models, I can render every new piece in minutes instead of scheduling a photo shoot. Plus consistent styling across your entire catalog.",
        "why_good_fit": "Small-batch model means frequent new products = frequent photo shoots. 3D renders solve this permanently.",
    },
    {
        "name": "Smorsh",
        "market_id": "jewelry_fashion",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://smorsh.in/",
        "source": "Web Search — Shopify jewelry brands India",
        "notes": "Modern minimalist 18K gold-plated jewelry. Multiple collections. ₹649-₹899 price range. Shopify store. Active brand with accessible pricing — likely has tight budget for visuals.",
        "pitch_service": "product_renders",
        "pitch_angle": "At your price point, expensive photography eats into margins. 3D renders give you premium visuals at a fraction of the cost — and once the model is made, every variant is just a material change.",
        "why_good_fit": "Budget-conscious brand that would value cost-effective high-quality visuals. 3D renders are cheaper than repeated photo shoots.",
    },
    {
        "name": "Veyaa",
        "market_id": "jewelry_fashion",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://veyaa.in/",
        "source": "Web Search — Indian handcrafted jewelry",
        "notes": "Handcrafted jewelry with personalized options, polki diamonds, traditional elements. Ships worldwide. Offers personalization = perfect use case for a 3D configurator.",
        "pitch_service": "interactive_3d_web",
        "pitch_angle": "You offer personalized jewelry — imagine if customers could customize pieces in 3D on your website before ordering. I've built exactly this kind of configurator (link to demo). It would set you apart from every competitor.",
        "why_good_fit": "Personalization offering is a natural fit for an interactive 3D product configurator.",
    },

    # ── E-COMMERCE / D2C (HIGH PRIORITY) ───────────────────────────────
    {
        "name": "DEWDASH",
        "market_id": "ecommerce_sellers",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://www.linkedin.com/company/dewdash",
        "source": "Web Search — Mumbai D2C skincare startups 2024",
        "notes": "Skincare brand founded 2024, Mumbai. Emphasizes simplicity and freshness. Young brand likely building their visual identity — perfect timing to offer 3D product renders for their packaging.",
        "pitch_service": "product_renders",
        "pitch_angle": "As a new brand building your visual identity, 3D product renders give you studio-quality packaging visuals before you even have physical stock. Perfect for pre-launch marketing and e-commerce listings.",
        "why_good_fit": "New Mumbai-based D2C brand in early stage — needs visuals for launch marketing, cost-sensitive, local.",
    },
    {
        "name": "Fugeno Skincare",
        "market_id": "ecommerce_sellers",
        "contact_person": "Nupur Thakkar (Founder)",
        "contact_email": "",
        "website": "https://fugeno.com",
        "source": "Web Search — emerging D2C skincare brands India",
        "notes": "Gender-neutral skincare brand by 29-year-old founder. Available on Amazon, Myntra. Blends biomimicry with Indian home remedies. Growing brand that needs consistent product visuals across multiple marketplaces.",
        "pitch_service": "product_renders",
        "pitch_angle": "Selling on Amazon + Myntra + your own site means you need consistent, marketplace-compliant visuals everywhere. 3D renders give you that consistency — one model, infinite variations for every platform.",
        "why_good_fit": "Multi-platform D2C brand needing consistent visuals. Young founder likely open to modern approaches.",
    },
    {
        "name": "FLOWe Skincare",
        "market_id": "ecommerce_sellers",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://floweskincare.com/",
        "source": "Web Search — D2C skincare India",
        "notes": "AI-powered Ayurveda skincare brand. Seed funded (₹10L in 2023). Rebranding in 2025 with expanded product lines. Rebranding = perfect timing to need new product visuals and packaging renders.",
        "pitch_service": "product_renders",
        "pitch_angle": "I saw you're rebranding and expanding your product line — this is the perfect time to switch to 3D product renders. One 3D model per product, and you can render every future color/variant/packaging update without a reshoot.",
        "why_good_fit": "Rebranding phase = actively needs new visual content. Funded but small = has budget, values efficiency.",
    },
    {
        "name": "Essentive Skincare",
        "market_id": "ecommerce_sellers",
        "contact_person": "Team",
        "contact_email": "",
        "website": "https://essentive.in/",
        "source": "Web Search — Indian clinical skincare D2C",
        "notes": "Clinical skincare brand with research-backed formulations. Premium positioning with advanced ingredients. Needs premium-looking product visuals to match their scientific branding.",
        "pitch_service": "product_renders",
        "pitch_angle": "Your clinical, science-backed branding deserves visuals that match. 3D renders with precise lighting and materials can show the premium quality of your packaging — from frosted glass to metallic labels, with absolute consistency.",
        "why_good_fit": "Premium positioning but likely a small team. Needs high-end visuals that match their brand story.",
    },

    # ── FURNITURE / HOME DECOR (HIGH-value D2C) ────────────────────────
    {
        "name": "Andaman Aisle",
        "market_id": "ecommerce_sellers",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://andamanaisle.com/",
        "source": "Web Search — handcrafted furniture India",
        "notes": "Handcrafted custom furniture. Collections inspired by Pierre Jeanneret. Made-to-order by Indian artisans. Ethically sourced wood. Needs lifestyle renders showing furniture in room contexts.",
        "pitch_service": "product_renders",
        "pitch_angle": "Photographing furniture in styled rooms is expensive. With 3D renders, I can place every piece in beautiful room settings — living rooms, bedrooms, offices — without a physical set. Plus customers can see different wood finishes and fabrics instantly.",
        "why_good_fit": "Custom furniture brand = needs to show variations. 3D renders are far cheaper than staging multiple room setups.",
    },
    {
        "name": "Sycaro Studio",
        "market_id": "ecommerce_sellers",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://www.sycaro.com/",
        "source": "Web Search — handmade furniture brands India",
        "notes": "Handcrafted wooden tableware, objects, sculptures. Custom commission-based model. Behind-the-scenes content on website. Small artisan brand.",
        "pitch_service": "product_renders",
        "pitch_angle": "Your craftsmanship deserves to be shown from every angle. 3D renders of your wooden pieces can capture the grain, the finish, the curves — with perfect lighting every time. Plus interactive 3D on your website would let customers examine pieces up close.",
        "why_good_fit": "Artisan products where detail matters. 3D renders can capture wood grain and finish quality better than phone photos.",
    },
    {
        "name": "Ramgarh Crafts",
        "market_id": "ecommerce_sellers",
        "contact_person": "Team",
        "contact_email": "",
        "website": "https://www.ramgarhcrafts.com/",
        "source": "Web Search — solid wood furniture India online",
        "notes": "Handmade solid wood furniture. Sofas, dining sets, chairs, storage. Sustainable design. 4-6 week production per order. Online store with collections.",
        "pitch_service": "product_renders",
        "pitch_angle": "With 4-6 week production time, customers can't see the final product before ordering. 3D renders of your furniture in room settings would build confidence and reduce returns. I can also create 360° views for your product pages.",
        "why_good_fit": "Long production times make visualization crucial for buyer confidence. 3D room scenes help customers commit to purchase.",
    },
    {
        "name": "Dawn Riser",
        "market_id": "ecommerce_sellers",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://dawnriser.com/",
        "source": "Web Search — furniture brands India Shopify",
        "notes": "Bangalore-based (est. 2022). Solid wood (acacia, teak). Custom made-to-order. Factory-direct pricing. Ships India + USA. Growing brand with ambition.",
        "pitch_service": "product_renders",
        "pitch_angle": "You're shipping to the US market where customers expect premium product visuals. 3D renders with room staging would elevate your listings above competitors and help US customers visualize pieces in their homes.",
        "why_good_fit": "US market expansion requires premium visuals. Cost-conscious brand (factory-direct) would value 3D efficiency.",
    },

    # ── REAL ESTATE (HIGH PRIORITY) ────────────────────────────────────
    {
        "name": "Shree Jari Mari (Vaidiki Aarambh)",
        "market_id": "real_estate",
        "contact_person": "Sales Team",
        "contact_email": "",
        "website": "https://jvinfra.in/projects/vaidiki-aarambh",
        "source": "Web Search — new projects Dombivli 2025",
        "notes": "Small developer. 1 & 2 BHK project in Dombivli East (very close to Kalyan). Launched Nov 2025, 163 units. Under construction — needs 3D renders to sell units before completion. RERA registered.",
        "pitch_service": "arch_viz",
        "pitch_angle": "Your project is under construction and you're selling off-plan. Professional 3D interior renders of your 1BHK and 2BHK layouts would help buyers visualize their future home. I'm based in Kalyan — we can meet in person and I can visit the site.",
        "why_good_fit": "LOCAL to you (Dombivli/Kalyan area). Under-construction project = literally needs 3D renders to sell. Small builder = approachable.",
    },
    {
        "name": "Mahaavir Buildcon (Mahaavir Pride)",
        "market_id": "real_estate",
        "contact_person": "Marketing Team",
        "contact_email": "",
        "website": "https://regrob.com/project/mahaavir-pride-dombivli-thane/",
        "source": "Web Search — Dombivli builders 2025",
        "notes": "1, 2 & 3 BHK project in Dombivli East. 416 units, 2 towers (G+31). From ₹45 Lakhs. RERA approved. Likely has some renders but small builder may need better quality or additional visualizations.",
        "pitch_service": "arch_viz",
        "pitch_angle": "For a project this size (416 units), premium 3D walkthroughs and interior renders for each configuration can make a huge difference in conversions. I can also create 360° virtual tours buyers can view from their phone.",
        "why_good_fit": "Local Dombivli project, sizeable development that needs quality marketing visuals.",
    },
    {
        "name": "Swaminarayan Construction (City Neo)",
        "market_id": "real_estate",
        "contact_person": "Sales/Marketing",
        "contact_email": "",
        "website": "https://newprojectsonline.com/swaminarayan-city-neo-thane-dombivali",
        "source": "Web Search — Dombivli township projects",
        "notes": "Mega township on 100 acres in Dombivli West. 1, 2 & 3 BHK from ₹37.5L. Has school, 45K sqft clubhouse. A project this large needs constant marketing material — 3D renders, walkthroughs, virtual tours.",
        "pitch_service": "arch_viz",
        "pitch_angle": "A 100-acre township has so many amenities to showcase — clubhouse, school, landscaping, different apartment types. I can create a comprehensive 3D visualization package: interior renders for each unit type, aerial renders of the township, and virtual walkthroughs for your sales team.",
        "why_good_fit": "Large township project near you with many different areas that need visualization. Ongoing work potential.",
    },

    # ── INTERIOR DESIGNERS (HIGH PRIORITY) ─────────────────────────────
    {
        "name": "Studio 369",
        "market_id": "interior_designers",
        "contact_person": "Team",
        "contact_email": "studio369gsd@gmail.com",
        "contact_phone": "+91 9769355560",
        "website": "https://studio369.in/",
        "source": "Web Search — interior design firms Kalyan Thane",
        "notes": "7+ years experience. HQ in Vasind, Maharashtra. Serves Kalyan, Thane, Dombivli, Navi Mumbai. Luxury residential design. THEY ARE IN YOUR EXACT AREA. May or may not have in-house 3D — could use you for overflow or specific projects.",
        "pitch_service": "arch_viz",
        "pitch_angle": "I'm a 3D artist based in Kalyan — we're practically neighbors. If you ever need photorealistic 3D renders for client presentations, or if your pipeline gets busy and you need rendering support, I'd love to be your go-to. I can turn around interior renders quickly and I understand the local market.",
        "why_good_fit": "LOCATED IN YOUR AREA (Vasind/Kalyan). Interior design firm that serves exactly your geography. Easy to build in-person relationship.",
    },
    {
        "name": "Pure Space Interior",
        "market_id": "interior_designers",
        "contact_person": "Team",
        "contact_email": "info@purespace.co.in",
        "contact_phone": "+91 88799 55545",
        "website": "https://purespace.co.in/",
        "source": "Web Search — interior design Mumbai",
        "notes": "ISO certified interior design company in Kurla West, Mumbai. Does projects across Mumbai including Thane. Has multiple 2BHK/3BHK residential projects. May need external 3D rendering support.",
        "pitch_service": "arch_viz",
        "pitch_angle": "I noticed you're handling projects across Mumbai including Thane. If you ever need high-quality 3D renders for client presentations — interior visualizations, 360° views, or material comparisons — I can deliver fast turnarounds. My portfolio includes kitchen and bedroom renders similar to your project types.",
        "why_good_fit": "Established firm with multiple projects. Likely outsources some 3D work. Has contact info available.",
    },
    {
        "name": "Elevation Interiors",
        "market_id": "interior_designers",
        "contact_person": "Team",
        "contact_email": "",
        "website": "https://www.elevationinterior.com/",
        "source": "Web Search — interior designers Thane",
        "notes": "Multiple Thane residential projects including luxury 3BHK and 4BHK. Works on branded residences (Hiranandani, Dosti). Active with ongoing projects.",
        "pitch_service": "arch_viz",
        "pitch_angle": "Your luxury residential projects need renders that match the quality your clients expect. I specialize in photorealistic interior visualization with experience rendering kitchens, bedrooms, and living spaces. Let me show you what I can do with one of your current floor plans — free sample render.",
        "why_good_fit": "Luxury segment = higher budgets for visualization. Based in Thane = easy to meet in person.",
    },

    # ── TECH STARTUPS (MEDIUM PRIORITY) ────────────────────────────────
    {
        "name": "Homekar",
        "market_id": "startups_tech",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://homekar.com/",
        "source": "Web Search — furniture startup India AI visualization",
        "notes": "Customized furniture with AI-powered room design visualization. Based in Vadodara. They're already using AI visualization but could use a 3D artist to create higher quality renders and product models for their configurator.",
        "pitch_service": "interactive_3d_web",
        "pitch_angle": "Your AI visualization tool is great concept. I can help make it even better with hand-crafted 3D models that look more realistic than AI-generated ones. I've built interactive 3D configurators before (demo link) and can create photorealistic furniture models for your platform.",
        "why_good_fit": "Tech-forward furniture company that understands 3D value but may need artist-quality models.",
    },
    {
        "name": "Dr. Interior",
        "market_id": "startups_tech",
        "contact_person": "Founder",
        "contact_email": "",
        "website": "https://drinterior.in/",
        "source": "Web Search — AR interior design India",
        "notes": "India's first AR-enabled interior design platform. Instant placement, real-time rendering. Needs high-quality 3D furniture models for their AR library.",
        "pitch_service": "product_renders",
        "pitch_angle": "Your AR platform is only as good as the 3D models in it. I specialize in creating optimized, photorealistic 3D furniture models — web-ready, correctly scaled, with proper PBR materials. I can help expand your model library at scale.",
        "why_good_fit": "AR platform that literally needs 3D models as core product. Could be ongoing work creating their asset library.",
    },

    # ── MANUFACTURING (MEDIUM PRIORITY) ────────────────────────────────
    {
        "name": "Utkarsh Brush Private Limited",
        "market_id": "manufacturing_industrial",
        "contact_person": "Sales Team",
        "contact_email": "",
        "website": "https://www.ubplbrushes.com/",
        "source": "Web Search — Mumbai manufacturers IndiaMART",
        "notes": "Manufacturer of machine brushes & nylon strip brushes. Mumbai-based, est 2017. ₹1.5-5 Cr turnover. IndiaMART presence. Industrial products that would benefit from clean 3D product renders for catalog and website.",
        "pitch_service": "product_renders",
        "pitch_angle": "Your industrial brushes are precision products — but photos often don't show the detail and quality that sets you apart from competitors. 3D renders with perfect lighting can showcase the bristle density, material quality, and dimensions clearly. One-time investment for your entire product line.",
        "why_good_fit": "Industrial product manufacturer with likely basic product photos. Clean renders would differentiate their catalog.",
    },
    {
        "name": "Prizma Precision",
        "market_id": "manufacturing_industrial",
        "contact_person": "Team",
        "contact_email": "",
        "website": "https://www.prizmaprecision.in/",
        "source": "Web Search — Navi Mumbai manufacturers",
        "notes": "Conveyor systems and industrial components. Navi Mumbai. Up to 10 employees, ₹40L-1.5 Cr turnover. Small company that would value affordable professional visuals.",
        "pitch_service": "product_renders",
        "pitch_angle": "Your conveyor systems and industrial components are complex products that are hard to photograph well. 3D renders can show them from the perfect angle, with cutaway views to show internal mechanisms, and assembly animations to help customers understand installation.",
        "why_good_fit": "Small manufacturer with complex products that are hard to photograph. Near you in Navi Mumbai.",
    },
    {
        "name": "Mahavir Enterprise",
        "market_id": "manufacturing_industrial",
        "contact_person": "Team",
        "contact_email": "",
        "website": "https://www.mahavir-enterprise.com/",
        "source": "Web Search — Mumbai industrial manufacturers",
        "notes": "Industrial washers and fasteners manufacturer. Mumbai. ₹5-25 Cr turnover, 11-25 employees. Products that are usually photographed poorly on IndiaMART.",
        "pitch_service": "product_renders",
        "pitch_angle": "On IndiaMART and your website, professional product images make the difference between getting inquiries and being skipped. I can create clean, consistent 3D renders of your entire fastener range — every size, every variant — with a uniform look that builds trust.",
        "why_good_fit": "Mid-size manufacturer with broad product range. Clean renders for the full catalog would be high-value.",
    },

    # ── MARKETING AGENCIES (MEDIUM PRIORITY) ───────────────────────────
    {
        "name": "Tridimensional Studios",
        "market_id": "marketing_agencies",
        "contact_person": "Team",
        "contact_email": "",
        "contact_phone": "+91 8237272134",
        "website": "https://www.tridimensionalstudios.com/",
        "source": "Web Search — Mumbai 3D creative agencies",
        "notes": "Mumbai 3D service provider, 5+ years, 11 employees. Offers 3D modeling, animation, texturing. They're a competitor BUT also a potential collaborator — they may overflow work to freelancers when busy.",
        "pitch_service": "product_renders",
        "pitch_angle": "I know you're a 3D studio yourselves — but when projects pile up or you need a specific skill (like procedural Blender work or Three.js web integration), having a reliable freelancer on speed dial helps. I specialize in product viz and interactive 3D web — areas that complement your services.",
        "why_good_fit": "3D studio that may need freelance overflow support. Potential for ongoing partnership rather than one-off work.",
    },
    {
        "name": "Kreative ANT",
        "market_id": "marketing_agencies",
        "contact_person": "Production Team",
        "contact_email": "production@kreativeant.com",
        "contact_phone": "+91 93734 84283",
        "website": "https://www.kreativeant.com/",
        "source": "Web Search — Navi Mumbai VFX studios",
        "notes": "VFX studio in Navi Mumbai. 3D animation, VFX, motion graphics, compositing, commercial ads. A creative agency that may need freelance 3D support for specific projects.",
        "pitch_service": "marketing_content",
        "pitch_angle": "When you have commercial projects that need 3D product shots, environment modeling, or interactive web content, I can be your go-to freelancer. I work in Blender with procedural workflows — fast turnaround, consistent quality. Based nearby in Kalyan.",
        "why_good_fit": "Creative studio that does commercial work needing 3D. Local to you. Has email contact available.",
    },
    {
        "name": "JStudios",
        "market_id": "marketing_agencies",
        "contact_person": "Jatin",
        "contact_email": "jatinj8120@gmail.com",
        "contact_phone": "+91 9238947581",
        "website": "https://www.jstudios.in/",
        "source": "Web Search — Mumbai creative 3D agencies",
        "notes": "Mumbai agency specializing in 3D animation, motion graphics, VFX, AR/VR, product visualization. 4+ years experience. Small agency that might overflow work to reliable freelancers.",
        "pitch_service": "product_renders",
        "pitch_angle": "Hey Jatin, fellow 3D artist here based in Kalyan. When you've got more work than you can handle — especially product viz, Three.js web 3D, or Blender procedural work — I'd love to help out as your freelance backup. 9+ years experience, fast turnaround.",
        "why_good_fit": "Small agency with direct founder contact. Fellow 3D professional = understands your skills. Overflow work potential.",
    },

    # ── RESTAURANTS / HOSPITALITY (LOW PRIORITY but local) ─────────────
    {
        "name": "Local Kalyan/Thane restaurants without websites",
        "market_id": "restaurants_cafes",
        "contact_person": "Owner",
        "contact_email": "",
        "website": "",
        "source": "Google Maps — search 'restaurant Kalyan' or 'cafe Thane'",
        "notes": "STRATEGY LEAD: Search Google Maps for restaurants and cafes in Kalyan/Thane area that have Zomato/Swiggy but no website. Offer them an affordable modern website + good food/ambiance photography. Start with 2-3 local ones you personally know or visit.",
        "pitch_service": "small_websites",
        "pitch_angle": "I'm a local in Kalyan. I noticed you don't have your own website — you're only on Zomato/Swiggy which take 25-30% commission. A simple modern website with online ordering could save you lakhs per year. I can build one for you at a very reasonable rate.",
        "why_good_fit": "Lowest barrier to entry. Walk-in pitch. Local relationship building. Good for portfolio and cash flow.",
    },
]


def _enrich_lead(lead_data: dict) -> dict:
    """Convert a discovered lead into the standard lead format."""
    return {
        "name": lead_data["name"],
        "market_id": lead_data["market_id"],
        "contact_person": lead_data.get("contact_person", ""),
        "contact_method": "email" if lead_data.get("contact_email") else "website",
        "contact_info": lead_data.get("contact_email", ""),
        "contact_email": lead_data.get("contact_email", ""),
        "contact_phone": lead_data.get("contact_phone", ""),
        "website": lead_data.get("website", ""),
        "notes": lead_data.get("notes", ""),
        "source": lead_data.get("source", "Auto-discovered"),
        "pitch_service": lead_data.get("pitch_service", ""),
        "pitch_angle": lead_data.get("pitch_angle", ""),
        "why_good_fit": lead_data.get("why_good_fit", ""),
        "has_budget": False,
        "budget_unknown": True,
        "easy_to_reach": bool(lead_data.get("contact_email")),
        "contact_available": bool(lead_data.get("contact_email") or lead_data.get("website")),
        "created_at": datetime.now().isoformat(),
        "status": "new",
        "history": [
            {
                "date": datetime.now().isoformat(),
                "action": "auto_discovered",
                "notes": f"Found via: {lead_data.get('source', 'web search')}",
            }
        ],
    }


def discover_leads(save: bool = True) -> list:
    """Discover and return leads from all target markets."""
    leads = [_enrich_lead(ld) for ld in DISCOVERED_LEADS]

    if save:
        filepath = get_data_dir() / "leads.json"
        with open(filepath, "w") as f:
            json.dump(leads, f, indent=2, default=str)
        console.print(f"\n[green]Saved {len(leads)} leads to {filepath}[/green]")

    return leads


def display_discovered_leads():
    """Display all discovered leads with full details."""
    markets_data = load_target_markets()
    profile = load_profile()
    market_map = {m["id"]: m for m in markets_data["target_markets"]}
    service_map = {s["id"]: s for s in profile["services"]}

    console.print()
    console.print(
        Panel(
            "[bold]Auto-Discovered Potential Clients[/bold]\n"
            "Real businesses found across your target markets, scored and ready for outreach.",
            style="cyan",
        )
    )

    current_market = None
    lead_num = 0

    for ld in DISCOVERED_LEADS:
        market = market_map.get(ld["market_id"], {})
        service = service_map.get(ld.get("pitch_service", ""), {})

        if ld["market_id"] != current_market:
            current_market = ld["market_id"]
            priority = market.get("priority", "low")
            pcolor = {"high": "green", "medium": "yellow", "low": "red"}.get(priority, "white")
            console.print(
                f"\n[bold {pcolor}]{'━' * 80}[/bold {pcolor}]"
                f"\n[bold {pcolor}]  {market.get('name', current_market).upper()}  "
                f"(Priority: {priority.upper()})  "
                f"| Budget: {market.get('budget_range', 'varies')}[/bold {pcolor}]"
                f"\n[bold {pcolor}]{'━' * 80}[/bold {pcolor}]"
            )

        lead_num += 1

        score = 50
        if market.get("priority") == "high":
            score += 20
        elif market.get("priority") == "medium":
            score += 10
        if ld.get("contact_email"):
            score += 15
        if ld.get("contact_phone"):
            score += 5
        if ld.get("website"):
            score += 5
        score = min(score, 100)
        score_color = "green" if score >= 70 else "yellow" if score >= 50 else "red"

        table = Table(
            title=f"\n  #{lead_num}  {ld['name']}",
            title_style="bold white",
            show_header=False,
            border_style="cyan",
            width=90,
            padding=(0, 1),
        )
        table.add_column("Field", style="bold cyan", width=18, no_wrap=True)
        table.add_column("Details", width=68)

        table.add_row("Fit Score", f"[{score_color}]{score}/100[/{score_color}]")
        table.add_row("Website", ld.get("website", "—"))

        contacts = []
        if ld.get("contact_email"):
            contacts.append(f"Email: {ld['contact_email']}")
        if ld.get("contact_phone"):
            contacts.append(f"Phone: {ld['contact_phone']}")
        if ld.get("contact_person"):
            contacts.append(f"Contact: {ld['contact_person']}")
        table.add_row("Contact", "\n".join(contacts) if contacts else "Via website")

        table.add_row("Found Via", ld.get("source", "—"))
        table.add_row("About", ld.get("notes", "—"))
        table.add_row("Why Good Fit", f"[green]{ld.get('why_good_fit', '—')}[/green]")
        table.add_row(
            "Service to Pitch",
            f"[bold]{service.get('name', ld.get('pitch_service', '—'))}[/bold]",
        )
        table.add_row("Pitch Angle", f"[italic]{ld.get('pitch_angle', '—')}[/italic]")
        table.add_row(
            "Approach",
            market.get("approach", "—"),
        )

        console.print(table)

    console.print(f"\n[bold cyan]{'═' * 80}[/bold cyan]")

    summary_table = Table(title="\nLead Summary by Market", border_style="cyan")
    summary_table.add_column("Market", style="bold", width=35)
    summary_table.add_column("Priority", width=10, justify="center")
    summary_table.add_column("Leads Found", width=12, justify="center")
    summary_table.add_column("With Email", width=12, justify="center")

    market_counts = {}
    for ld in DISCOVERED_LEADS:
        mid = ld["market_id"]
        if mid not in market_counts:
            market_counts[mid] = {"total": 0, "with_email": 0}
        market_counts[mid]["total"] += 1
        if ld.get("contact_email"):
            market_counts[mid]["with_email"] += 1

    for mid, counts in market_counts.items():
        market = market_map.get(mid, {})
        pcolor = {"high": "green", "medium": "yellow", "low": "red"}.get(
            market.get("priority", "low"), "white"
        )
        summary_table.add_row(
            market.get("name", mid),
            f"[{pcolor}]{market.get('priority', '?').upper()}[/{pcolor}]",
            str(counts["total"]),
            str(counts["with_email"]),
        )

    console.print(summary_table)
    console.print(f"\n[bold]Total leads discovered: {len(DISCOVERED_LEADS)}[/bold]")
    console.print(
        "\n[dim]Run 'python3 -m src.cli discover --save' to save these to your lead tracker.[/dim]"
        "\n[dim]Run 'python3 -m src.cli generate-email' to create outreach emails for saved leads.[/dim]"
    )
