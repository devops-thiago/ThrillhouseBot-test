import sqlite3
from datetime import datetime, timezone
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

app = FastAPI(title="Thrillhouse User API", version="0.1.0")

DB_PATH = "users.db"


# --- Database helpers ---

def get_db() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def init_db() -> None:
    with get_db() as conn:
        conn.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT    NOT NULL,
                email       TEXT    NOT NULL UNIQUE,
                created_at  TEXT    NOT NULL
            )
        """)


# --- Models ---

class UserCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=100, examples=["Alice"])
    email: str = Field(..., pattern=r"^[^@\s]+@[^@\s]+\.[^@\s]+$", examples=["alice@example.com"])


class UserUpdate(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=100)
    email: Optional[str] = Field(None, pattern=r"^[^@\s]+@[^@\s]+\.[^@\s]+$")


class UserResponse(BaseModel):
    id: int
    name: str
    email: str
    created_at: str


# --- Startup ---

@app.on_event("startup")
def startup() -> None:
    init_db()


# --- Routes ---

@app.post("/users", response_model=UserResponse, status_code=201)
def create_user(user: UserCreate) -> dict:
    """Create a new user."""
    now = datetime.now(timezone.utc).isoformat()
    with get_db() as conn:
        try:
            cursor = conn.execute(
                "INSERT INTO users (name, email, created_at) VALUES (?, ?, ?)",
                (user.name, user.email, now),
            )
            conn.commit()
        except sqlite3.IntegrityError:
            raise HTTPException(status_code=409, detail="A user with this email already exists")
        row = conn.execute(
            "SELECT * FROM users WHERE id = ?", (cursor.lastrowid,)
        ).fetchone()
    return dict(row)


@app.get("/users", response_model=list[UserResponse])
def list_users() -> list[dict]:
    """Return all users."""
    with get_db() as conn:
        rows = conn.execute("SELECT * FROM users ORDER BY id").fetchall()
    return [dict(r) for r in rows]


@app.get("/users/{user_id}", response_model=UserResponse)
def get_user(user_id: int) -> dict:
    """Return a single user by ID."""
    with get_db() as conn:
        row = conn.execute("SELECT * FROM users WHERE id = ?", (user_id,)).fetchone()
    if row is None:
        raise HTTPException(status_code=404, detail="User not found")
    return dict(row)


@app.put("/users/{user_id}", response_model=UserResponse)
def update_user(user_id: int, user: UserUpdate) -> dict:
    """Update an existing user."""
    with get_db() as conn:
        existing = conn.execute(
            "SELECT * FROM users WHERE id = ?", (user_id,)
        ).fetchone()
        if existing is None:
            raise HTTPException(status_code=404, detail="User not found")

        fields: list[str] = []
        values: list[str | int] = []
        if user.name is not None:
            fields.append("name = ?")
            values.append(user.name)
        if user.email is not None:
            fields.append("email = ?")
            values.append(user.email)

        if fields:
            values.append(user_id)
            conn.execute(
                f"UPDATE users SET {', '.join(fields)} WHERE id = ?", values
            )
            conn.commit()

        row = conn.execute(
            "SELECT * FROM users WHERE id = ?", (user_id,)
        ).fetchone()
    return dict(row)


@app.delete("/users/{user_id}", status_code=204)
def delete_user(user_id: int) -> None:
    """Delete a user."""
    with get_db() as conn:
        cursor = conn.execute("DELETE FROM users WHERE id = ?", (user_id,))
        conn.commit()
        if cursor.rowcount == 0:
            raise HTTPException(status_code=404, detail="User not found")


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}
