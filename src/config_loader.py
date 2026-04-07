import os
import yaml
from pathlib import Path


def get_project_root() -> Path:
    return Path(__file__).parent.parent


def load_yaml(filename: str) -> dict:
    filepath = get_project_root() / "config" / filename
    with open(filepath, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


def load_profile() -> dict:
    return load_yaml("profile.yaml")


def load_target_markets() -> dict:
    return load_yaml("target_markets.yaml")


def get_data_dir() -> Path:
    data_dir = get_project_root() / "data"
    data_dir.mkdir(exist_ok=True)
    return data_dir
