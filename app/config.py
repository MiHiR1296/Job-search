import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    SECRET_KEY = os.getenv("SECRET_KEY", "dev-secret-key")
    SQLALCHEMY_DATABASE_URI = os.getenv("DATABASE_URL", "sqlite:///leads.db")
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
    GOOGLE_PLACES_API_KEY = os.getenv("GOOGLE_PLACES_API_KEY", "")

# ---------------------------------------------------------------------------
# Mihir's profile — drives all AI-generated outreach
# ---------------------------------------------------------------------------
ARTIST_PROFILE = {
    "name": os.getenv("YOUR_NAME", "Mihir Botle"),
    "email": os.getenv("YOUR_EMAIL", "mihir.botle@gmail.com"),
    "phone": os.getenv("YOUR_PHONE", "+91 8692865684"),
    "portfolio": os.getenv("YOUR_PORTFOLIO", "https://www.behance.net/mihirbotle"),
    "linkedin": os.getenv("YOUR_LINKEDIN", "https://linkedin.com/in/mihirbotle"),
    "location": "Mumbai, India",
    "experience_years": 9,
    "tagline": "3D Generalist & Technical Artist",
    "summary": (
        "9+ years of experience creating 3D visuals, animations, and interactive content. "
        "I specialize in explainer videos, architectural visualizations, product renders, "
        "marketing content, and small websites. I've delivered work for companies like FYND, "
        "HERE Technologies, P&G, Amazon, and Wayfair."
    ),
}

# ---------------------------------------------------------------------------
# Services Mihir offers — each entry controls how leads are scored and what
# value proposition is surfaced in the outreach copy.
# ---------------------------------------------------------------------------
SERVICES = [
    {
        "id": "explainer_video",
        "label": "3D Explainer Videos",
        "description": (
            "Animated 3D explainer videos that break down your product or service in "
            "a clear, engaging way — ideal for landing pages, ads, and investor decks."
        ),
        "ideal_clients": [
            "saas startup", "tech startup", "edtech", "fintech", "app developer",
            "software company", "mobile app", "e-learning", "training company",
        ],
        "keywords": ["explainer", "product demo", "animation", "video", "saas"],
    },
    {
        "id": "arch_viz",
        "label": "Architectural Visualization",
        "description": (
            "Photorealistic 3D renderings and walkthroughs of buildings, interiors, and "
            "real-estate developments — helps clients sell spaces before they're built."
        ),
        "ideal_clients": [
            "real estate developer", "architect", "interior designer", "construction company",
            "property developer", "home builder", "renovation contractor",
        ],
        "keywords": ["architecture", "real estate", "interior", "rendering", "visualization"],
    },
    {
        "id": "product_render",
        "label": "Product Renders",
        "description": (
            "Studio-quality 3D product renders and lifestyle shots — perfect for e-commerce, "
            "packaging, catalogs, and ad campaigns without expensive photoshoots."
        ),
        "ideal_clients": [
            "e-commerce", "consumer goods", "jewelry", "furniture", "electronics",
            "cosmetics", "fashion brand", "startup product", "amazon seller",
        ],
        "keywords": ["product", "e-commerce", "packaging", "render", "photography"],
    },
    {
        "id": "marketing_content",
        "label": "Marketing & Social Content",
        "description": (
            "Eye-catching 3D visuals, motion graphics, and short animations for social media, "
            "digital ads, and brand campaigns."
        ),
        "ideal_clients": [
            "digital marketing agency", "brand agency", "advertising agency",
            "social media manager", "small business marketing", "local business",
        ],
        "keywords": ["marketing", "social media", "ads", "brand", "motion graphics"],
    },
    {
        "id": "small_website",
        "label": "Small Business Websites",
        "description": (
            "Clean, modern websites with optional 3D / interactive elements — "
            "built for small businesses that want to stand out online."
        ),
        "ideal_clients": [
            "restaurant", "cafe", "boutique", "salon", "gym", "yoga studio",
            "local shop", "small business", "freelancer", "consultant",
        ],
        "keywords": ["website", "web design", "landing page", "portfolio site"],
    },
]

# Business categories to search for in Google Places
BUSINESS_CATEGORIES = [
    # Arch viz
    "real_estate_agency",
    "general_contractor",
    # Product renders
    "jewelry_store",
    "furniture_store",
    "home_goods_store",
    # Marketing
    "marketing_agency",
    # Websites
    "restaurant",
    "cafe",
    "beauty_salon",
    "gym",
    "yoga_studio",
    # SaaS / Startups — searched by keyword instead of type
]
