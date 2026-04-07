from flask import Flask
from app.config import Config, ARTIST_PROFILE
from app.database import init_db


def create_app():
    app = Flask(__name__)
    app.config.from_object(Config)

    init_db(app)

    from app.routes import bp
    app.register_blueprint(bp)

    # Make ARTIST_PROFILE available as `profile` in every template
    @app.context_processor
    def inject_profile():
        return {"profile": ARTIST_PROFILE}

    return app
