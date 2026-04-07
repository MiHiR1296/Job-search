from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()


def init_db(app):
    db.init_app(app)
    with app.app_context():
        # Import models so SQLAlchemy registers them before create_all
        from app import models  # noqa: F401
        db.create_all()
